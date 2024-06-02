(defproject t-buck "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/java.jdbc "0.7.11"]
                 [cli4clj "1.7.6"]
                 [honeysql "1.0.444"]
                 [table "0.5.0"]
                 [environ "1.2.0"]
                 [org.postgresql/postgresql "42.2.14"]]
  :plugins [[lein-environ "1.2.0"]
            [lein-pprint "1.3.2"]]
  :source-paths ["."]
  :main main)
