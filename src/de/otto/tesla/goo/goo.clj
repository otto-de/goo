(ns de.otto.tesla.goo.goo
  (:require [iapetos.core :as p]
            [iapetos.export :as e]))

(def empty-registry (p/collector-registry))

(def default-registry (atom empty-registry))

(defn clear-default-registry! []
  (.clear (.raw @default-registry))                         ; for unknown reasons there is still state left in the underlying CollectorRegistry
  (reset! default-registry (p/collector-registry)))

(defn register [& ms]
  (swap! default-registry (fn [r] (apply p/register r ms))))

(defmacro with-default-registry [& ops]
  `(-> @default-registry ~@ops))

(defmacro register+execute [name m op]
  `(do
     (when-not (@default-registry ~name)
       (register (~(first m) ~name ~@(rest m))))
     (~(first op) @default-registry ~name ~@(rest op))))

(defn get-from-default-registry [name]
  (@default-registry name))

(defn text-format []
  (e/text-format @default-registry))
