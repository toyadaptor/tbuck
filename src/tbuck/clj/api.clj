(ns tbuck.clj.api
  (:require [tbuck.clj.core :refer :all]))

(defn convert-keys [m]
  (clojure.set/rename-keys m {:bucket_name :bucket-name
                              :base_date :base-date}))


(defn main []
  (let [tid "main"
        tong (tong-get tid)
        buckets (->> (bucket-list tid)
                     (map #(convert-keys %)))
        inout (first (inout-list tid))]
    {:tong-name    (:tong_name tong)
     :tong-amount  (:amount tong)
     :is-valid-sum true
     :last-inout   (if (empty? inout) "no inout" (:base_date inout))
     :buckets      buckets}))


(defn tong-inouts [tidconkkkk]
  (println "tong-inouts: " tid)
  (let [inouts (->> (inout-list tid)
                    (map #(convert-keys %)))]
    {:inouts inouts}))


(defn bucket-divides [bid]
  (println "bucket-divides : " bid)
  (let [bucket (convert-keys (bucket-get bid))
        divides (->> (bucket-divide-list bid)
                     (map #(convert-keys %)))]
    (println divides)
    {:bucket bucket
     :divides divides}))


(defn tong-inouts-detail [ono]
  {:inout   (convert-keys (divide-info-ono ono))
   :divides (->> (divide-info-ono-after ono)
                 (map #(convert-keys %)))})

(defn bucket-divides-detail [dno]
  (let [divide (divide-info-dno dno)]
    (tong-inouts-detail (:ono divide))))

















