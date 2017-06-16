(ns de.otto.tesla.redis.sentinel.core
  (:require [taoensso.carmine.commands :as commands]
            [taoensso.carmine :as car]
            [com.stuartsierra.component :as c]
            [clojure.tools.logging :as log]
            [taoensso.encore :as enc])
  (:import (clojure.lang ExceptionInfo)))

(defonce ^:private command-spec
         (if-let [edn (enc/slurp-resource "sentinel-commands.edn")]
           (try
             (enc/read-edn edn)
             (catch Exception e
               (throw (ex-info "Failed to read Carmine commands edn" {} e))))
           (throw (ex-info "Failed to find Carmine commands edn" {}))))

(defmacro defcommands []
  `(do ~@(map (fn [[k v]] `(commands/defcommand ~k ~v)) command-spec)))

(defcommands)


(defn assert-master-role [redis-spec]                       ; !only works for redis >= 2.8.12. For prior versions parse the role field of the "info" command
  (let [role (car/wcar {:spec redis-spec :pool {}} (role))]
    (= "master" (first role))))

(defn current-master [{:keys [master-name sentinels]}]
  (loop [remaining-sentinels sentinels]
    (if-let [master (car/wcar {:spec (first remaining-sentinels) :pool {}} (master master-name))]
      {:host (first master)
       :port (Integer/parseInt (second master))}
      (if (seq (rest remaining-sentinels))
        (recur (rest remaining-sentinels))
        (throw (IllegalStateException. "No sentinel knew the master."))))))


(defn asserted-current-master [conf]
  (let [master (current-master conf)]
    (if (assert-master-role master)
      master
      (throw (IllegalStateException. "The master determined by the sentinels doesn't know of his role as master.")))))

(defn update-conn-spec [client]
  (let [current-master (asserted-current-master client)]
    (-> client
        (update :spec merge (:spec client) current-master)
        (assoc :pool (:pool client)))))

(defmacro wcar
  "It's the same as taoensso.carmine/wcar, but supports
      :master-name \"mymaster\"
      :sentinels [{:host [HOST1] :port [SOMEPORT1]}, {:host [HOST2] :port [SOMEPORT2]}].
  "
  [client & sigs]
  `(let [outcomes# (car/wcar (update-conn-spec ~client) ~@sigs)
         exceptions# (filter #(= ExceptionInfo (type %)) outcomes#)]
     (when (seq exceptions#)
       (throw (first exceptions#)))
     (if (= 1 (count outcomes#))
       (first outcomes#)
       outcomes#)))

(defrecord Redis-Sentinel-Client [config config-key]
  c/Lifecycle
  (start [self]
    (let [redis-config (get-in config [:config :redis config-key])]
      (log/info "-> starting Redis-Sentinel-Client")
      (log/info (format "Start redis-sentinel client with master %s and following sentinels: %s" (:redis-master redis-config) (:sentinels redis-config)))
      (assoc self :redis-config redis-config)))
  (stop [self]
    (log/info "<- stopping Redis-Client")
    self))

(defn new-redis-sentinel-client
  ([]
   (new-redis-sentinel-client :default))
  ([redis-config-key]
   (map->Redis-Sentinel-Client {:redis-config-key redis-config-key})))
