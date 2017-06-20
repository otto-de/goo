(ns de.otto.tesla.goo.goo-test
  (:require [clojure.test :refer :all]
            [de.otto.tesla.goo.goo :as goo]
            [iapetos.core :as p]))

(use-fixtures :each #(do (goo/clear-default-registry!) (%)))

(deftest register-test
  (testing "it registers a metric under a name"
    (goo/register (p/counter :app/requests))
    (is (@goo/default-registry :app/requests))))

(deftest with-default-registry-test
  (testing "it uses the default registry for lookup of metrics"
    (goo/register (p/counter :app/requests))
    (goo/with-default-registry (p/inc :app/requests))
    (goo/with-default-registry (p/inc :app/requests))
    (is (= 2.0
           (.get (@goo/default-registry :app/requests))))))

(deftest register+execute-test
  (testing "it allows to register metrics with the default registry on the fly"
    (goo/register+execute :app/requests (p/counter {}) (p/inc {}))
    (goo/register+execute :app/requests (p/counter {}) (p/inc {}))
    (is (= 2.0
           (.get (@goo/default-registry :app/requests))))))
