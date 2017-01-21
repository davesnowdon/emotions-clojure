(defproject emotions "0.2.2"
  :description "Clojure implementation of an emotional model for autonomous agents"
  :url "https://github.com/davesnowdon/emotions-clojure"
  :license {:name "LGPL v2.1"
            :url "http://www.gnu.org/licenses/lgpl-2.1.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clj-time "0.13.0"]
                 [expectations "2.1.9"]]
  :profile {:dev {:dependencies [[expectations "2.1.9"]]}}
  :plugins [[lein-autoexpect "1.9.0"]
            [lein-ancient "0.6.10"]])
