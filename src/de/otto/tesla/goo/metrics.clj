(ns de.otto.tesla.goo.metrics
  (:require [com.stuartsierra.component :as cp]
            [clojure.tools.logging :as log]
            [metrics.core :as metrics]
            [metrics.meters :as meters]
            [metrics.gauges :as gauges]
            [metrics.counters :as counters]
            [metrics.histograms :as hist]
            [de.otto.tesla.goo.name-converter :as nc]))

(def metrics (atom []))

(defn- create-metric [name labels creation-fn]
  (let [mm {:name  name
            :labels labels}
        metric (assoc mm :metric (creation-fn (nc/to-graphite mm)))]
    (log/infof "Create new metric %s" name)
    (swap! metrics #(conj % metric))
    metric))

(defn- search [name labels creation-fn]
  (if-let [metric (some (fn [metric] (when (and (= labels (:labels metric)) (= name (:name metric)))
                                       metric))
                        @metrics)]
    (:metric metric)
    (create-metric name labels creation-fn)))

(defn clear-metrics []
  (doseq [metric @metrics]
    (metrics/remove-metric (nc/to-graphite metric)))
  (reset! metrics []))

(defn meter [name labels]
  (search name labels meters/meter))

(defn counter [name labels]
  (search name labels counters/counter))

(defn histogram [name labels]
  (search name labels hist/histogram))

(defn gauge [name labels]
  (search name labels gauges/gauge))



(defrecord Goo-Metrics []
  cp/Lifecycle
  (start [self]
    (log/info "-> starting Goo-Metrics")
    )
  (stop [self]
    (log/info "<- stopping Goo-Metrics")))
