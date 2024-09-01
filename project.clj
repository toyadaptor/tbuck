(defproject t-buck "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.11.0"]
                 [org.clojure/java.jdbc "0.7.11"]
                 [org.clojure/data.json "2.5.0"]
                 [cli4clj "1.7.6"]
                 [com.github.seancorfield/honeysql "2.4.947"]
                 [table "0.5.0"]
                 [environ "1.2.0"]
                 [org.postgresql/postgresql "42.2.14"]
                 [ring/ring-jetty-adapter "1.9.6"]
                 ;[metosin/reitit-ring "0.7.1"]
                 [metosin/reitit "0.7.1"]
                 [buddy/buddy-auth "3.0.323"]
                 [buddy/buddy-sign "3.5.351"]
                 [ring-cors "0.1.13"]
                 [http-kit "2.6.0"]
                 [clj-time "0.15.2"]]
  :plugins [[lein-environ "1.2.0"]
            [lein-pprint "1.3.2"]]
  :source-paths ["src/main"]
  :main tbuck.clj.main)


