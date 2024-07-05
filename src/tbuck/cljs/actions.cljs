(ns tbuck.cljs.actions
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [tbuck.cljs.state :as state]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]))

(def backend "http://192.168.0.200:1234")

(defn get-main []
      (go (let [response (<! (http/get (str backend "/api/main")
                                       {:with-credentials? false}))]
               (state/set-main (-> response :body)))))


(defn get-tong-inouts [tid]
      (go (let [response (<! (http/get (str backend "/api/tong/" tid "/inouts")
                                       {:with-credentials? false}))]
               (state/set-tong-inouts (-> response :body)))))

(defn get-bucket-divides [bid]
      (go (let [response (<! (http/get (str backend "/api/bucket/" bid "/divides")
                                       {:with-credentials? false}))]
               (state/set-bucket-divides (-> response :body)))))

(defn get-tong-inouts-detail [ono]
      (go (let [response (<! (http/get (str backend "/api/inouts/" ono)
                                       {:with-credentials? false}))]

               (state/set-inouts-detail (-> response :body)))))

(defn get-bucket-divides-detail [dno]
      (go (let [response (<! (http/get (str backend "/api/divides/" dno)
                                       {:with-credentials? false}))]

               (state/set-divides-detail (-> response :body)))))












