(defproject emotions "0.1.1"
  :description "Clojure implementation of an emotional model for autonomous agents"
  :url "https://github.com/davesnowdon/emotions-clojure"
  :license {:name "LGPL v2.1"
            :url "http://www.gnu.org/licenses/lgpl-2.1.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [expectations "1.4.56"]]
  :profile {:dev {:dependencies [[expectations "1.4.56"]]}}
  :plugins [[lein-autoexpect "1.0"]
            [lein-ancient "0.5.4"]])
