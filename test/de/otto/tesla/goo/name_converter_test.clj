(ns de.otto.tesla.goo.name-converter-test
  (:require [clojure.test :refer :all]
            [de.otto.tesla.goo.name-converter :as nc]))

(deftest to-graphite-test
  (testing "if a simple metric get's converted"
    (is (= ["my-metric"]
           (nc/to-graphite "my-metric" []))))
  (testing "if a metric with a label gets converted"
    (is (= ["my-metric.live"]
           (nc/to-graphite "my-metric" [["stage" "live"]])))))
