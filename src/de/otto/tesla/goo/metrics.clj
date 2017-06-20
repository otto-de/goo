(ns de.otto.tesla.goo.metrics
  (:require [clojure.tools.logging :as log]
            [metrics.core :as metrics]
            [metrics.meters :as meters]
            [metrics.gauges :as gauges]
            [metrics.counters :as counters]
            [metrics.timers :as timers]
            [metrics.histograms :as hist]))

(def empty-registry {})

(def metrics (atom empty-registry))

(defn metrics-snapshot []
  @metrics)

(defn- create-metric-object [name type]
  (let [creation-fn (case type
                      :meter meters/meter
                      :counter counters/counter
                      :histogram hist/histogram
                      :timer timers/timer
                      :gauge gauges/gauge)]
    (creation-fn name)))

(defn- codahale-name [name labels]
  (cons name (map second labels)))

(defn- create-metric [metric-name labels metric-type]
  (let [metric {:name metric-name
                :labels labels
                :type metric-type
                :metric (create-metric-object (codahale-name metric-name labels) metric-type)}]
    (log/infof "Create new metric %s of type %s" metric-name (name metric-type))
    (swap! metrics assoc [metric-name labels] metric)
    metric))

(defn- look-up [name labels type]
  (if-let [metric (get (metrics-snapshot) [name labels])]
    (:metric metric)
    (:metric (create-metric name labels type))))

(defn clear-metrics []
  (doseq [[[name labels] _] (metrics-snapshot)]
    (metrics/remove-metric (codahale-name name labels)))
  (reset! metrics empty-registry))

(defn meter
  ([name]
   (meter name []))
  ([name labels]
   (look-up name labels :meter)))

(defn counter
  ([name]
   (counter name []))
  ([name labels]
   (look-up name labels :counter)))

(defn histogram
  ([name]
   (histogram name []))
  ([name labels]
   (look-up name labels :histogram)))

(defn gauge
  ([name]
   (gauge name []))
  ([name labels]
   (look-up name labels :gauge)))

(defn timer
  ([name]
   (timer name []))
  ([name labels]
   (look-up name labels :timer)))
