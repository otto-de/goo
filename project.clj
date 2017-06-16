(defproject de.otto.tesla/goo "0.0.1-SNAPSHOT"
  :repositories [["central" {:url "http://artifactory.lhotse.ov.otto.de/artifactory/maven-central-remote"}]
                 ["clojars" {:url "http://artifactory.lhotse.ov.otto.de/artifactory/clojars-releases-remote"}]
                 ["nexus-releases" {:url "http://artifactory.lhotse.ov.otto.de/artifactory/nexus-releases-remote"}]
                 ["releases" {:url           "http://artifactory.lhotse.ov.otto.de/artifactory/maven-tesla-releases-local"
                              :username      :env/artifactory_user
                              :password      :env/artifactory_password
                              :sign-releases false}]
                 ["snapshots" {:url           "http://artifactory.lhotse.ov.otto.de/artifactory/maven-tesla-snapshots-local"
                               :username      :env/artifactory_user
                               :password      :env/artifactory_password
                               :sign-releases false}]]
  :description "Component for exposing metrics to graphtie as well as "
  :url "https://www.otto.de/suche/tesla/"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [com.stuartsierra/component "0.3.2"]
                 [metrics-clojure "2.8.0"]
                 [metrics-clojure-graphite "2.8.0"]]
  :profiles {:dev {:plugins [[lein-release/lein-release "1.0.9"]]}}
  )
