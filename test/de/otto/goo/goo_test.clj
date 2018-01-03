(ns de.otto.goo.goo-test
  (:require [clojure.test :refer :all]
            [de.otto.goo.goo :as metrics]
            [iapetos.core :as p]
            [clojure.tools.logging :as log]
            [clojure.string :as str])
  (:import (io.prometheus.client Collector$MetricFamilySamples$Sample)))

(use-fixtures :each #(do (metrics/clear-default-registry!) (%)))

(deftest register-test
  (testing "it registers a metric under a name"
    (metrics/register! (p/counter :app/requests))
    (is ((metrics/snapshot) :app/requests)))

  (testing "it warns if the metric is already registered"
    (metrics/clear-default-registry!)
    (let [logged (atom [])]
      (with-redefs [log/log* (fn [_ level _ message] (swap! logged conj [level message]))]
        (metrics/register! (p/counter :app_requests))
        (metrics/register! (p/counter :app_requests))
        (let [warning (first @logged)]
          (is (= 1 (count @logged)))
          (is (= (first warning) :warn))
          (is (str/includes? (second warning) "app_requests")))))))

(deftest register-test
  (testing "it registers a metric under a name"
    (metrics/register! (p/counter :app/requests))
    (is ((metrics/snapshot) :app/requests)))

  (testing "it warns if the metric is already registered"
    (metrics/clear-default-registry!)
    (let [logged (atom [])]
      (with-redefs [log/log* (fn [_ level _ message] (swap! logged conj [level message]))]
        (metrics/register! (p/counter :app_requests))
        (metrics/register! (p/counter :app_requests))
        (let [warning (first @logged)]
          (is (= 1 (count @logged)))
          (is (= (first warning) :warn))
          (is (str/includes? (second warning) "app_requests")))))))

(deftest with-default-registry-test
  (testing "it uses the default registry for lookup of metrics"
    (metrics/register! (p/counter :app/requests))
    (metrics/with-default-registry (p/inc :app/requests))
    (metrics/with-default-registry (p/inc :app/requests))
    (is (= 2.0
           (.get ((metrics/snapshot) :app/requests))))))

(deftest register+execute-test
  (testing "it allows to register metrics with the default registry on the fly"
    (metrics/register+execute! :app/requests (p/counter {}) (p/inc {}))
    (metrics/register+execute! :app/requests (p/counter {}) (p/inc {}))
    (is (= 2.0
           (.get ((metrics/snapshot) :app/requests)))))

  (testing "it allows to register metrics with the default registry with labels on the fly"
    (metrics/register+execute! :app/requests2 (p/counter {:labels [:rc]}) (p/inc {:rc 200}))
    (metrics/register+execute! :app/requests2 (p/counter {}) (p/inc {:rc 500}))
    (is (= 1.0
           (.get ((metrics/snapshot) :app/requests2 {:rc 200}))
           (.get ((metrics/snapshot) :app/requests2 {:rc 500}))))))

(deftest inc-test
  (testing "it calls the iapetos inc function with metric name"
    (let [called-with (atom nil)]
      (with-redefs [p/inc (fn [_ metric] (swap! called-with (constantly metric)))]
        (metrics/register! (p/counter :cntr1))
        (metrics/inc! :cntr1)
        (is (= :cntr1
               @called-with)))))

  (testing "it calls the iapetos inc function with metric name and labels"
    (let [called-with (atom nil)]
      (with-redefs [p/inc (fn [_ metric lb] (swap! called-with (constantly [metric lb])))]
        (metrics/register! (p/counter :cntr2 {:labels [:a]}))
        (metrics/inc! :cntr2 {:a "a"})
        (is (= [:cntr2 {:a "a"}]
               @called-with)))))
  (testing "counter can be incremented"
    (metrics/clear-default-registry!)
    (metrics/register-counter! :my/counter {})
    (is (= 0.0 (.get ((metrics/snapshot) :my/counter))))
    (metrics/inc! :my/counter {})
    (is (= 1.0 (.get ((metrics/snapshot) :my/counter)))))
  (testing "counter with labels can be incremented"
    (metrics/clear-default-registry!)
    (metrics/register-counter! :my/counter {:labels [:foo]})
    (is (= 0.0 (.get ((metrics/snapshot) :my/counter))))
    (metrics/inc! :my/counter {:foo :bar})
    (is (= 0.0 (.get ((metrics/snapshot) :my/counter))))
    (is (= 1.0 (.get ((metrics/snapshot) :my/counter {:foo :bar}))))))



(deftest dec-test
  (testing "it calls the iapetos dec function with metric name"
    (let [called-with (atom nil)]
      (with-redefs [p/dec (fn [_ metric] (swap! called-with (constantly metric)))]
        (metrics/register! (p/gauge :gauge1))
        (metrics/dec! :gauge1)
        (is (= :gauge1
               @called-with)))))

  (testing "it calls the iapetos dec function with metric name and labels"
    (let [called-with (atom nil)]
      (with-redefs [p/dec (fn [_ metric lb] (swap! called-with (constantly [metric lb])))]
        (metrics/register! (p/gauge :gauge2 {:labels [:a]}))
        (metrics/dec! :gauge2 {:a "a"})
        (is (= [:gauge2 {:a "a"}]
               @called-with))))))

(deftest set-macro-test
  (testing "it calls the iapetos set function with metric name"
    (let [called-with (atom nil)]
      (with-redefs [p/set (fn [_ metric amount] (swap! called-with (constantly [metric amount])))]
        (metrics/register! (p/gauge :gauge1))
        (metrics/update! :gauge1 5)
        (is (= [:gauge1 5]
               @called-with)))))

  (testing "it calls the iapetos set function with metric name and labels"
    (let [called-with (atom nil)]
      (with-redefs [p/set (fn [_ metric lb amount] (swap! called-with (constantly [metric lb amount])))]
        (metrics/register! (p/gauge :gauge2 {:labels [:a]}))
        (metrics/update! :gauge2 {:a "a"} 5)
        (is (= [:gauge2 {:a "a"} 5]
               @called-with))))))

(deftest get-from-default-registry-test
  (testing "it returns a metric with a specific label"
    (metrics/register! (p/counter :counter1) {:labels [:a]})
    (is (= ((metrics/snapshot) :counter1)
           (metrics/get-from-default-registry :counter1 {:a "a"})))))

(deftest compojure-path->url-path-test
  (testing "it removes params from path"
    (is (= "/path1/path2"
           (#'metrics/compojure-path->url-path "/path1/path2/:id")))
    (is (= "/path1/path2"
           (#'metrics/compojure-path->url-path "/path1/:id/path2")))))

(defn value
  ([gauge-name]
   (-> (metrics/snapshot) (.raw) (.getSampleValue (name gauge-name))))
  ([name lables values]
   (-> (metrics/snapshot) (.raw) (.getSampleValue name (into-array String lables) (into-array values)))))

(deftest timing-middleware-test
  (let [response     {:status 200}
        handler      (fn [& _] (Thread/sleep 90) response)
        get-request  {:compojure/route [:get "/path1/path2/:id"]}
        get2-request {:compojure/route [:get "dummy"]}
        post-request {:compojure/route [:post "/path1/path2/:id"]}]
    (testing "it returns the response of the handler fn"
      (is (= response
             ((metrics/timing-middleware handler) get2-request))))
    (testing "it returns nil if the handler returns nil"
      (is (= nil
             ((metrics/timing-middleware (constantly nil)) get2-request))))
    (testing "it creates metrics for a given wrapped get request"
      (metrics/clear-default-registry!)
      ((metrics/timing-middleware handler) get-request)
      (let [snapshot (metrics/snapshot)]
        (is (= 1.0
               (value "http_duration_in_s_count" ["path" "method" "rc"] ["/path1/path2" ":get" "200"])))
        (is (= [0.0 0.0 0.0 0.0 1.0 1.0 1.0 1.0]
               (seq (.buckets (.get (snapshot :http/duration-in-s {:rc 200 :method :get :path "/path1/path2"}))))))))
    (testing "it creates metrics for a two post requests"
      (metrics/clear-default-registry!)
      ((metrics/timing-middleware handler) post-request)
      ((metrics/timing-middleware handler) post-request)
      (let [snapshot (metrics/snapshot)]
        (is (= 2.0
               (value "http_duration_in_s_count" ["path" "method" "rc"] ["/path1/path2" ":post" "200"])))
        (is (= [0.0 0.0 0.0 0.0 2.0 2.0 2.0 2.0]
               (seq (.buckets (.get (snapshot :http/duration-in-s {:rc 200 :method :post :path "/path1/path2"}))))))))))

(deftest timed-test
  (testing "for the timed macro on can determine the buckets optionally"
    (metrics/clear-default-registry!)
    (let [times-called (atom 0)]
      (= 1 (metrics/timed :mytest/metric {} (do (swap! times-called inc) (Thread/sleep 2) @times-called) [0.001 0.1]))
      (is (= [0.0 1.0 1.0] (seq (.buckets (.get ((metrics/snapshot) :mytest/metric {:exception :none}))))))
      (is (= 1 @times-called))))
  (testing "timed macro should not call register repeatedly"
    (metrics/clear-default-registry!)
    (metrics/timed :register/cached {} (do))
    (with-redefs [metrics/register-with-action (fn [& _] (throw (Exception. "register called!")))]
      (metrics/timed :register/cached {} (do)))))

(deftest measured-execution-test
  (testing "it measures the execution time of the given body"
    (metrics/clear-default-registry!)
    (let [times-called (atom 0)]
      (metrics/measured-execution :test-fn #(do (swap! times-called inc) (Thread/sleep 2)))
      (is (= 0.0
             (first (.-buckets (.get ((metrics/snapshot) :measured-execution/execution-time-in-s {:function :test-fn :exception :none}))))))
      (is (= 1 @times-called))
      (is (= 1.0
             (second (.-buckets (.get ((metrics/snapshot) :measured-execution/execution-time-in-s {:function :test-fn :exception :none}))))))))
  (testing "it measures exceptions"
    (try
      (metrics/measured-execution :test-fn #(do (Thread/sleep 2) (throw (RuntimeException.))))
      (catch RuntimeException _))
    (is (= 0.0
           (first (.-buckets (.get ((metrics/snapshot) :measured-execution/execution-time-in-s {:function :test-fn :exception "java.lang.RuntimeException"}))))))
    (is (= 1.0
           (second (.-buckets (.get ((metrics/snapshot) :measured-execution/execution-time-in-s {:function  :test-fn
                                                                                                 :exception "java.lang.RuntimeException"})))))))
  (testing "it returns something"
    (is (= "test"
           (metrics/measured-execution :fn-name (constantly "test"))))))


(deftest callback-gauge-test
  (testing "callback gauge val changes if callback function returns different val"
    (metrics/clear-default-registry!)
    (let [foo (atom 1)]
      (metrics/register-callback-gauge! "callback" "help" #(deref foo))
      (is (= 1.0 (value "callback")))
      (swap! foo inc)
      (is (= 2.0 (value "callback")))))
  (testing "use labels"
    (metrics/clear-default-registry!)
    (let [foo (atom 1)
          bar (atom 2)]
      (metrics/register-callback-gauge! "callback" "help" #(deref foo) {"atom" "foo"})
      (metrics/register-callback-gauge! "callback" "help" #(deref bar) {"atom" "bar"})
      (is (= 1.0 (value "callback" ["atom"] ["foo"])))
      (is (= 2.0 (value "callback" ["atom"] ["bar"])))
      (swap! foo inc)
      (swap! bar inc)
      (is (= 2.0 (value "callback" ["atom"] ["foo"])))
      (is (= 3.0 (value "callback" ["atom"] ["bar"])))))
  (testing "namespaced keywords work too"
    (metrics/clear-default-registry!)
    (let [foo (atom 1)
          bar (atom 8)]
      (metrics/register-callback-gauge! :my/callback "help" #(deref foo) {:atom :foo})
      (is (= 1.0 (value "my_callback" ["atom"] ["foo"])))
      (metrics/register-callback-gauge! :my/callback "help" #(deref bar) {:atom :bar})
      (is (= 8.0 (value "my_callback" ["atom"] ["bar"])))
      (swap! bar inc)
      (is (= 9.0 (value "my_callback" ["atom"] ["bar"]))))))

(deftest serialize-sample-test
  (testing "it serializes a single sample"
    (let [sample (Collector$MetricFamilySamples$Sample. "name" [] [] 2.0)]
      (is (= ["prefix" "name" [] " 2.0 12345\n"]
             (metrics/serialize-sample sample "prefix" 12345))))
    (let [sample (Collector$MetricFamilySamples$Sample. "name" ["l1" "l2"] ["v1" "v2"] 1.4)]
      (is (= ["prefix" "name" [".l1.v1" ".l2.v2"] " 1.4 42\n"]
             (metrics/serialize-sample sample "prefix" 42)))))

  (testing "it cleanses names and labels"
    (let [sample (Collector$MetricFamilySamples$Sample. "NämE!" ["l$1" "l.2"] ["v@1" "v😀2"] 99.9)]
      (is (= ["prefix" "N_mE_" [".l_1.v_1" ".l_2.v_2"] " 99.9 987\n"]
             (metrics/serialize-sample sample "prefix" 987))))))
