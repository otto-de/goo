(ns de.otto.tesla.goo.prometheus
  (:require [metrics.core :as metrics]
            [de.otto.tesla.goo.metrics :as goo]
            [clojure.string :as cs]
            [clojure.tools.logging :as log])
  (:import (com.codahale.metrics Snapshot)))

(defn cleansed-name [name]
  (-> name
      (cs/replace #"\.|-" "_")
      (cs/replace #"\W" "")))

(defn type->prom-name [type]
  (case type
    :histogram "summary"
    :meter "counter"
    (name type)))

(defn- type-line [name type]
  (format "# TYPE %s %s\n" name type))

(defn stringify-labels [labels]
  (if (empty? labels)
    ""
    (->> labels
         (map (fn [[k v]] (format "%s=\"%s\"" (cleansed-name k) v)))
         (cs/join ",")
         (format "{%s}"))))

(defn- single-value-metric->text [name type labels value]
  (let [pn (cleansed-name name)
        type-str (type->prom-name type)]
    (format "%s%s%s %s\n" (type-line pn type-str) pn (stringify-labels labels) value)))

(defn counter->text [{:keys [name type labels metric]}]
  (single-value-metric->text name type labels (.getCount metric)))

(defn gauge->text [{:keys [name type labels metric]}]
  (let [v (.getValue metric)]
    (if (number? v)
      (single-value-metric->text name type labels v)
      (log/warnf "Invalid metric value for gauge \"%s\"" name))))

(defn format-quantiles [pn labels snapshot]
  (let [quantiles [0.001 0.01 0.1 0.5 0.9 0.99 0.999]
        labels+quantile (fn [q] (stringify-labels (concat [["quantile" (str q)]] labels)))
        quantile-str (fn [q] (format "%s%s %s\n" pn (labels+quantile q) (.getValue snapshot (double q))))]
    (->> quantiles (map quantile-str) (cs/join))))

(defn histogram->text [{:keys [name type labels metric]}]
  (let [pn (cleansed-name name)
        ^Snapshot snapshot (.getSnapshot metric)]
    (str
      (type-line pn "summary")
      (format-quantiles pn labels snapshot)
      (format "%s_sum%s %s\n" pn (stringify-labels labels) (reduce + (.getValues snapshot)))
      (format "%s_count%s %s\n" pn (stringify-labels labels) (.getCount metric)))))

(defn generate-prometheus-metrics [metrics]
  (let [transform-fn (fn [m] (case (:type m)
                               :meter (counter->text m)
                               :counter (counter->text m)
                               :histogram (histogram->text m)
                               :timer (histogram->text m)
                               :gauge (gauge->text m)))]
    (->> metrics (map transform-fn) (cs/join))))