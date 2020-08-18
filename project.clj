(defproject fr24-logger "0.1.0-SNAPSHOT"
  :description "Logger saving live aircraft track data to files."
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/data.json "0.2.6"]
                 [clojure.java-time "0.3.2"]
                 [clj-http "3.10.0"]
                 [me.raynes/fs "1.4.6"]
                 [overtone/at-at "1.2.0"]]
  :main ^:skip-aot fr24-logger.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
