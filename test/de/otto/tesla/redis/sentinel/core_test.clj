(ns de.otto.tesla.redis.sentinel.core-test
  (:require [clojure.test :refer :all]
            [de.otto.tesla.redis.sentinel.core :refer :all])
  (:import (clojure.lang ExceptionInfo)))

(deftest commands-are-present-test
  (testing "get master command is defined"
    (is (thrown-with-msg? ExceptionInfo #"Redis commands must be called.+" (master "mymaster")))
    (is (thrown-with-msg? ExceptionInfo #"Redis commands must be called.+" (slaves "mymaster")))
    (is (thrown-with-msg? ExceptionInfo #"Redis commands must be called.+" (sentinels "mymaster")))))
