(ns de.otto.tesla.goo.metrics_test
  (:require [clojure.test :refer :all]
            [metrics.core :as metrics]
            [de.otto.tesla.goo.metrics :as goo]
            [metrics.meters :as meters]
            [metrics.gauges :as gauges]
            [metrics.timers :as timers]
            [metrics.histograms :as hist]
            [metrics.counters :as counters]))

(defn clear-ram [f]
  (goo/clear-metrics)
  (f))

(use-fixtures :each clear-ram)

(deftest clear-metrics-test
  (testing "if a created meter gets deleted"
    (metrics/remove-all-metrics)
    (goo/meter "mymeter" ["stage" "dev"])
    (goo/clear-metrics)
    (is (empty? @goo/metrics))
    (is (empty? (.getMeters metrics/default-registry)))))

(deftest create-meter-test
  (testing "if a meter gets created"
    (let [meter (goo/meter "my-metric" [])]
      (is (= "my-metric"
             (:name meter)))
      (is (= []
             (:labels meter)))
      (is (= (:metric meter)
             (meters/meter "my-metric")))))

  (testing "if a meter with labels gets created"
    (let [meter (goo/meter "my-labeled-metric" [["stage" "live"]])]
      (is (= "my-labeled-metric"
             (:name meter)))
      (is (= [["stage" "live"]]
             (:labels meter)))
      (is (= (:metric meter)
             (meters/meter "my-labeled-metric.live")))))

  (testing "if different labels result in different meter"
    (let [meter (goo/meter "my-labeled-metric" [["stage" "live"]
                                                ["label1" "value1"]])]
      (is (= "my-labeled-metric"
             (:name meter)))
      (is (= [["stage" "live"]
              ["label1" "value1"]]
             (:labels meter)))
      (is (= (:metric meter)
             (meters/meter "my-labeled-metric.live.value1"))))))

(deftest create-gauge-test
  (testing "if a gauge gets created"
    (let [gauge (goo/gauge "my-metric" [])]
      (is (= "my-metric"
             (:name gauge)))
      (is (= []
             (:labels gauge)))
      (is (= (:metric gauge)
             (gauges/gauge "my-metric")))))

  (testing "if a gauge with labels gets created"
    (let [gauge (goo/gauge "my-labeled-metric" [["stage" "live"]])]
      (is (= "my-labeled-metric"
             (:name gauge)))
      (is (= [["stage" "live"]]
             (:labels gauge)))
      (is (= (:metric gauge)
             (gauges/gauge "my-labeled-metric.live"))))))

(deftest create-histogram-test
  (testing "if a histogram gets created"
    (let [histogram (goo/histogram "my-metric" [])]
      (is (= "my-metric"
             (:name histogram)))
      (is (= []
             (:labels histogram)))
      (is (= (:metric histogram)
             (hist/histogram "my-metric")))))

  (testing "if a histogram with labels gets created"
    (let [histogram (goo/histogram "my-labeled-metric" [["stage" "live"]])]
      (is (= "my-labeled-metric"
             (:name histogram)))
      (is (= [["stage" "live"]]
             (:labels histogram)))
      (is (= (:metric histogram)
             (hist/histogram "my-labeled-metric.live"))))))

(deftest create-counter-test
  (testing "if a counter gets created"
    (let [counter (goo/counter "my-metric" [])]
      (is (= "my-metric"
             (:name counter)))
      (is (= []
             (:labels counter)))
      (is (= (:metric counter)
             (counters/counter "my-metric")))))

  (testing "if a counter with labels gets created"
    (let [counter (goo/counter "my-labeled-metric" [["stage" "live"]])]
      (is (= "my-labeled-metric"
             (:name counter)))
      (is (= [["stage" "live"]]
             (:labels counter)))
      (is (= (:metric counter)
             (counters/counter "my-labeled-metric.live"))))))

(deftest create-timer-test
  (testing "if a timer gets created"
    (let [timer (goo/timer "my-metric" [])]
      (is (= "my-metric"
             (:name timer)))
      (is (= []
             (:labels timer)))
      (is (= (:metric timer)
             (timers/timer "my-metric")))))

  (testing "if a timer with labels gets created"
    (let [timer (goo/timer "my-labeled-metric" [["stage" "live"]])]
      (is (= "my-labeled-metric"
             (:name timer)))
      (is (= [["stage" "live"]]
             (:labels timer)))
      (is (= (:metric timer)
             (timers/timer "my-labeled-metric.live"))))))
