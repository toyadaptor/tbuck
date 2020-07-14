(defproject t-buck "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/java.jdbc "0.7.11"]
                 [cli4clj "1.7.6"]
                 [honeysql "1.0.444"]
                 [table "0.5.0"]
                ; [org.postgresql/postgresql "9.1-901-1.jdbc4"]

                 [org.postgresql/postgresql "42.2.14"]]
  :source-paths ["."]
  :main main)
