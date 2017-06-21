(ns de.otto.tesla.goo.goo-graphite
  (:require [com.stuartsierra.component :as c]
            [de.otto.tesla.stateful.scheduler :as sched]
            [de.otto.tesla.goo.goo :as goo]
            [overtone.at-at :as at]
            [clojure.tools.logging :as log])
  (:import (goo GraphiteExporter)))


(defn push-to-graphite [config]
  (try
    (let [host (get-in config [:config :graphite-host])
          port (Integer/parseInt (get-in config [:config :graphite-port]))
          r (.raw (goo/snapshot))]
      (log/infof "Reporting to Graphite %s:%s" host port)
      (GraphiteExporter/push host port r))
    (catch Exception e
      (log/error e "Error while Reporting to Graphite"))))

(defrecord GooGraphite [config scheduler]
  c/Lifecycle
  (start [self]
    (let [interval-in-ms (* 1000 (get-in config [:config :graphite-interval-seconds]))]
      (log/info "-> Starting Goo Graphite")
      (at/every interval-in-ms #(push-to-graphite config) (sched/pool scheduler) :desc "Goo Graphite"))
    self)

  (stop [self]
    (log/info "<- Stopping Goo Graphite")
    self))

(defn new-goo-graphite []
  (map->GooGraphite {}))


