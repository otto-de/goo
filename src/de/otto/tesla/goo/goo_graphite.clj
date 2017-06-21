(ns de.otto.tesla.goo.goo-graphite
  (:require [com.stuartsierra.component :as c]
            [de.otto.tesla.stateful.scheduler :as sched]
            [de.otto.tesla.goo.goo :as goo]
            [overtone.at-at :as at]
            [clojure.tools.logging :as log])
  (:import (goo GraphiteExporter)))


(defrecord GooGraphite [config scheduler]
  c/Lifecycle
  (start [self]
    (let [host (get-in config [:config :graphite-host])
          port (get-in config [:config :graphite-port])
          exporter (GraphiteExporter. host port)
          interval-in-ms (* 1000 (get-in config [:config :graphite-interval-seconds]))
          r (.raw (goo/snapshot))]
      (log/info "Starting Goo Graphite")

      (at/every interval-in-ms #(.push exporter r) (sched/pool scheduler) :desc "Goo Graphite"))
    )
  (stop [self]
    (log/info "Stopping Goo Graphite")
    self))


(defn new-goo-graphite []
  (map->GooGraphite {}))


