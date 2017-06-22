(ns de.otto.tesla.goo.core-test
  (:require [clojure.test :refer :all]
            [de.otto.tesla.goo.core :as goo]
            [iapetos.core :as p]
            [clojure.tools.logging :as log]
            [clojure.string :as str]))

(use-fixtures :each #(do (goo/clear-default-registry!) (%)))

(deftest register-test
  (testing "it registers a metric under a name"
    (goo/register (p/counter :app/requests))
    (is (@goo/default-registry :app/requests)))

  (testing "it warns if the metric is already registered"
    (goo/clear-default-registry!)
    (let [logged (atom [])]
      (with-redefs [log/log* (fn [_ level _ message] (swap! logged conj [level message]))]
        (goo/register (p/counter :app_requests))
        (goo/register (p/counter :app_requests))
        (let [warning (first @logged)]
          (is (= 1 (count @logged)))
          (is (= (first warning) :warn))
          (is (str/includes? (second warning) "app_requests")))))))

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
