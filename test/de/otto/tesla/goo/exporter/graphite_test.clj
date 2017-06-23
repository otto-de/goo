(ns de.otto.tesla.goo.exporter.graphite-test
  (:require [clojure.test :refer :all]
            [de.otto.tesla.goo.exporter.graphite :as gg]))

(deftest build-prefix-test
  (testing "prefix handling"
    (is (= "prefix." (gg/build-prefix {:prefix "prefix"} "hostname")))
    (is (= "" (gg/build-prefix {:prefix nil} "hostname")))
    (is (= "prefix." (gg/build-prefix {:prefix "prefix"} nil)))
    (is (= "" (gg/build-prefix {:prefix nil} nil))))

  (testing "hostname handling"
    (is (= "prefix.host.name." (gg/build-prefix {:prefix "prefix" :include-hostname :full} "host.name")))
    (is (= "prefix.host." (gg/build-prefix {:prefix "prefix" :include-hostname :first-part} "host.name")))
    (is (= "host.name." (gg/build-prefix {:include-hostname :full} "host.name")))))
