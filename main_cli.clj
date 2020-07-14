(ns main
  (:require
   (cli4clj [cli :as cli])
   (clj-assorted-utils [util :as utils])
   [clojure.java.jdbc :as jdbc]
   [table.core :as t]
   [honeysql.core :as sql]
   [honeysql.helpers :refer :all :as helpers])
  (:gen-class))

(def dbspec {:dbtype "postgresql"
            :dbname "tbuck"
            :host "localhost"
            :user "snailoff"
            :password ""})

(defn tong-list []
  (let [row (jdbc/query dbspec (->
                                (select :tid :amount :tong_name)
                                (from :tong)
                                sql/format))]
    (t/table row)
    ))

(defn bucket-list []
  (let [row (jdbc/query dbspec (->
                                (select :bid :amount :bucket_name)
                                (from :bucket)
                                sql/format))]
    (t/table row)
  ))

(defn inout-list []
  (let [row (jdbc/query dbspec
                        (->
                                (select :id :base_date :amount :is_divide :comment)
                                (from :inout)
                                (order-by [:base_date])
                                sql/format))]
    (t/table row)
    ))

(defn inout-detail [oid]
  (println "### inout info")
  (t/table (jdbc/query dbspec
                       (-> (select :base_date :amount :is_divide :comment)
                           (from :inout)
                           (where [:= :id oid])
                           sql/format)))

  (println "### devide list")
  (t/table (jdbc/query dbspec
                       (-> (select :id :bid :amount :create_date :comment)
                           (from :divide)
                           (where [:= :oid oid])
                           (order-by [:bid])
                           sql/format))))

(defn bucket-divide-list [bid]
  (let [row (jdbc/query dbspec
                        (->
                         (select :id : :comment :amount :create_date)
                         (from [:divide])
                         (where [:= :bid bid])
                         (order-by [:create_date])
                         sql/format))]
    (t/table row)
    ))

(defn -main
  [& args]
  (println "t-buck!")

  (cli/start-cli {:cmds {:bk {:fn #(println (bucket-list))
                              :short-info "bucket list"}
                         :bkd {:fn bucket-divide-list
                              :short-info "divide list"}
                         :tl {:fn #(println (bucket-list))
                              :short-info "bucket list"}
                         :io {:fn #(println (inout-list))
                              :short-info "inout list"}
                         :iod {:fn inout-detail
                              :short-info "inout detail - divide info"}
                         }
                  :allow-eval true
                  :prompt-string "tbuck# "
                  :alternate-scrolling true
                  :alternate-height 5

                  }))

