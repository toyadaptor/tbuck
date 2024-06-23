(ns tbuck.clj.core
  (:refer-clojure :exclude [update])
  (:require
    [clojure.java.jdbc :as j]
    [environ.core :refer [env]]
    [honeysql.core :as sql]
    [honeysql.helpers :refer :all :as helpers]
    [clojure.string :as str]
    [clojure.edn :as edn]))

(def dbspec {:dbtype      "postgresql"
             :dbname      (env :db-name)
             :host        (env :db-host)
             :port        (env :db-port)
             :user        (env :db-user)
             :password    (env :db-password)
             :auto-commit true})

(defn inout-sum [tid]
  (println "# inout-sum. tid : " tid)
  (j/with-db-connection [tx dbspec]
                        (j/execute! tx
                                    (-> (helpers/update :tong)
                                        (sset {:amount (-> (select (sql/call :coalesce (sql/call :sum :amount) 0))
                                                           (from :inout)
                                                           (where [:= :tid tid]))})
                                        (where [:= :tid tid])
                                        (sql/format)))))


(defn bucket-sum [bid]
  (println "# bucket-sum. bid: " bid)
  (j/with-db-connection [tx dbspec]
                        (j/execute! tx
                                    (-> (helpers/update :bucket)
                                        (sset {:amount (-> (select [(sql/call :sum :amount) :amount_sum])
                                                           (from :divide)
                                                           (where [:= :bid bid]))})

                                        (where [:= :bid bid])
                                        (sql/format)))))

(defn tong-get [tid]
  (first (j/query dbspec (->
                           (select :tid :amount :tong_name)
                           (from :tong)
                           (where [:= :tid tid])
                           sql/format))))

(defn tong-list []
  (j/query dbspec (->
                    (select :tid :amount :tong_name)
                    (from :tong)
                    (order-by [:tid :bid])
                    sql/format)))

(defn bucket-list
  ([] (j/query dbspec (->
                        (select :bid :amount :bucket_name :tid)
                        (from :bucket)
                        (order-by [:tid :bid])
                        sql/format)))
  ([tid] (j/query dbspec (->
                           (select :bid :amount :bucket_name :tid)
                           (from :bucket)
                           (where [:= :tid tid])
                           (order-by [:tid :bid])
                           sql/format))))

(comment (bucket-list "main"))


(defn bucket-divide-list [bid]
  (j/query dbspec
           (->
             (select :dno :comment :amount :create_date)
             (from :divide)
             (where [:= :bid bid])
             (order-by [:dno])
             sql/format)))


(defn inout-list [tid]
  (j/query dbspec
           (-> (select :ono :base_date :amount :is_divide :comment)
               (from :inout)
               (where [:= :tid tid])
               (order-by [:ono])
               sql/format)))


(defn check []
  (println "### tong, bucket sum")
  {:tong-bucket-sum   (let [rows (j/query dbspec
                                          (-> (select :x.tid :x.tong_amount :x.bucket_sum
                                                      [(sql/call := :x.tong_amount
                                                                 :x.bucket_sum) :is_ok])

                                              (from
                                                [(-> (select :t.tid
                                                             [:t.amount :tong_amount]
                                                             [(sql/call :coalesce
                                                                        (-> (select (sql/call :sum :b.amount))
                                                                            (from [:bucket :b])
                                                                            (where [:= :b.tid :t.tid]))
                                                                        0) :bucket_sum])

                                                     (from [:tong :t])) :x])
                                              (order-by [:x.tid])
                                              (sql/format)))]

                        (filter #(not (:is_ok %)) rows))
   :tong-inout-sum    (let [rows (j/query dbspec
                                          (-> (select :x.tid :x.tong_amount :x.inout_sum
                                                      [(sql/call := :x.tong_amount
                                                                 :x.inout_sum) :is_ok])

                                              (from
                                                [(-> (select :t.tid
                                                             [:t.amount :tong_amount]
                                                             [(sql/call :coalesce
                                                                        (-> (select (sql/call :sum :o.amount))
                                                                            (from [:inout :o])
                                                                            (where [:= :o.tid :t.tid]))
                                                                        0) :inout_sum])

                                                     (from [:tong :t])) :x])

                                              (order-by [:x.tid])
                                              (sql/format)))]
                        (filter #(not (:is_ok %)) rows))
   :bucket-divide-sum (let [rows (j/query dbspec
                                          (-> (select :x.bid :x.bucket_amount :x.divide_sum
                                                      [(sql/call := :x.bucket_amount
                                                                 :x.divide_sum) :is_ok])

                                              (from
                                                [(-> (select :b.bid
                                                             [:b.amount :bucket_amount]
                                                             [(sql/call :coalesce
                                                                        (-> (select (sql/call :sum :d.amount))
                                                                            (from [:divide :d])
                                                                            (where [:= :d.bid :b.bid]))
                                                                        0) :divide_sum])

                                                     (from [:bucket :b])) :x])

                                              (order-by [:x.bid])
                                              (sql/format)))]

                        (filter #(not (:is_ok %)) rows))
   :inout-divide-sum  (let [rows (j/query dbspec
                                          (-> (select :x.tid :x.ono :x.inout_amount :x.is_divide :x.divide_sum
                                                      [(sql/call := :x.inout_amount
                                                                 :x.divide_sum) :is_ok])

                                              (from
                                                [(-> (select :o.tid :o.ono :o.is_divide
                                                             [:o.amount :inout_amount]
                                                             [(sql/call :coalesce
                                                                        (-> (select (sql/call :sum :d.amount))
                                                                            (from [:divide :d])
                                                                            (where [:= :d.ono :o.ono]))
                                                                        0) :divide_sum])

                                                     (from [:inout :o])
                                                     (order-by [:o.tid :o.ono])) :x])
                                              (order-by [:x.tid])
                                              (sql/format)))]
                        (filter #(not (:is_ok %)) rows))})




(defn divide-new [ono]
  (let [row (j/query dbspec
                     (-> (select :ono :amount)
                         (from :inout)
                         (where [:and [:= :ono ono] [:= :is_divide false]])
                         (sql/format)))]
    (if (not (empty? row))
      ;(println (edn/read-string (:amount (first row))))
      (let [arr []]
        (println "### divide-new")
        (println "ono : " ono)
        (loop [sum (:amount (first row))
               cnt 0]
          (println "remain [" sum "]")
          (when (or (not= sum 0) (= cnt 0))
            (println "amount / bid / comment")
            (print "===>")
            (let [sep (str/split (read-line) #"/")
                  amount (edn/read-string (nth sep 0))
                  bid (nth sep 1)
                  comment (nth sep 2)]
              (println "input : " amount bid comment)
              (conj arr [amount bid comment])

              (j/with-db-connection [tx dbspec]
                                    (j/execute! tx
                                                (-> (insert-into :divide)
                                                    (columns :ono :amount :bid :comment :etc)
                                                    (values [[ono amount bid comment "etc"]]) sql/format))
                                    (j/execute! tx
                                                (-> (helpers/update :inout)
                                                    (sset {:is_divide true})
                                                    (where [:= :ono ono]) (sql/format)))
                                    (bucket-sum bid))

              (recur (- sum amount) (inc cnt)))))))))


(defn divide-info-ono-after [ono]

  (j/query dbspec
           (-> (select :dno :bid :amount :create_date :comment)
               (from :divide)
               (where [:= :ono ono])
               (order-by [:bid])
               sql/format)))


(defn divide-info-ono [ono]
  (j/query dbspec
           (-> (select :base_date :amount :is_divide :comment)
               (from :inout)
               (where [:= :ono ono])
               sql/format)))





(defn divide-info-dno [dno]
  (let [row (j/query dbspec
                     (-> (select :dno :ono :bid :amount :create_date :comment)
                         (from :divide)
                         (where [:= :dno dno])
                         sql/format))]
    {:divide-info row
     :inout-info  (divide-info-ono (:ono (first row)))}))



(defn divide-remove [ono]
  (divide-info-ono ono)
  (j/with-db-connection [tx dbspec]
                        (j/execute! tx (-> (delete-from :divide)
                                           (where [:= :ono ono])
                                           (sql/format)))
                        (j/execute! tx (-> (helpers/update :inout)
                                           (sset {:is_divide false})
                                           (where [:= :ono ono]) (sql/format)))))



(defn inout-get [tid]
  (first (j/query dbspec
                  (-> (select :tid)
                      (from :tong)
                      (where [:= :tid tid])
                      (sql/format)))))

(comment (inout-get "main"))

(defn inout-new [tid amount comment base_date]
  (j/execute! dbspec (-> (insert-into :inout)
                         (columns :amount :comment :base_date :is_divide :tid)
                         (values [[amount comment base_date false tid]]) sql/format))
  (println "added.")
  (inout-sum tid))


(defn inout-remove [ono]
  (if-let [inout (first (j/query dbspec
                                 (-> (select :*)
                                     (from :inout)
                                     (where [:= :ono ono])
                                     sql/format)))]
    (j/with-db-connection [conn dbspec]
                          (j/execute! conn (-> (delete-from :divide)
                                               (where [:= :ono ono])
                                               (sql/format)))
                          (j/execute! conn (-> (delete-from :inout)
                                               (where [:= :ono ono])
                                               (sql/format)))
                          (println "deleted.")
                          (inout-sum (:tid inout)))
    (println "no ono")))


