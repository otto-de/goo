(ns de.otto.tesla.goo.goo-console
  (:require [clojure.tools.logging :as log]
            [overtone.at-at :as at]
            [de.otto.tesla.stateful.scheduler :as sched]
            [de.otto.tesla.goo.goo :as goo]))

(defn write-to-console [console-config]
  (log/info "Goo Console Reporting:")
  (log/info (goo/text-format)))

(defn start! [console-config scheduler]
  (let [interval-in-ms (* 1000 (:interval-in-s console-config))]
    (log/info "Starting goo console exporter")
    (at/every interval-in-ms #(write-to-console console-config ) (sched/pool scheduler) :desc "Goo Console")))
