(ns de.otto.tesla.goo.prometheus-test
  (:require [clojure.test :refer :all]
            [de.otto.tesla.goo.prometheus :as prom]
            [metrics.histograms :as hists]
            [metrics.core :as metrics]
            [metrics.counters :as counter]
            [metrics.meters :as meters]
            [metrics.timers :as timer]
            [metrics.gauges :as gauges]
            [de.otto.tesla.goo.metrics :as goo]))

(defn clear-metrics-registry [f]
  (metrics/remove-all-metrics)
  (f))

(use-fixtures :each clear-metrics-registry)

(deftest generate-prometheus-metrics-test
  (testing "Should collect metrics into a single string representation"
    (let [metrics [{:name   "counter1"
                    :labels []
                    :type   :counter
                    :metric (counter/counter "counter1")}
                   {:name   "hist1"
                    :labels []
                    :type   :histogram
                    :metric (hists/histogram "hist1")}
                   {:name   "gauge1"
                    :labels []
                    :type   :gauge
                    :metric (gauges/gauge-fn "gauge1" (constantly 42))}]]
      (is (= (str "# TYPE counter1 counter\n"
                  "counter1 0\n"
                  "# TYPE hist1 summary\n"
                  "hist1{quantile=\"0.01\"} 0.0\n"
                  "hist1{quantile=\"0.05\"} 0.0\n"
                  "hist1{quantile=\"0.5\"} 0.0\n"
                  "hist1{quantile=\"0.9\"} 0.0\n"
                  "hist1{quantile=\"0.99\"} 0.0\n"
                  "hist1_sum 0\n"
                  "hist1_count 0\n"
                  "# TYPE gauge1 gauge\n"
                  "gauge1 42\n")
             (prom/generate-prometheus-metrics metrics)))))

  (testing "Should collect metrics with labels into a single string representation"
    (let [metrics [{:name   "counter1"
                    :labels [["stage" "live"] ["job" "goo"]]
                    :type   :counter
                    :metric (counter/counter "counter1")}
                   {:name   "hist1"
                    :labels [["stage" "live"]]
                    :type   :histogram
                    :metric (hists/histogram "hist1")}
                   {:name   "gauge1"
                    :labels [["stage" "live"]]
                    :type   :gauge
                    :metric (gauges/gauge-fn "gauge1" (constantly 42))}]]
      (is (= (str "# TYPE counter1 counter\n"
                  "counter1{stage=\"live\",job=\"goo\"} 0\n"
                  "# TYPE hist1 summary\n"
                  "hist1{quantile=\"0.01\",stage=\"live\"} 0.0\n"
                  "hist1{quantile=\"0.05\",stage=\"live\"} 0.0\n"
                  "hist1{quantile=\"0.5\",stage=\"live\"} 0.0\n"
                  "hist1{quantile=\"0.9\",stage=\"live\"} 0.0\n"
                  "hist1{quantile=\"0.99\",stage=\"live\"} 0.0\n"
                  "hist1_sum{stage=\"live\"} 0\n"
                  "hist1_count{stage=\"live\"} 0\n"
                  "# TYPE gauge1 gauge\n"
                  "gauge1{stage=\"live\"} 42\n")
             (prom/generate-prometheus-metrics metrics)))))

  (testing "if integration with goo works properly"
    (goo/meter "my_meter" [["stage" "develop"] ["job" "goo"]])
    (is (= (str "# TYPE my_meter counter\n"
                "my_meter{stage=\"develop\",job=\"goo\"} 0\n")
           (prom/generate-prometheus-metrics (goo/metrics-snapshot))))))

(deftest stringify-labels-test
  (testing "if no labels return an empty string"
    (is (= ""
           (prom/stringify-labels []))))

  (testing "if a label gets stringified in a prometheus parseable way"
    (is (= "{stage=\"live\"}"
           (prom/stringify-labels [["stage" "live"]]))))

  (testing "if two labels get stringified in a prometheus parseable way"
    (is (= "{stage=\"live\",job=\"goo\"}"
           (prom/stringify-labels [["stage" "live"]
                                   ["job" "goo"]])))))

(deftest histograms-transformation-test
  (testing "Should transform a histogram into their string representation"
    (let [h (hists/update! (hists/histogram "foobar.hist1") 5)]
      (is (= (str "# TYPE hist1 summary\n"
                  "hist1{quantile=\"0.01\"} 5.0\n"
                  "hist1{quantile=\"0.05\"} 5.0\n"
                  "hist1{quantile=\"0.5\"} 5.0\n"
                  "hist1{quantile=\"0.9\"} 5.0\n"
                  "hist1{quantile=\"0.99\"} 5.0\n"
                  "hist1_sum 5\n"
                  "hist1_count 1\n")
             (prom/histogram->text {:name   "hist1"
                                    :labels []
                                    :type   :histogram
                                    :metric h})))))

  (testing "Should transform a histogram with labels into their string representation"
    (let [h (hists/update! (hists/histogram "foobar.hist2") 5)]
      (is (= (str "# TYPE hist2 summary\n"
                  "hist2{quantile=\"0.01\",stage=\"live\"} 5.0\n"
                  "hist2{quantile=\"0.05\",stage=\"live\"} 5.0\n"
                  "hist2{quantile=\"0.5\",stage=\"live\"} 5.0\n"
                  "hist2{quantile=\"0.9\",stage=\"live\"} 5.0\n"
                  "hist2{quantile=\"0.99\",stage=\"live\"} 5.0\n"
                  "hist2_sum{stage=\"live\"} 5\n"
                  "hist2_count{stage=\"live\"} 1\n")
             (prom/histogram->text {:name   "hist2"
                                    :labels [["stage" "live"]]
                                    :type   :histogram
                                    :metric h}))))))

(deftest counters-transformation-test
  (testing "Should transform a counter into their string representation"
    (let [c (counter/inc! (counter/counter "foobar.counter2") 3)]
      (is (= (str "# TYPE counter2 counter\n"
                  "counter2 3\n")
             (prom/counter->text {:name   "counter2"
                                  :labels []
                                  :type   :counter
                                  :metric c})))))

  (testing "Should transform a counter with label into their string representation"
    (let [c (counter/inc! (counter/counter "foobar.counter3") 3)]
      (is (= (str "# TYPE counter3 counter\n"
                  "counter3{stage=\"live\"} 3\n")
             (prom/counter->text {:name   "counter3"
                                  :labels [["stage" "live"]]
                                  :type   :counter
                                  :metric c}))))))

(deftest meters-transformation-test
  (testing "Should transform a meter into a string representation and map it to a prometheus counter"
    (let [m (meters/mark! (meters/meter "meter1"))]
      (is (= (str "# TYPE meter1 counter\n"
                  "meter1 1\n")
             (prom/counter->text {:name   "meter1"
                                  :labels []
                                  :type   :meter
                                  :metric m})))))

  (testing "Should transform a meter with labels into a string representation and map it to a prometheus counter"
    (let [m (meters/mark! (meters/meter "meter2"))]
      (is (= (str "# TYPE meter2 counter\n"
                  "meter2{stage=\"live\"} 1\n")
             (prom/counter->text {:name   "meter2"
                                  :labels [["stage" "live"]]
                                  :type   :meter
                                  :metric m}))))))

(deftest gauges-transformation-test
  (testing "Should transform a gauge into a string representation"
    (let [g1 (gauges/gauge-fn "foobar.gauge1" (constantly 42))
          g-invalid (gauges/gauge-fn "foobar.gauge1" (constantly "No number"))]
      (is (= (str "# TYPE gauge1 gauge\n"
                  "gauge1 42\n")
             (prom/gauge->text {:name   "gauge1"
                                :labels []
                                :type   :gauge
                                :metric g1})))
      (is (= nil
             (prom/gauge->text {:name   "my_name"
                                :labels []
                                :type   :gauge
                                :metric g-invalid})))))

  (testing "Should transform a gauge with labels into a string representation"
    (let [g1 (gauges/gauge-fn "foobar.gauge1" (constantly 42))]
      (is (= (str "# TYPE gauge2 gauge\n"
                  "gauge2{stage=\"live\"} 42\n")
             (prom/gauge->text {:name   "gauge2"
                                :labels [["stage" "live"]]
                                :type   :gauge
                                :metric g1}))))))
