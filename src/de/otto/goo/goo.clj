(ns de.otto.goo.goo
  (:require [iapetos.core :as p]
            [iapetos.export :as e]
            [clojure.tools.logging :as log]
            [iapetos.core :as prom]
            [clojure.string :as str])
  (:import (io.prometheus.client Collector$MetricFamilySamples$Sample Gauge Gauge$Child)))

(def empty-registry (p/collector-registry))

(defonce default-registry (atom empty-registry))

(defn snapshot []
  @default-registry)

(defn clear-default-registry! []
  (.clear (.raw (snapshot)))                                ; for unknown reasons there is still state left in the underlying CollectorRegistry
  (reset! default-registry (p/collector-registry)))

(defn get-from-default-registry
  ([name]
   (get-from-default-registry name {}))
  ([name labels]
   ((snapshot) name labels)))

(defmacro with-default-registry [& ops]
  `(-> (snapshot) ~@ops))

(defn- register-with-action [action ms]
  (try
    (swap! default-registry (fn [r] (apply p/register r ms)))
    (catch IllegalArgumentException e
      (action e))))

(defn- register-as [metric collector]
  (swap! default-registry (fn [r] (p/register-as r metric collector))))

(defn register! [& ms]
  (register-with-action #(log/warn (.getMessage %)) ms))

(defn quiet-register! [& ms]
  (register-with-action (fn [_]) ms))

;TODO:
;(defn get-metric-value [name]
;  (.get ^Collector ((snapshot)) name))

(defmacro inc! [& opts]
  `(with-default-registry (p/inc ~@opts)))

(defmacro dec! [& opts]
  `(with-default-registry (p/dec ~@opts)))

(defmacro update! [& opts]
  `(with-default-registry (p/set ~@opts)))

(defmacro observe! [& opts]
  `(with-default-registry (p/observe ~@opts)))

(defmacro register+execute! [name m op]
  `(do
     (when-not ((snapshot) ~name)
       (register! (~(first m) ~name ~@(rest m))))
     (~(first op) (snapshot) ~name ~@(rest op))))

(defn register-counter! [name options]
  (register! (p/counter name options)))

(defn register-gauge! [name options]
  (register! (p/gauge name options)))

(defn register-summary! [name options]
  (register! (p/summary name options)))

(defn register-histogram! [name options]
  (register! (p/histogram name options)))

(defn- compojure-path->url-path [cpj-path]
  (->> (str/split cpj-path #"/")
       (filter #(not (re-matches #"^:.*" %)))
       (filter #(not (re-matches #"^:.*" %)))
       (str/join "/")))

(defn milli-to-seconds [milliseconds]
  (double (/ milliseconds 1000)))

(defn timing-middleware [handler]
  (quiet-register! (p/histogram :http/duration-in-s {:labels [:path :method :rc] :buckets [0.05 0.1 0.15 0.2]}))
  (fn [request]
    (assert (:compojure/route request) "Couldn't get route out of request. Is middleware applied AFTER compojure route matcher?")
    (let [start-time (System/currentTimeMillis)
          response   (handler request)
          [method path] (:compojure/route request)
          labels     {:path   (compojure-path->url-path path)
                      :method method
                      :rc     (:status response)}]
      (observe! :http/duration-in-s labels (milli-to-seconds (- (System/currentTimeMillis) start-time)))
      response)))

(defmacro timed [metric-name labels->values body]
  `(do
     (quiet-register! (prom/histogram ~metric-name {:labels (conj (keys ~labels->values) :exception) :buckets [0.001 0.005 0.01 0.02 0.05 0.1]}))
     (let [start-time# (System/currentTimeMillis)]
       (try
         (let [result# ~body]
           (observe! ~metric-name (merge ~labels->values {:exception :none})
                     (milli-to-seconds (- (System/currentTimeMillis) start-time#)))
           result#)
         (catch Exception e#
           (observe! ~metric-name (merge ~labels->values {:exception (.getName (class e#))})
                     (milli-to-seconds (- (System/currentTimeMillis) start-time#)))
           (throw e#))))))

(defn measured-execution [fn-name fn & fn-params]
  (timed :measured-execution/execution-time-in-s {:function fn-name} (apply fn fn-params)))


(defn- sanitize [name]
  (iapetos.metric/sanitize (str name)))

(defn- label-names [labels->values]
  (into-array String (map name (keys labels->values))))

(defn- label-values [labels->values]
  (into-array String (map name (vals labels->values))))

(defn register-callback-gauge!
  "Register a gauge that uses a callback function to determine its value."
  ([name ^String help callback-fn]
   (register-callback-gauge! name help callback-fn {}))
  ([name ^String help callback-fn labels->values]
   (if-let [collector (get-from-default-registry (sanitize name))]
     (.setChild collector (proxy [Gauge$Child] [] (get [] (callback-fn))) (label-values labels->values))
     (-> (Gauge/build (sanitize name) help)
         (.labelNames (label-names labels->values))
         (.create)
         (.setChild (proxy [Gauge$Child] [] (get [] (callback-fn))) (label-values labels->values))
         (#(register-as (sanitize name) %))))))


(defn samples-from [registry]
  (->> registry
       (.raw)
       (.metricFamilySamples)
       (enumeration-seq)
       (map #(vec (.-samples %)))
       (flatten)))

(defn- ^String cleansed [^String s]
  (str/replace s #"[^a-zA-Z0-9_-]" "_"))

(defn- cleansed-labels [sample]
  (->> (interleave (.-labelNames sample) (.-labelValues sample))
       (map cleansed)
       (partition 2)))

(defn serialize-sample [sample prefix timestamp]
  [prefix
   (cleansed (.name sample))
   (for [[name value] (cleansed-labels sample)]
     (format ".%s.%s" name value))
   (format " %s %d\n" (.value sample) timestamp)])

(defn serialize-metrics [timestamp prefix registry]
  (let [samples (samples-from registry)]
    (for [^Collector$MetricFamilySamples$Sample sample samples]
      (serialize-sample sample prefix timestamp))))

(defn text-format []
  (e/text-format (snapshot)))
