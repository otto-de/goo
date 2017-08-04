(ns de.otto.goo.CallbackGauge
  (:import (io.prometheus.client Collector Collector$MetricFamilySamples Collector$Type Collector$MetricFamilySamples$Sample)
           (clojure.lang IFn))
  (:gen-class
    :name de.otto.goo.CallbackGauge
    :extends io.prometheus.client.Collector
    :constructors {[String String clojure.lang.IFn] []}
    :state state
    :init init))

(defn -init [^String name ^String help ^IFn callback-fn]
  [[] (atom {:name name :help help :callback-fn callback-fn})])

(defn -collect [this]
  (let [{:keys [name help callback-fn]} @(.state this)]
    (->> (callback-fn)
         (double)
         (Collector$MetricFamilySamples$Sample. name [] [])
         (conj [])
         (Collector$MetricFamilySamples. name (Collector$Type/GAUGE) help)
         (conj []))))