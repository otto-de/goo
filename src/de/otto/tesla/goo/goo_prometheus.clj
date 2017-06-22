(ns de.otto.tesla.goo.goo-prometheus
  (:require [de.otto.tesla.stateful.handler :as handler]
            [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [compojure.core :as c]
            [de.otto.tesla.goo.goo :as goo]))

(defn metrics-response []
  {:status  200
   :headers {"Content-Type" "text/plain"}
   :body    (goo/text-format)})

(defn make-handler [{metrics-path :metrics-path}]
  (c/routes (c/GET metrics-path [] (metrics-response))))


(defn register-endpoint! [prometheus-config handler]
  (log/info "Register goo prometheus endpoint")
  (handler/register-handler handler (make-handler prometheus-config)))
