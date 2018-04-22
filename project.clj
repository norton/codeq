(defproject datomic/codeq "0.1.0-SNAPSHOT"
  :description "codeq does a code-aware import of your git repo into a Datomic db"
  :url "http://datomic.com"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main datomic.codeq.core
  :plugins [[lein-tar "1.1.0"]]
  :dependencies [[com.datomic/datomic-free "0.9.5697"]
                 [commons-codec "1.11"]
                 [org.clojure/clojure "1.9.0"]]
  :source-paths ["src" "examples/src"]
  :profiles {:uberjar {:aot :all}})
