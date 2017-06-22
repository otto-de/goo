(ns de.otto.tesla.goo.exporter.graphite
  (:require [com.stuartsierra.component :as c]
            [de.otto.tesla.stateful.scheduler :as sched]
            [de.otto.tesla.goo.core :as goo]
            [overtone.at-at :as at]
            [clojure.tools.logging :as log]
            [environ.core :as env]
            [clojure.string :as cs])
  (:import (goo GraphiteExporter)))

(defn hostname []
  (or (:host env/env) (:host-name env/env) (:hostname env/env)
      "localhost"))

(defn build-prefix [prefix app-hostname hostname-transform]
  (let [transformed (hostname-transform app-hostname)
        p (cs/join "." (remove nil? [prefix transformed]))]
    (str p (when-not (empty? p) "."))))


(defn push-to-graphite [graphite-config hostname-transform]
  (try
    (let [host (:host graphite-config)
          port (Integer/parseInt (:port graphite-config))
          prefix (:prefix graphite-config)
          sane-prefix (build-prefix prefix (hostname) hostname-transform)
          r (.raw (goo/snapshot))]
      (log/infof "Reporting to Graphite %s:%s with %s as prefix" host port sane-prefix)
      (GraphiteExporter/push host port sane-prefix r))
    (catch Exception e
      (log/error e "Error while Reporting to Graphite"))))

(defn start!
  ([graphite-config scheduler]
   (start! graphite-config scheduler identity))

  ([graphite-config scheduler hostname-transform]
   (let [interval-in-ms (* 1000 (:interval-in-s graphite-config))]
     (log/info "Starting goo graphite exporter")
     (at/every interval-in-ms #(push-to-graphite graphite-config hostname-transform) (sched/pool scheduler) :desc "Goo Graphite"))))

