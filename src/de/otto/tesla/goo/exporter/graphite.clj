(ns de.otto.tesla.goo.exporter.graphite
  (:require [com.stuartsierra.component :as c]
            [de.otto.tesla.stateful.scheduler :as sched]
            [de.otto.tesla.goo.core :as goo]
            [overtone.at-at :as at]
            [clojure.tools.logging :as log]
            [environ.core :as env]
            [clojure.string :as cs])
  (:import (goo GraphiteExporter)))

(defn- short-hostname [hostname]
  (re-find #"[^.]*" hostname))

(defn hostname []
  (or (:host env/env) (:host-name env/env) (:hostname env/env)
      "localhost"))

(defn build-prefix [{:keys [prefix include-hostname]} app-hostname]
  (let [hostname-part (case include-hostname
                        :full (identity app-hostname)
                        :first-part (short-hostname app-hostname)
                        nil nil)
        p (cs/join "." (remove nil? [prefix hostname-part]))]
    (str p (when-not (empty? p) "."))))

(defn push-to-graphite [graphite-config]
  (try
    (let [host (:host graphite-config)
          port (Integer/parseInt (:port graphite-config))
          sane-prefix (build-prefix graphite-config (hostname))
          r (.raw (goo/snapshot))]
      (log/infof "Reporting to Graphite %s:%s with %s as prefix" host port sane-prefix)
      (GraphiteExporter/push host port sane-prefix r))
    (catch Exception e
      (log/error e "Error while Reporting to Graphite"))))

(defn start! [graphite-config scheduler]
  (let [interval-in-ms (* 1000 (:interval-in-s graphite-config))]
    (log/info "Starting goo graphite exporter")
    (at/every interval-in-ms #(push-to-graphite graphite-config) (sched/pool scheduler) :desc "Goo Graphite")))

