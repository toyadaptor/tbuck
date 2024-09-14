(ns tbuck.clj.api
  (:require [tbuck.clj.core :as core]
            [tbuck.clj.auth :as auth]
            [clj-time.core :as time]
            [buddy.sign.jwt :as jwt]))

(defn convert-keys [m]
  (clojure.set/rename-keys m {:bucket_name :bucket-name
                              :base_date   :base-date}))


(defn login [username password]
  (let [valid? (some-> auth/auth-data
                       (get (keyword username))
                       (= password))]
    (if valid?
      (let [claims {:user (keyword username)
                    :exp  (time/plus (time/now) (time/seconds 3600))}
            token (jwt/sign claims auth/secret {:alg :hs512})]
        {:status  200 :body {:token token}
         :cookies {"token" {:value     token
                            :http-only true
                            :secure    false
                            :path      "/"}}})

      {:status 400 :body {:error-text "nop!"}})))

(defn main [tid]
  (let [tong (core/tong-get tid)
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
      {:status 500
       :body   {:error-text (.getMessage e)}})))



(defn divide-new [tid ono divides]
  (try
    (clojure.pprint/pprint divides)
    (if-let [inout (core/inout-get ono)]
      (let [list (->> (vals divides)
                      (map #(merge % {:val (Integer/valueOf (:val %))}))
                      (filter #(and (not= 0 (:val %))
                                    (= "main" (:tid %)))))
            sum (reduce + (map #(Integer/valueOf (:val %)) list))]
        (clojure.pprint/pprint inout)
        (clojure.pprint/pprint list)
        (if (= (:amount inout) sum)
          (core/divide-new tid ono (->> list
                                        (map #(select-keys % [:val :bid]))
                                        (map #(merge % {:ono       (:ono inout)
                                                        :comment   (:comment inout)
                                                        :base_date (:base_date inout)}))
                                        (mapv #(clojure.set/rename-keys % {:val :amount}))))
          (throw (Exception. "not same!")))))
    {:status 200 :body {}}

    (catch Exception e
      {:status 500
       :body   {:error-text (.getMessage e)}})))

(comment



  (let [x {:cover
           {:tid         "main",
            :amount      317702,
            :val         "-800000",
            :bucket-name "보험",
            :bid         "cover"},
           :loan
           {:tid         "main",
            :amount      2220000,
            :val         "-1000000",
            :bucket-name "대출",
            :bid         "loan"},
           :travel
           {:tid         "main",
            :amount      1000000,
            :val         0,
            :bucket-name "여행",
            :bid         "travel"},
           :event
           {:tid "main", :amount 0, :val 0, :bucket-name "경조사", :bid "event"},
           :stash
           {:tid         "main",
            :amount      -63767,
            :val         0,
            :bucket-name "비상금",
            :bid         "stash"},
           :buffer
           {:tid "main", :amount 0, :val 0, :bucket-name "버퍼", :bid "buffer"}}]
    (divide-new 51 x)
    #_(let [list (filter #(not= 0 (:val %))
                         (map #(Integer/valueOf (:val %)) (vals x)))]
        list))



  (let [list (filter #(not= 0 (:val %))
                     (vals {:cover {:tid "main", :amount 317702, :val 0, :bucket-name "보험", :bid "cover"},
                            :loan  {:tid "main", :amount 2220000, :val -180000, :bucket-name "대출", :bid "loan"}
                            :nana  {:tid "main", :amount 2220000, :val -1800000, :bucket-name "대출", :bid "nana"}}))]

    (reduce + (map :val list)))

  ()

  (let [x]
    ())
  (core/inout-get 49))


