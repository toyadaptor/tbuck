(ns tbuck.clj.api
  (:require [tbuck.clj.core :as core]))

(defn convert-keys [m]
  (clojure.set/rename-keys m {:bucket_name :bucket-name
                              :base_date   :base-date}))


(defn main []
  (let [tid "main"
        tong (core/tong-get tid)
        buckets (->> (core/bucket-list tid)
                     (map #(convert-keys %)))
        inout (first (core/inout-list tid))]
    {:tong-name    (:tong_name tong)
     :tong-amount  (:amount tong)
     :is-valid-sum true
     :last-inout   (if (empty? inout) "no inout" (:base_date inout))
     :buckets      buckets}))


(defn tong-inouts [tid]
  (println "tong-inouts: " tid)
  (let [inouts (->> (core/inout-list tid)
                    (map #(convert-keys %)))]
    {:inouts inouts}))

(defn tong-inout-new [tid amount base-date comment]
  (try
    (core/inout-new tid amount comment base-date)
    {:status 200}
    (catch Exception e
      {:status 500 :body {:error-text (.getMessage e)}})))





(defn bucket-list [tid]
  (try
    (let [buckets (->> (core/bucket-list tid)
                       (map #(convert-keys %)))]
      {:status 200
       :body   {:buckets buckets}})
    (catch Exception e
      {:status 500
       :body   {:error-text (.getMessage e)}})))

(defn bucket-divides [bid]
  (println "bucket-divides : " bid)
  (let [bucket (convert-keys (core/bucket-get bid))
        divides (->> (core/bucket-divide-list bid)
                     (map #(convert-keys %)))]
    (println divides)
    {:bucket  bucket
     :divides divides}))


(defn tong-inouts-detail [ono]
  {:inout   (convert-keys (core/divide-info-ono ono))
   :divides (->> (core/divide-info-ono-after ono)
                 (map #(convert-keys %)))})

(defn tong-inouts-removing [ono]
  (core/inout-remove ono))

(defn bucket-divides-detail [dno]
  (let [divide (core/divide-info-dno dno)]
    (tong-inouts-detail (:ono divide))))

(defn inout-info-for-divide-new [ono]
  (try
    (let [buckets (into {} (map (fn [[k v]] {k (first v)})
                                (group-by :bid (->> (core/bucket-list "main")
                                                    (map #(convert-keys %))))))
          inout (convert-keys (core/divide-info-ono ono))]
      {:status 200
       :body   {:inout   inout
                :buckets buckets}})
    (catch Exception e
      {:status 500}
      :body {:error-text (.getMessage e)})))



