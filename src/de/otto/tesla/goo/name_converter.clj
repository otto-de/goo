(ns de.otto.tesla.goo.name-converter
  (:require [clojure.string :as str]))

(defn to-graphite [{:keys [name labels]}]
  (let [label-str (some->> labels
                           (map second)
                           (str/join "."))]
    (if (empty? label-str)
      [name]
      [(format "%s.%s" name label-str)])))