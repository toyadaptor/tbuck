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

(defn tong-inouts [tid]
  (println "tong-inouts: " tid)
  (let [inouts (->> (inout-list tid)
                    (map #(clojure.set/rename-keys % {:base_date :base-date})))]
    {:inouts inouts}))


(defn bucket-divides [bid]
  (println "bucket-divides : " bid)
  (let [bucket (clojure.set/rename-keys (bucket-get bid) {:bucket_name :bucket-name})
        divides (->> (bucket-divide-list bid)
                     (map #(clojure.set/rename-keys % {:base_date :base-date})))]
    (println divides)
    {:bucket bucket
     :divides divides}))


(defn tong-inouts-detail [ono]
  {:inout (clojure.set/rename-keys (divide-info-ono ono) {:base_date :base-date})
   :divides (->> (divide-info-ono-after ono)
                 (map #(clojure.set/rename-keys % {:bucket_name :bucket-name})))})

(defn bucket-divides-detail [dno]
  (let [divide (divide-info-dno dno)]
    (tong-inouts-detail (:ono divide))))

















