(ns tbuck.cljs.actions
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [tbuck.cljs.state :as state :refer [s-main s-pieces]]
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


(defn get-pieces []
      (go (let [response (<! (http/get (str backend "/api/piece-recent-list")
                                       {:with-credentials? false}))]

               (state/set-pieces (-> response :body :pieces)))))



(defn get-piece [piece-id]
      (go (let [response (<! (http/get (str backend "/api/piece/" piece-id)
                                       {:with-credentials? false}))]
               (state/set-main (-> response :body :piece)))))






