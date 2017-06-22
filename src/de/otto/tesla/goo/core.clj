(ns de.otto.tesla.goo.core
  (:require [iapetos.core :as p]
            [iapetos.export :as e]
            [clojure.tools.logging :as log]))

(def empty-registry (p/collector-registry))

(def default-registry (atom empty-registry))

(defn snapshot []
  @default-registry)

(defn clear-default-registry! []
  (.clear (.raw (snapshot)))                         ; for unknown reasons there is still state left in the underlying CollectorRegistry
  (reset! default-registry (p/collector-registry)))

(defn register [& ms]
  (try
    (swap! default-registry (fn [r] (apply p/register r ms)))
    (catch IllegalArgumentException e
      (log/warn (.getMessage e)))))

(defmacro with-default-registry [& ops]
  `(-> (snapshot) ~@ops))

(defmacro register+execute [name m op]
  `(do
     (when-not ((snapshot) ~name)
       (register (~(first m) ~name ~@(rest m))))
     (~(first op) (snapshot) ~name ~@(rest op))))

(defn get-from-default-registry [name]
  ((snapshot) name))

(defn text-format []
  (e/text-format (snapshot)))

