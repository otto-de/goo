(ns de.otto.tesla.goo.goo-graphite
  (:require [com.stuartsierra.component :as c]
            [de.otto.tesla.stateful.scheduler :as sched]
            [de.otto.tesla.goo.goo :as goo]
            [overtone.at-at :as at]
            [clojure.tools.logging :as log])
  (:import (goo GraphiteExporter)))


(defn sanitize-prefix [prefix]
  (if (or (empty? prefix) (= \. (last prefix)))
    prefix
    (str prefix ".")))

(defn push-to-graphite [graphite-config]
  (try
    (let [host (:host graphite-config)
          port (Integer/parseInt (:port graphite-config))
          prefix (sanitize-prefix (:prefix graphite-config ""))
          r (.raw (goo/snapshot))]
      (log/infof "Reporting to Graphite %s:%s with %s as prefix" host port prefix)
      (GraphiteExporter/push host port prefix r))
    (catch Exception e
      (log/error e "Error while Reporting to Graphite"))))

(defrecord GooGraphite [config scheduler]
  c/Lifecycle
  (start [self]
    (let [graphite-config (get-in config [:config :goo :graphite])
          interval-in-ms (* 1000 (:interval-in-s graphite-config))]
      (log/info "-> Starting Goo Graphite")
      (at/every interval-in-ms #(push-to-graphite graphite-config) (sched/pool scheduler) :desc "Goo Graphite"))
    self)

  (stop [self]
    (log/info "<- Stopping Goo Graphite")
    self))

(defn new-goo-graphite []
  (map->GooGraphite {}))


