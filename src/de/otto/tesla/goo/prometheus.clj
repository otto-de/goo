(ns de.otto.tesla.goo.prometheus
  (:require [metrics.core :as metrics]
            [de.otto.tesla.goo.metrics :as goo]
            [clojure.string :as str]
            [clojure.tools.logging :as log])
  (:import (com.codahale.metrics Snapshot)))

(defn prometheus-name [name]
  (-> name
      (str/replace #"\.|-" "_")
      (str/replace #"\W" "")))

(defn- type-line [name type]
  (format "# TYPE %s %s\n" name type))

(defn- single-value-metric->text [name type-str value]
  (let [pn (prometheus-name name)]
    (format "%s%s %s\n" (type-line pn type-str) pn value)))

(defn counter->text [[name counter]]
  (single-value-metric->text name "counter" (.getCount counter)))

(defn gauge->text [[name gauge]]
  (let [v (.getValue gauge)]
    (if (number? v)
      (single-value-metric->text name "gauge" v)
      (log/warnf "Invalid metric value for gauge \"%s\"" name))))

(defn histogram->text [[name histogram]]
  (let [pn (prometheus-name name)
        ^Snapshot snapshot (.getSnapshot histogram)]
    (str
      (type-line pn "summary")
      (format "%s{quantile=\"0.01\"} %s\n" pn (.getValue snapshot (double 0.01)))
      (format "%s{quantile=\"0.05\"} %s\n" pn (.getValue snapshot (double 0.05)))
      (format "%s{quantile=\"0.5\"} %s\n" pn (.getMedian snapshot))
      (format "%s{quantile=\"0.9\"} %s\n" pn (.getValue snapshot (double 0.9)))
      (format "%s{quantile=\"0.99\"} %s\n" pn (.get99thPercentile snapshot))
      (format "%s_sum %s\n" pn (reduce (fn [agg val] (+ agg val)) 0 (.getValues snapshot)))
      (format "%s_count %s\n" pn (.getCount histogram)))))

(defn generate-prometheus-metrics []
  (let [transform-fn #(case %
                         :meter counter->text
                         :counter counter->text
                         :histogram histogram->text
                         :timer histogram->text
                         :gauge gauge->text)]
    (str/join (map (fn [metric] ((transform-fn (:type metric)) [(:name metric) (:metric metric)])) @goo/metrics))))