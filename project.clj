(defproject de.otto.tesla/goo "0.2.0-SNAPSHOT"
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
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [iapetos "0.1.6"]
                 [org.clojure/tools.logging "0.4.0"]]
  :test-paths ["test" "test-resources"]
  :profiles {:dev {:jvm-opts     ["-Dlog_appender=consoleAppender"
                                  "-Dlog_level=INFO"]
                   :plugins [[lein-release/lein-release "1.0.9"]]}})
