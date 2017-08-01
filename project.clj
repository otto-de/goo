(defproject de.otto/goo "0.2.4"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :url "https://github.com/otto-de/goo"
  :scm {:name "git"
                :url  "https://github.com/otto-de/goo"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [iapetos "0.1.6"]
                 [org.clojure/tools.logging "0.4.0"]]
  :test-paths ["test" "test-resources"]
  :lein-release {:deploy-via :clojars}
  :profiles {:dev {:jvm-opts     ["-Dlog_appender=consoleAppender"
                                  "-Dlog_level=INFO"]
                   :plugins [[lein-release/lein-release "1.0.9"]]}})
