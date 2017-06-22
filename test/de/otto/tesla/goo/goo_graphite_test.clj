(ns de.otto.tesla.goo.goo-graphite-test
  (:require [clojure.test :refer :all]
            [de.otto.tesla.goo.goo-graphite :as gg]))

(deftest build-prefix-test
  (is (= "prefix.hostname." (gg/build-prefix "prefix" "hostname" identity)))
  (is (= "hostname." (gg/build-prefix nil "hostname" identity)))
  (is (= "prefix." (gg/build-prefix "prefix" nil identity)))
  (is (= "" (gg/build-prefix nil nil identity))))

(deftest hostname-transform-test
  (is (= "prefix.abc." (gg/build-prefix "prefix" "host.name" (fn [hn] "abc"))))
  (is (= "prefix.host." (gg/build-prefix "prefix" "host.name" #(re-find #"[^.]*" %)))))
