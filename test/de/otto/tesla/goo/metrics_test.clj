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
    (goo/meter "mymeter" [["stage" "dev"]])
    (goo/clear-metrics)
    (is (empty? @goo/metrics))
    (is (empty? (.getMeters metrics/default-registry)))))

(deftest create-meter-test
  (testing "if a meter gets created"
    (is (= (goo/meter "my-metric" [])
           (meters/meter "my-metric"))))

  (testing "if a meter with labels gets created"
    (is (= (goo/meter "my-labeled-metric" [["stage" "live"]])
           (meters/meter "my-labeled-metric.live"))))

  (testing "if different labels result in different meter"
    (is (= (goo/meter "my-labeled-metric" [["stage" "live"]
                                           ["label1" "value1"]])
           (meters/meter "my-labeled-metric.live.value1")))))

(deftest create-gauge-test
  (testing "if a gauge gets created"
    (is (= (goo/gauge "my-metric" [])
           (gauges/gauge "my-metric"))))

  (testing "if a gauge with labels gets created"
    (is (= (goo/gauge "my-labeled-metric" [["stage" "live"]])
           (gauges/gauge "my-labeled-metric.live")))))

(deftest create-histogram-test
  (testing "if a histogram gets created"
    (is (= (goo/histogram "my-metric" [])
           (hist/histogram "my-metric"))))

  (testing "if a histogram with labels gets created"
    (is (= (goo/histogram "my-labeled-metric" [["stage" "live"]])
           (hist/histogram "my-labeled-metric.live")))))

(deftest create-counter-test
  (testing "if a counter gets created"
    (is (= (goo/counter "my-metric" [])
           (counters/counter "my-metric"))))

  (testing "if a counter with labels gets created"
    (is (= (goo/counter "my-labeled-metric" [["stage" "live"]])
           (counters/counter "my-labeled-metric.live")))))

(deftest create-timer-test
  (testing "if a timer gets created"
    (is (= (goo/timer "my-metric" [])
           (timers/timer "my-metric"))))

  (testing "if a timer with labels gets created"
    (is (= (goo/timer "my-labeled-metric" [["stage" "live"]])
           (timers/timer "my-labeled-metric.live")))))
