(ns de.otto.tesla.goo.metrics_test
  (:require [clojure.test :refer :all]
            [metrics.core :as metrics]
            [de.otto.tesla.goo.metrics :as goo]
            [metrics.meters :as meters]))

(defn clear-ram [f]
  (goo/clear-metrics)
  (f))

(use-fixtures :each clear-ram)

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
    (let [meter (goo/meter "my-metric" [["stage" "live"]])]
      (is (= "my-metric"
             (:name meter)))
      (is (= [["stage" "live"]]
             (:labels meter)))
      (is (= (:metric meter)
             (meters/meter "my-metric.live"))))))