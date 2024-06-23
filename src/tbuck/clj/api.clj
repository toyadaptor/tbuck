(ns tbuck.clj.api
  (:require [tbuck.clj.core :refer :all]))

(defn main []
  (let [tid "main"
        tong (tong-get "main")
        buckets (->> (bucket-list tid)
                     (map #(clojure.set/rename-keys % {:bucket_name :bucket-name})))]
    {:tong-name    (:tong_name tong)
     :tong-amount  (:amount tong)
     :is-valid-sum true
     :last-inout   "2024.06.23. 12:34"
     :buckets      buckets}))

(defn bucket-divides [bid]
  (println "bucket-divides : " bid)
  {:divides [{:dno     123
              :ono     456
              :bid     "travel"
              :amount  123
              :comment "정기적."}]})

(defn tong-inouts [tid]
  (println "tong-inouts: " tid)
  {:inouts [{:ono    77
             :amount 88888}]})







