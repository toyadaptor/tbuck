(ns web
  (:use org.httpkit.server)
  (:require [main :refer :all]
            [reitit.ring :as ring]
            [ring.middleware.file :refer [wrap-file]]
            [ring.middleware.params :refer [wrap-params]]))


(defn handler [req]
  (cond (clojure.string/starts-with? (:uri req) "/main")
        {:status 200
         :headers {"Content-Type" "text/html"}
         :body "ok"}
        :else {:status 404
               :body "not found"}))



(def app-handler
  (-> handler
      wrap-params
      (wrap-file "resources/public" {:prefer-handler? true})))

(defn start [& _]
  (run-server app-handler {:port 3333}))

(comment
  (start)
  (tong-list))
