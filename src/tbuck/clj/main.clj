(ns tbuck.clj.main
  (:refer-clojure :exclude [update])
  (:require
    [tbuck.clj.core :refer :all]
    [clojure.java.jdbc :as j]
    [environ.core :refer [env]]
    [table.core :as t]
    [honeysql.core :as sql]
    [honeysql.helpers :refer :all :as helpers]
    [clojure.string :as str]
    [clojure.edn :as edn])
  (:gen-class))

(defn top-menu []
  (t/table [{:no "t" :menu "tong list"}
            {:no "t [tid]" :menu "tong inout list"}
            {:no "" :menu ""}
            {:no "b" :menu "buckets"}
            {:no "b [bid]" :menu "bucket divide list"}
            {:no "" :menu ""}
            {:no "tn [tid]" :menu "inout new"}
            {:no "dn [ono]" :menu "divide(inout-bucket) new"}
            {:no "" :menu ""}
            {:no "to [ono]" :menu "divide(inout-bucket) info(ono)"}
            {:no "do [dno]" :menu "divide(inout-bucket) info(dno)"}
            {:no "" :menu ""}
            {:no "tr [ono]" :menu "inout remove"}
            {:no "dr [ono]" :menu "divide(inout-bucket) remove"}
            {:no "" :menu ""}
            {:no "c" :menu "check"}
            {:no "" :menu ""}
            {:no "q" :menu "quit"}]))


(defn -main
  [& args]
  (println "t-buck!")
  (loop [some ""]
    (let [sep (str/split some #" ") cmd (first sep) con (second sep)]
      (cond
        (= "" cmd) (do (print (str (char 27) "[2J")) (top-menu))
        (and (= "q" cmd) (nil? con)) (System/exit 0)
        (and (= "t" cmd) (nil? con)) (t/table (tong-list))
        (and (= "t" cmd) (some? con)) (t/table (inout-list con))
        (and (= "b" cmd) (nil? con)) (t/table (bucket-list))
        (and (= "b" cmd) (some? con)) (t/table (bucket-divide-list con))
        (and (= "tn" cmd) (some? con)) (let [tid con
                                             row (j/query dbspec
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
                                               (inout-new tid amount comment base_date)))))



        (and (= "tr" cmd) (some? con)) (inout-remove (edn/read-string con))
        (and (= "dn" cmd) (some? con)) (divide-new (edn/read-string con))
        (and (= "to" cmd) (some? con)) (let [ono (edn/read-string con)]
                                         (println "### divide-info-ono")
                                         (println "# inout info")
                                         (t/table (divide-info-ono ono))
                                         (println "# divide list")
                                         (t/table (divide-info-ono-after ono)))
        (and (= "do" cmd) (some? con)) (let [dno (edn/read-string con)
                                             {:keys [divide-info inout-info]} (divide-info-dno dno)]
                                         (println "#divide-info-dno")
                                         (println "dno : " dno)
                                         (println "### divide info")
                                         (t/table divide-info)
                                         (t/table inout-info))
        (and (= "dr" cmd) (some? con)) (let [ono con]
                                         (println "### divide-remove")
                                         (println "ono : " ono)
                                         (divide-remove ono))

        (and (= "c" cmd) (nil? con)) (let [{:keys [tong-bucket-sum tong-inout-sum bucket-divide-sum inout-divide-sum]} (check)]
                                       (t/table tong-bucket-sum)
                                       (t/table tong-inout-sum)
                                       (t/table bucket-divide-sum)
                                       (t/table inout-divide-sum))
        :else (top-menu)))


    (println)
    (print ">> ")
    (flush)
    (recur (read-line))))

(comment
  (t/table [{:hehe 123 :nana 456} {:hehe :11 :nana 44}])
  (t/table nil))


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
