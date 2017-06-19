(ns de.otto.tesla.goo.metrics
  (:require [clojure.tools.logging :as log]
            [metrics.core :as metrics]
            [metrics.meters :as meters]
            [metrics.gauges :as gauges]
            [metrics.counters :as counters]
            [metrics.timers :as timers]
            [metrics.histograms :as hist]
            [de.otto.tesla.goo.name-converter :as nc]))

(def metrics (atom []))

(defn create-metric-object [name type]
  (let [creation-fn (case type
                      :meter meters/meter
                      :counter counters/counter
                      :histogram hist/histogram
                      :timer timers/timer
                      :gauge gauges/gauge)]
    (creation-fn name)))

(defn- create-metric [name labels type]
  (let [mm {:name  name :labels labels}
        metric (assoc mm :metric (create-metric-object (nc/to-graphite mm) type))]
    (log/infof "Create new metric %s of type %s" name (name type))
    (swap! metrics #(conj % metric))
    metric))

(defn- look-up [name labels type]
  (if-let [metric (some (fn [metric] (when (and (= labels (:labels metric))
                                                (= name (:name metric)))
                                       metric))
                        @metrics)]
    (:metric metric)
    (create-metric name labels type)))

(defn clear-metrics []
  (doseq [metric @metrics]
    (metrics/remove-metric (nc/to-graphite metric)))
  (reset! metrics []))

(defn meter [name labels]
  (look-up name labels :meter))

(defn counter [name labels]
  (look-up name labels :counter))

(defn histogram [name labels]
  (look-up name labels :histogram))

(defn gauge [name labels]
  (look-up name labels :gauge))

(defn timer [name labels]
  (look-up name labels :timer))
