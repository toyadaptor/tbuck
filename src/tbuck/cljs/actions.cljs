(ns tbuck.cljs.actions
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [tbuck.cljs.state :as state]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]))

(def backend "http://localhost:1234")

(defn get-main []
      (go (let [response (<! (http/get (str backend "/api/private/main")
                                       {:with-credentials? true}))]
               (state/set-main (-> response :body)))))


(defn get-tong-inouts [tid]
      (go (let [response (<! (http/get (str backend "/api/private/tong/" tid "/inouts")
                                       {:with-credentials? true}))]
               (state/set-tong-inouts (-> response :body)))))




(defn get-bucket-divides [bid]
      (go (let [response (<! (http/get (str backend "/api/private/bucket/" bid "/divides")
                                       {:with-credentials? true}))]
               (state/set-bucket-divides (-> response :body)))))

(defn get-tong-inouts-detail [ono]
      (go (let [response (<! (http/get (str backend "/api/private/inouts/" ono)
                                       {:with-credentials? true}))]

               (state/set-inouts-detail (-> response :body)))))



(defn get-bucket-list []
      (go (let [response (<! (http/get (str backend "/api/private/buckets")
                                       {:with-credentials? true}))]
               (state/set-buckets (-> response :body)))))

(defn get-bucket-divides-detail [dno]
      (go (let [response (<! (http/get (str backend "/api/private/divides/" dno)
                                       {:with-credentials? true}))]

               (state/set-divides-detail (-> response :body)))))







(defn create-tong-inout [tid {:keys [amount base-date comment]} close-fn]
      (go (let [response (<! (http/post (str backend "/api/private/tong/" tid "/inouts")
                                        {:with-credentials? true
                                         :json-params       {:amount    (js/parseInt amount 10)
                                                             :base-date base-date
                                                             :comment   comment}}))]
               (if (= 200 (:status response))
                 (do (close-fn)
                     (get-tong-inouts tid))
                 (js/alert (-> response :body :error-text))))))


(defn remove-tong-inout [tid ono]
      (go (let [response (<! (http/delete (str backend "/api/private/inouts/" ono)
                                          {:with-credentials? true}))]
               (get-tong-inouts tid))))


(defn get-divide-new-ready [ono callback]
      (go (let [response (<! (http/get (str backend "/api/private/inout/" ono "/divide-new-ready")
                                       {:with-credentials? true}))]
               (state/set-divide-new-ready (-> response :body))
               (callback))))


(defn create-bucket-divide [ono divides callback]
      (go (let [response (<! (http/post (str backend "/api/private/inout/" ono "/divide-new")
                                        {:with-credentials? true
                                         :json-params       {:divides divides}}))]
               (callback response))))


(defn do-login [username password callback]
      (go (let [response (<! (http/post (str backend "/api/login")
                                        {:with-credentials? true
                                         :json-params       {:username username
                                                             :password password}}))]
               (state/set-login true)
               (callback response))))


(defn do-logout [callback]
      (go (let [response (<! (http/post (str backend "/api/private/logout")
                                        {:with-credentials? true}))]
               (state/set-login false)
               (callback response))))


