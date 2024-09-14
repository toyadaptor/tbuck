(ns tbuck.clj.core
  (:refer-clojure :exclude [update])
  (:require
    [clojure.java.jdbc :as j]
    [environ.core :refer [env]]
    ;[honeysql.core :as sql]
    [honey.sql :as sql]
    ;[honeysql.helpers :refer :all :as helpers]
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
                                    (sql/format
                                      {:update :tong
                                       :set    {:amount {:select [[(sql/call :coalesce (sql/call :sum :amount) 0)]]
                                                         :from   :inout
                                                         :where  [:= :tid tid]}}
                                       :where  [:= :tid tid]}))))





(defn bucket-sum [bid]
  (println "# bucket-sum. bid: " bid)
  (j/with-db-connection [tx dbspec]
                        (j/execute! tx
                                    (sql/format
                                      {:update :bucket
                                       :set    {:amount {:select :%sum.amount
                                                         :from   :divide
                                                         :where  [:= :bid bid]}}
                                       :where  [:= :bid bid]}))))


(defn tong-get [tid]
  (first
    (j/query dbspec
             (sql/format
               {:select [:tid :amount :tong_name]
                :from   :tong
                :where  [:= :tid tid]}))))



(defn tong-list []
  (j/query dbspec
           (sql/format
             {:select   [:tid :amount :tong_name]
              :from     [:tong]
              :order-by [[:tid :asc]]})))


(defn bucket-get [bid]
  (first
    (j/query dbspec
             (sql/format
               {:select [:bid :amount :bucket_name :tid :comment]
                :from   [:bucket]
                :where  [:= :bid bid]}))))

(defn bucket-list
  ([]
   (j/query dbspec
            (sql/format
              {:select   [:bid :amount :bucket_name :tid]
               :from     [:bucket]
               :order-by [[:tid :asc] [:bucket_name :asc]]})))


  ([tid]
   (j/query dbspec
            (sql/format
              {:select   [:bid :amount :bucket_name :tid]
               :from     [:bucket]
               :where    [:= :tid tid]
               :order-by [[:bid :asc]]}))))


(defn bucket-divide-list [bid]
  (j/query dbspec
           (sql/format
             {:select   [:dno :comment :amount :create_date :base_date :ono]
              :from     [:divide]
              :where    [:= :bid bid]
              :order-by [[:dno :desc]]})))


(defn inout-list [tid]
  (j/query dbspec
           (sql/format
             {:select   [:ono :base_date :amount :is_divide :comment]
              :from     [:inout]
              :where    [:= :tid tid]
              :order-by [[:ono :desc]]})))




;(defn check []
;  (println "### tong, bucket sum")
;  {:tong-bucket-sum   (let [rows (j/query dbspec
;                                          (-> (select :x.tid :x.tong_amount :x.bucket_sum
;                                                      [(sql/call := :x.tong_amount
;                                                                 :x.bucket_sum) :is_ok])
;
;                                              (from
;                                                [(-> (select :t.tid
;                                                             [:t.amount :tong_amount]
;                                                             [(sql/call :coalesce
;                                                                        (-> (select (sql/call :sum :b.amount))
;                                                                            (from [:bucket :b])
;                                                                            (where [:= :b.tid :t.tid]))
;                                                                        0) :bucket_sum])
;
;                                                     (from [:tong :t])) :x])
;                                              (order-by [:x.tid])
;                                              (sql/format)))]
;
;                        (filter #(not (:is_ok %)) rows))
;   :tong-inout-sum    (let [rows (j/query dbspec
;                                          (-> (select :x.tid :x.tong_amount :x.inout_sum
;                                                      [(sql/call := :x.tong_amount
;                                                                 :x.inout_sum) :is_ok])
;
;                                              (from
;                                                [(-> (select :t.tid
;                                                             [:t.amount :tong_amount]
;                                                             [(sql/call :coalesce
;                                                                        (-> (select (sql/call :sum :o.amount))
;                                                                            (from [:inout :o])
;                                                                            (where [:= :o.tid :t.tid]))
;                                                                        0) :inout_sum])
;
;                                                     (from [:tong :t])) :x])
;
;                                              (order-by [:x.tid])
;                                              (sql/format)))]
;                        (filter #(not (:is_ok %)) rows))
;   :bucket-divide-sum (let [rows (j/query dbspec
;                                          (-> (select :x.bid :x.bucket_amount :x.divide_sum
;                                                      [(sql/call := :x.bucket_amount
;                                                                 :x.divide_sum) :is_ok])
;
;                                              (from
;                                                [(-> (select :b.bid
;                                                             [:b.amount :bucket_amount]
;                                                             [(sql/call :coalesce
;                                                                        (-> (select (sql/call :sum :d.amount))
;                                                                            (from [:divide :d])
;                                                                            (where [:= :d.bid :b.bid]))
;                                                                        0) :divide_sum])
;
;                                                     (from [:bucket :b])) :x])
;
;                                              (order-by [:x.bid])
;                                              (sql/format)))]
;
;                        (filter #(not (:is_ok %)) rows))
;   :inout-divide-sum  (let [rows (j/query dbspec
;                                          (-> (select :x.tid :x.ono :x.inout_amount :x.is_divide :x.divide_sum
;                                                      [(sql/call := :x.inout_amount
;                                                                 :x.divide_sum) :is_ok])
;
;                                              (from
;                                                [(-> (select :o.tid :o.ono :o.is_divide
;                                                             [:o.amount :inout_amount]
;                                                             [(sql/call :coalesce
;                                                                        (-> (select (sql/call :sum :d.amount))
;                                                                            (from [:divide :d])
;                                                                            (where [:= :d.ono :o.ono]))
;                                                                        0) :divide_sum])
;
;                                                     (from [:inout :o])
;                                                     (order-by [:o.tid :o.ono])) :x])
;                                              (order-by [:x.tid])
;                                              (sql/format)))]
;                        (filter #(not (:is_ok %)) rows))})


(defn check []
  (println "### tong, bucket sum")
  {:tong-bucket-sum
   (let [rows (j/query dbspec
                       (sql/format
                         {:select   [:x.tid :x.tong_amount :x.bucket_sum
                                     [(sql/call := :x.tong_amount :x.bucket_sum) :is_ok]]
                          :from     [[{:select [:t.tid
                                                [:t.amount :tong_amount]
                                                [(sql/call :coalesce
                                                           {:select [:%sum.amount]
                                                            :from   [[:bucket :b]]
                                                            :where  [:= :b.tid :t.tid]}
                                                           0) :bucket_sum]]
                                       :from   [[:tong :t]]} :x]]
                          :order-by [[:x.tid :asc]]}))]
     (filter #(not (:is_ok %)) rows))

   :tong-inout-sum                                          ; TODO
   (let [rows (j/query dbspec
                       (sql/format
                         {:select   [:x.tid :x.tong_amount :x.inout_sum
                                     [[:= :x.tong_amount :x.inout_sum] :is_ok]]
                          :from     [[{:select [:t.tid
                                                [:t.amount :tong_amount]
                                                [(sql/call :coalesce
                                                           {:select [:%sum.amount]
                                                            :from   [[:inout :o]]
                                                            :where  [:= :o.tid :t.tid]}
                                                           0) :inout_sum]]
                                       :from   [[:tong :t]]} :x]]
                          :order-by [[:x.tid :asc]]}))]
     (filter #(not (:is_ok %)) rows))


   :bucket-divide-sum                                       ; TODO
   (let [rows (j/query dbspec
                       (sql/format
                         {:select   [:x.bid :x.bucket_amount :x.divide_sum
                                     [(sql/call := :x.bucket_amount :x.divide_sum) :is_ok]]
                          :from     [[{:select [:b.bid
                                                [:b.amount :bucket_amount]
                                                [(sql/call :coalesce
                                                           {:select [:%sum.amount]
                                                            :from   [[:divide :d]]
                                                            :where  [:= :d.bid :b.bid]}
                                                           0) :divide_sum]]
                                       :from   [[:bucket :b]]} :x]]
                          :order-by [[:x.bid :asc]]}))]
     (filter #(not (:is_ok %)) rows))

   :inout-divide-sum
   (let [rows (j/query dbspec
                       (sql/format
                         {:select   [:x.tid :x.ono :x.inout_amount :x.is_divide :x.divide_sum
                                     [(sql/call := :x.inout_amount :x.divide_sum) :is_ok]]
                          :from     [[{:select   [:o.tid :o.ono :o.is_divide
                                                  [:o.amount :inout_amount]
                                                  [(sql/call :coalesce
                                                             {:select [:%sum.amount]
                                                              :from   [[:divide :d]]
                                                              :where  [:= :d.ono :o.ono]}
                                                             0) :divide_sum]]
                                       :from     [[:inout :o]]
                                       :order-by [[:o.tid :asc] [:o.ono :asc]]} :x]]
                          :order-by [[:x.tid :asc]]}))]
     (filter #(not (:is_ok %)) rows))})



;(defn terminal-divide-new [ono]
;  (if-let [row (first (j/query dbspec
;                               (-> (select :*)
;                                   (from :inout)
;                                   (where [:and [:= :ono ono] [:= :is_divide false]])
;                                   (sql/format))))]
;    (let [arr []]
;      (println "### divide-new")
;      (println "ono : " ono)
;      (println row)
;      (loop [sum (:amount row)
;             cnt 0]
;        (println "remain [" sum "]")
;        (when (or (not= sum 0) (= cnt 0))
;          (println "amount / bid / comment")
;          (print "===>")
;          (let [sep (str/split (read-line) #"/")
;                amount (edn/read-string (nth sep 0))
;                bid (nth sep 1)
;                comment (nth sep 2)]
;            (println "input : " amount bid comment)
;            (conj arr [amount bid comment])
;
;            (j/with-db-connection [tx dbspec]
;                                  (j/execute! tx
;                                              (-> (insert-into :divide)
;                                                  (columns :ono :amount :bid :comment :etc :base_date)
;                                                  (values [[ono amount bid (:comment row) "etc" (:base_date row)]]) sql/format))
;                                  (j/execute! tx
;                                              (-> (helpers/update :inout)
;                                                  (sset {:is_divide true})
;                                                  (where [:= :ono ono]) (sql/format)))
;                                  (bucket-sum bid))
;
;            (recur (- sum amount) (inc cnt))))))))

(defn terminal-divide-new [ono]
  (if-let [row (first (j/query dbspec
                               (sql/format
                                 {:select [:*]
                                  :from   [:inout]
                                  :where  [:and [:= :ono ono] [:= :is_divide false]]})))]
    (let [arr []]
      (println "### divide-new")
      (println "ono : " ono)
      (println row)
      (loop [sum (:amount row)
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
                                              (sql/format
                                                {:insert-into :divide
                                                 :columns [:ono :amount :bid :comment :etc :base_date]
                                                 :values [[ono amount bid (:comment row) "etc" (:base_date row)]]}))
                                  (j/execute! tx
                                              (sql/format
                                                {:update :inout
                                                 :set {:is_divide true}
                                                 :where [:= :ono ono]}))
                                  (bucket-sum bid))

            (recur (- sum amount) (inc cnt))))))))

(defn divide-new [tid ono list]
  (j/with-db-connection [tx dbspec]
                        (j/execute! tx
                                    (sql/format
                                      {:insert-into :divide
                                       :values list}))
                        (j/execute! tx
                                    (sql/format
                                      {:update :inout
                                       :set {:is_divide true}
                                       :where [:= :ono ono]})))
  (doseq [item list]
    (bucket-sum (:bid item)))
  (inout-sum tid))

(defn divide-info-ono-after [ono]
  (j/query dbspec
           (sql/format
             {:select    [:d.* :b.bucket_name]
              :from      [[:divide :d]]
              :left-join [[:bucket :b] [:= :b.bid :d.bid]]
              :where     [:= :d.ono ono]})))



(defn divide-info-ono [ono]
  (first
    (j/query dbspec
             (sql/format
               {:select [:*]
                :from   [:inout]
                :where  [:= :ono ono]}))))


(defn divide-info-dno [dno]
  (first
    (j/query dbspec
             (sql/format
               {:select [:dno :ono :bid :amount :create_date :comment]
                :from :divide
                :where [:= :dno dno]}))))



;(defn divide-remove [ono]
;  (divide-info-ono ono)
;  (j/with-db-connection [tx dbspec]
;                        (j/execute! tx (-> (delete-from :divide)
;                                           (where [:= :ono ono])
;                                           (sql/format)))
;                        (j/execute! tx (-> (helpers/update :inout)
;                                           (sset {:is_divide false})
;                                           (where [:= :ono ono]) (sql/format)))))
(defn divide-remove [ono]
  (divide-info-ono ono)
  (j/with-db-connection [tx dbspec]
                        (j/execute! tx
                                    (sql/format
                                      {:delete-from :divide
                                       :where       [:= :ono ono]}))


                        (j/execute! tx
                                    (sql/format
                                      {:update :inout
                                       :set    {:is_divide false}
                                       :where  [:= :ono ono]}))))



(defn tong-get [tid]
  (first
    (j/query dbspec
             (sql/format
               {:select :*
                :from   :tong
                :where  [:= :tid tid]}))))




;(defn inout-new [tid amount comment base_date]
;  (j/execute! dbspec (-> (insert-into :inout)
;                         (columns :amount :comment :base_date :is_divide :tid)
;                         (values [[amount comment base_date false tid]]) sql/format))
;  (println "added.")
;  (inout-sum tid))

(defn inout-new [tid amount comment base_date]
  (j/execute! dbspec
              (sql/format
                {:insert-into :inout
                 :columns     [:amount :comment :base_date :is_divide :tid]
                 :values      [[amount comment base_date false tid]]}))
  (println "added.")
  (inout-sum tid))


(defn inout-get [ono]
  (first
    (j/query dbspec
             (sql/format
               {:select :*
                :from   [:inout]
                :where  [:= :ono ono]}))))


(defn inout-remove [ono]
  (if-let [inout (first (j/query dbspec
                                 (sql/format
                                   {:select [:*]
                                    :from   [:inout]
                                    :where  [:= :ono ono]})))]
    (j/with-db-connection [conn dbspec]
                          (j/execute! conn
                                      (sql/format
                                        {:delete-from :divide
                                         :where       [:= :ono ono]}))


                          (j/execute! conn
                                      (sql/format
                                        {:delete-from :inout
                                         :where       [:= :ono ono]}))
                          (println "deleted.")
                          (inout-sum (:tid inout)))
    (println "no ono")))






