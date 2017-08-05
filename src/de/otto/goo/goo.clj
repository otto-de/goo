(ns de.otto.goo.goo
  (:require [iapetos.core :as p]
            [iapetos.export :as e]
            [clojure.tools.logging :as log]
            [iapetos.core :as prom]
            [iapetos.metric :as metric]
            [clojure.string :as str])
  (:import (iapetos.registry IapetosRegistry)
           (io.prometheus.client SimpleCollector Collector CollectorRegistry Collector$MetricFamilySamples Collector$MetricFamilySamples$Sample Gauge Gauge$Child)
           (de.otto.goo CallbackGauge)))

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
  (swap! default-registry (fn [r] (p/register-as r metric collector)))
  )

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

(defn text-format []
  (e/text-format (snapshot)))

(defn- compojure-path->url-path [cpj-path]
  (->> (str/split cpj-path #"/")
       (filter #(not (re-matches #"^:.*" %)))
       (filter #(not (re-matches #"^:.*" %)))
       (str/join "/")))

(defn timing-middleware [handler]
  (let [http-labels [:path :method :rc]]
    (quiet-register! (p/histogram :http/duration-in-s {:labels http-labels :buckets [0.05 0.1 0.15 0.2]}))
    (quiet-register! (p/counter :http/calls-total {:labels http-labels})))
  (fn [request]
    (assert (:compojure/route request) "Couldn't get route out of request. Is middleware applied AFTER compojure route matcher?")
    (if-let [response (handler request)]
      (let [[method path] (:compojure/route request)
            labels {:path   (compojure-path->url-path path)
                    :method method
                    :rc     (:status response)}]
        (inc! :http/calls-total labels)
        (prom/with-duration (get-from-default-registry :http/duration-in-s labels) response)))))

(defn- milli-to-seconds [milliseconds]
  (double (/ milliseconds (* 1000))))

(defn measured-execution
  ([fn-name fn & fn-params]
   (quiet-register! (prom/histogram :measured-execution/execution-time-in-s
                                    {:labels [:function :exception] :buckets [0.001 0.005 0.01 0.02 0.05 0.1]}))
   (let [start-time (System/currentTimeMillis)]
     (try
       (let [result (apply fn fn-params)]
         (observe! :measured-execution/execution-time-in-s {:function fn-name :exception :none}
                   (milli-to-seconds (- (System/currentTimeMillis) start-time)))
         result)
       (catch Exception e
         (observe! :measured-execution/execution-time-in-s {:function fn-name :exception (.getName (class e))}
                   (milli-to-seconds (- (System/currentTimeMillis) start-time)))
         (throw e))))))

(defn samples-from [registry]
  (->> registry
       (.raw)
       (.metricFamilySamples)
       (enumeration-seq)
       (map #(vec (.-samples %)))
       (flatten)))

(defn register-counter! [name options]
  (register! (p/counter name options)))

(defn register-gauge! [name options initial]
  (register! (p/gauge name options))
  (update! name initial))

(defn register-callback-gauge!
  ([^String name ^String help callback-fn]
    (register-callback-gauge! name help callback-fn (make-array String 0)(make-array String 0) ))
  ([^String name ^String help callback-fn label-names label-values]
   (if-let [collector (get-from-default-registry name)]
     (.setChild collector (proxy [Gauge$Child] [] (get [] (callback-fn))) label-values)
     (-> (Gauge/build name help)
         (.labelNames label-names)
         (.create)
         (.setChild (proxy [Gauge$Child] [] (get [] (callback-fn))) label-values)
         (#(register-as name %))))
    ))
(defn register-summary! [name options]
  (register! (p/summary name options)))

(defn register-histogram! [name options]
  (register! (p/histogram name options)))

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
