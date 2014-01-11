(defproject emotions "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [expectations "1.4.52"]]
  :profile {:dev {:dependencies [[expectations "1.4.52"]]}}
  :plugins [[lein-autoexpect "1.0"]
            [lein-ancient "0.5.4"]])
