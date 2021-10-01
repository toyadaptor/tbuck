(ns main
  (:refer-clojure :exclude [update])
  (:require
   (cli4clj [cli :as cli])
   (clj-assorted-utils [util :as utils])
   [clojure.java.jdbc :as j]
   [table.core :as t]
   [honeysql.core :as sql]
   [honeysql.helpers :refer :all :as helpers]
   [clojure.string :as str]
   [clojure.edn :as edn])
  (:gen-class))

(def dbspec {:dbtype "postgresql"
            :dbname "tbuck"
            :host "sy.monologue.me"
            :port "65432"
            :user "postgres" 
            :password "red38;" 
            :auto-commit true})

(defn inout-sum [tid]
  (println "# inout-sum. tid : " tid)
  (j/with-db-connection [tx dbspec]
    (j/execute! tx
                (-> (helpers/update :tong)
                    (sset {:amount (-> (select (sql/call :sum :amount))
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
                                       (where [:= :bid bid])
                                       )})
                    (where [:= :bid bid])
                    (sql/format))))

;  (let [rows (j/query dbspec (->
;                              (select :bid :tid)
;                              (from :bucket)
;                              (where [:= :bid bid])
;                              sql/format))
;        tid (:tid (first rows))]
;    (inout-sum tid))
  )



(defn tong-list []
  (let [rows (j/query dbspec (->
                                (select :tid :amount :tong_name)
                                (from :tong)
                                (order-by [:tid :bid])
                                sql/format))]
    (t/table rows)))

(defn bucket-list []
  (let [rows (j/query dbspec (->
                                (select :bid :amount :bucket_name :tid)
                                (from :bucket)
                                (order-by [:tid :bid])
                                sql/format))]
    (t/table rows)))

(defn bucket-divide-list [bid]
  (let [rows (j/query dbspec
                         (->
                          (select :dno :comment :amount :create_date)
                          (from :divide)
                          (where [:= :bid bid])
                          (order-by [:dno])
                          sql/format))]
    (t/table rows)))


(defn inout-list [tid]
  (let [rows (j/query dbspec
                        (-> (select :ono :base_date :amount :is_divide :comment)
                            (from :inout)
                            (where [:= :tid tid])
                            (order-by [:ono])
                            sql/format))]
    (t/table rows)))

(defn check []
  (println "### tong, bucket sum")
  (let [rows (j/query dbspec
                         (-> (select :x.tid :x.tong_amount :x.bucket_sum
                                     [(sql/call := :x.tong_amount
                                               :x.bucket_sum) :is_ok]
                                     )
                             (from
                              [(-> (select :t.tid
                                           [:t.amount :tong_amount]
                                           [(sql/call :coalesce 
                                                      (-> (select (sql/call :sum :b.amount))
                                                          (from [:bucket :b])
                                                          (where [:= :b.tid :t.tid]))
                                                      0
                                             ) :bucket_sum]
                                           )
                                   (from [:tong :t])) :x])
                             (order-by [:x.tid])
                             (sql/format))
                         )]
    (t/table (filter #(not (:is_ok %)) rows)))

  (println "### tong, inout sum")
  (let [rows (j/query dbspec
                         (-> (select :x.tid :x.tong_amount :x.inout_sum
                                     [(sql/call := :x.tong_amount
                                               :x.inout_sum) :is_ok]
                                     )
                             (from
                              [(-> (select :t.tid
                                           [:t.amount :tong_amount]
                                           [(sql/call :coalesce 
                                                      (-> (select (sql/call :sum :o.amount))
                                                          (from [:inout :o])
                                                          (where [:= :o.tid :t.tid]))
                                                      0
                                             ) :inout_sum]
                                           )
                                   (from [:tong :t])) :x]
                              )
                             (order-by [:x.tid])
                             (sql/format))
                         )]
    (t/table (filter #(not (:is_ok %)) rows)))

  (println "### bucket, divide sum")
  (let [rows (j/query dbspec
                         (-> (select :x.bid :x.bucket_amount :x.divide_sum
                                     [(sql/call := :x.bucket_amount
                                               :x.divide_sum) :is_ok]
                                     )
                             (from
                              [(-> (select :b.bid
                                           [:b.amount :bucket_amount]
                                           [(sql/call :coalesce 
                                                      (-> (select (sql/call :sum :d.amount))
                                                          (from [:divide :d])
                                                          (where [:= :d.bid :b.bid]))
                                                      0
                                             ) :divide_sum]
                                           )
                                   (from [:bucket :b])) :x]
                              )
                             (order-by [:x.bid])
                             (sql/format))
                         )]
    (t/table (filter #(not (:is_ok %)) rows)))

  (println "### inout, divide sum")
  (let [rows (j/query dbspec
                         (-> (select :x.tid :x.ono :x.inout_amount :x.is_divide :x.divide_sum
                                     [(sql/call := :x.inout_amount
                                               :x.divide_sum) :is_ok]
                                     )
                             (from
                              [(-> (select :o.tid :o.ono :o.is_divide
                                           [:o.amount :inout_amount]
                                           [(sql/call :coalesce
                                                      (-> (select (sql/call :sum :d.amount))
                                                          (from [:divide :d])
                                                          (where [:= :d.ono :o.ono]))
                                                      0
                                             ) :divide_sum]
                                           )
                                   (from [:inout :o])
                                   (order-by [:o.tid :o.ono])) :x]
                              )
                             ;(where [:<> :x.inout_amount :x.divide_sum])
                             (order-by [:x.tid])
                             (sql/format))
                         )]
    (t/table (filter #(not (:is_ok %)) rows))))


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

              (recur (- sum amount) (inc cnt)))))

        ))))

(defn divide-info-ono [ono]
  (println "### divide-info-ono")
  (println "ono : " ono)

  (println "# inout info")
  (t/table (j/query dbspec
                    (-> (select :base_date :amount :is_divide :comment)
                        (from :inout)
                        (where [:= :ono ono])
                        sql/format)))

  (println "# divide list")
  (t/table (j/query dbspec
                    (-> (select :dno :bid :amount :create_date :comment)
                        (from :divide)
                        (where [:= :ono ono])
                        (order-by [:bid])
                        sql/format))))

(defn divide-info-dno [dno]
  (println "#divide-info-dno")
  (println "dno : " dno)
  (println "### divide info")
  (let [row (j/query dbspec
                     (-> (select :dno :ono :bid :amount :create_date :comment)
                         (from :divide)
                         (where [:= :dno dno])
                         sql/format))]
    (t/table row)
    (divide-info-ono (:ono (first row)))))


(defn divide-remove [ono]
  (println "### divide-remove")
  (println "ono : " ono)
  (divide-info-ono ono)
  (println "remove? (y/n)")
  (flush)
  (if (= "y" (read-line))
    (j/with-db-connection [tx dbspec]
      (j/execute! tx (-> (delete-from :divide)
                           (where [:= :ono ono])
                           (sql/format))
                  (println "removed."))
      (j/execute! tx (-> (helpers/update :inout)
                         (sset {:is_divide false})
                         (where [:= :ono ono]) (sql/format))
                  (println "inout's is_divide to false."))
      )))



(defn inout-new [tid]
  (let [row (j/query dbspec
                        (-> (select :tid)
                            (from :tong)
                            (where [:= :tid tid])
                            (sql/format)))]

    (if (not-empty row)
      (do
        (println "### inout-new")
        (println "tid : " tid)
        (println "amount / comment / basedate(YYYYMMDD)")
        (print "===>")
        (let [sep (str/split (read-line) #"/")
              amount (edn/read-string (nth sep 0))
              comment (nth sep 1)
              base_date (nth sep 2)]
          (println "input : " amount comment base_date)

          (j/execute! dbspec (-> (insert-into :inout)
                             (columns :amount :comment :base_date :is_divide :tid)
                             (values [[amount comment base_date false tid]]) sql/format))))))
  (println "added.")
  (inout-sum tid))

(defn inout-remove [ono]
  (println "### inout-remove")
  (println "ono : " ono)
  (divide-info-ono ono)
  (println "remove? (y/n)")
  (flush)
  (if (= "y" (read-line))
    (j/with-db-connection [conn dbspec]
      (j/execute! conn (-> (delete-from :divide)
                           (where [:= :ono ono])
                           (sql/format)))
      (j/execute! conn (-> (delete-from :inout)
                           (where [:= :ono ono])
                           (sql/format)))
      (println "deleted.")
      (inout-sum))))




(defn top-menu []
  (t/table [{:no "t" :menu "tong list"}
            {:no "t [tid]" :menu "tong inout list"}
            {:no "" :menu ""}
            {:no "b" :menu "buckets"}
            {:no "b [bid]" :menu "bucket divide list"}
            {:no "" :menu ""}
            {:no "tn [tid]" :menu "inout new"}
            {:no "tr [ono]" :menu "inout remove"}
            {:no "" :menu ""}
            {:no "dn [ono]" :menu "divide(inout-bucket) new"}
            {:no "to [ono]" :menu "divide(inout-bucket) info(ono)"}
            {:no "do [dno]" :menu "divide(inout-bucket) info(dno)"}
            {:no "dr [ono]" :menu "divide(inout-bucket) remove"}
            {:no "" :menu ""}
            {:no "c" :menu "check"}
            {:no "" :menu ""}
            {:no "q" :menu "quit"}
            ]))

(defn -main
  [& args]
  (println "t-buck!")
  (loop [some ""]
    (let [sep (str/split some #" ") cmd (first sep) con (second sep)]
      (cond
        (= "" cmd) (do (print (str (char 27) "[2J")) (top-menu))
        (and (= "q" cmd) (nil? con)) (System/exit 0)
        (and (= "t" cmd) (nil? con)) (tong-list)
        (and (= "t" cmd) (some? con)) (inout-list con)
        (and (= "b" cmd) (nil? con)) (bucket-list)
        (and (= "b" cmd) (some? con)) (bucket-divide-list con)
        (and (= "tn" cmd) (some? con)) (inout-new con)
        (and (= "tr" cmd) (some? con)) (inout-remove (edn/read-string con))
        (and (= "dn" cmd) (some? con)) (divide-new (edn/read-string con))
        (and (= "to" cmd) (some? con)) (divide-info-ono (edn/read-string con))
        (and (= "do" cmd) (some? con)) (divide-info-dno (edn/read-string con))
        (and (= "dr" cmd) (some? con)) (divide-remove (edn/read-string con))
        (and (= "c" cmd) (nil? con)) (check)
        :else (top-menu))
      )

    (println)
    (print ">> ")
    (flush)
    (recur (read-line))
   ))


                                        ;(defn test-rp []
                                        ;  (let [rows (j/query dbspec (->
                                        ;                                 (select :tid :bid :amount :bucket_name)
                                        ;                                 (from :bucket)
                                        ;                                 (order-by [:tid :bid])
                                        ;                                 sql/format))]
                                        ;    (t/table rows)
                                        ;    (row-print rows)
                                        ;    ))
                                        ;
                                        ;(defn row-print [rows]
                                        ;  (if (some? rows)
                                        ;    (do
                                        ;      (doseq [subj (map first (first rows))]
                                        ;        (some? (print (format "%15s" (name subj)))))
                                        ;      (println)
                                        ;      (doseq [subj (map first (first rows))]
                                        ;        (some? (print "***************")))
                                        ;      )
                                        ;    )
                                        ;
                                        ;  )
