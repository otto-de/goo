(ns de.otto.tesla.redis.sentinel.integration-test
  (:require [clojure.test :refer :all]
            [de.otto.tesla.redis.sentinel.core :as core]
            [clojure.string :as str]
            [taoensso.carmine :as car]
            [com.stuartsierra.component :as c])
  (:import (redis.embedded RedisCluster)
           (redis.embedded.util JedisUtil)))


(def mycluster nil)
(defn cluster-fixture [f]
  (let [cluster (-> (RedisCluster/builder)
                    (.ephemeral)
                    (.sentinelCount 3)
                    (.quorumSize 2)
                    (.replicationGroup "master1" 1)
                    (.replicationGroup "master2" 1)
                    (.replicationGroup "master3" 1)
                    (.build))]
    (prn "start cluster...")
    (.start cluster)
    (prn "Started in-memory-redis-cluster")
    (try
      (with-redefs [mycluster cluster]
        (f))
      (finally
        (.stop cluster)
        (prn "stopped in-memory-redis-cluster")))))

(use-fixtures :once cluster-fixture)

(defn- is-ip-address? [string]
  (re-matches #"[0-9]{1,3} ?\.[0-9]{1,3} ?\.[0-9]{1,3}\.[0-9]{1,3}" string))

(defn- is-port? [port]
  (and (>= port 0)
       (<= port 65535)))

(defn- sentinels [cluster]
  (->> (JedisUtil/sentinelHosts cluster)
       (map (fn [addr] (str/split addr #":")))
       (map (fn [[host port]] {:host host :port (Integer/parseInt port)}))))

(defn- master [sentinels]
  (let [current-master (core/current-master {:master-name "master1" :sentinels sentinels})]
    (prn (format "Current master is: %s" current-master))
    current-master))

(deftest ^:unit find-current-master-test
  (testing "if the current master of a redics cluster is found by the current master command"
    (let [current-master (master (sentinels mycluster))]
      (prn (sentinels mycluster))
      (is (not (empty? current-master)))
      (is (is-ip-address? (:host current-master)))
      (is (is-port? (:port current-master))))
    ))

(deftest ^:unit is-role-test
  (let [sentinels (sentinels mycluster)
        current-master (master sentinels)]
    (testing "if a master is correctly identified as such"
      (is (core/assert-master-role current-master)))

    (testing "if a sentinel is not identified as master"
      (is (not (core/assert-master-role (first sentinels)))))

    (testing "if a non master redis is not identified as master"
      (let [non-master-redis-port (last (.serverPorts mycluster))]
        (is (not (core/assert-master-role {:host "localhost"
                                           :port non-master-redis-port})))))))


(deftest ^:unit wcar-test
  (testing "if a command executed with the info core/wcar macro goes against the master"
    (is (= "master"
           (first (core/wcar {:master-name "master1"
                              :sentinels (sentinels mycluster)
                              :pool :none} (core/role)))))))