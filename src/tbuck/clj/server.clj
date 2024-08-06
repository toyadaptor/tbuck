(ns tbuck.clj.server
  (:require
    [clojure.data.json :as json]
    [tbuck.clj.api :as api]
    [muuntaja.core :as muun]
    [reitit.ring :as ring]
    [reitit.ring.coercion :as rrc]
    [reitit.ring.middleware.muuntaja :as rrm-muuntaja]
    [reitit.ring.middleware.parameters :as rrm-parameter]
    [reitit.coercion.spec]
    [ring.adapter.jetty :as jetty]
    [ring.middleware.cors :refer [wrap-cors]]))

(def app-route
  (ring/ring-handler
    (ring/router
      [["/api"
        ["/main" {:get {:parameters {}
                        :responses  {200 {}}
                        :handler    (fn [{:keys []}]
                                      {:status 200
                                       :body   (api/main)})}}]
        ["/tong/:tid/inouts" {:get  {:parameters {:path {:tid string?}}
                                     :handler    (fn [{{{:keys [tid]} :path} :parameters}]
                                                   (clojure.pprint/pprint tid)
                                                   {:status 200
                                                    :body   (api/tong-inouts tid)})}
                              :post {:parameters  {:path {:tid string?}}
                                     :body-params {:amount    int?
                                                   :base-date string?
                                                   :comment   string?}
                                     :handler     (fn [{{{:keys [tid]} :path}              :parameters
                                                        {:keys [amount base-date comment]} :body-params}]
                                                    (api/tong-inout-new tid amount base-date comment))}}]


        ["/bucket/:bid/divides" {:get {:parameters {:path {:bid string?}}
                                       :responses  {200 {}}
                                       :handler    (fn [{{{:keys [bid]} :path} :parameters}]
                                                     {:status 200
                                                      :body   (api/bucket-divides bid)})}}]

        ["/inouts/:ono" {:get    {:parameters {:path {:ono int?}}
                                  :responses  {200 {:piece {}}}
                                  :handler    (fn [{{{:keys [ono]} :path} :parameters}]
                                                (clojure.pprint/pprint ono)
                                                {:status 200
                                                 :body   (api/tong-inouts-detail ono)})}
                         :delete {:parameters {:path {:ono int?}}
                                  :responses  {200 {:piece {}}}
                                  :handler    (fn [{{{:keys [ono]} :path} :parameters}]
                                                (clojure.pprint/pprint ono)
                                                {:status 200
                                                 :body   (api/tong-inouts-removing ono)})}}]

        ["/divides/:dno" {:get {:parameters {:path {:dno int?}}
                                :responses  {200 {}}
                                :handler    (fn [{{{:keys [dno]} :path} :parameters}]
                                              {:status 200
                                               :body   (api/bucket-divides-detail dno)})}}]]]


      {:data {:coercion   reitit.coercion.spec/coercion
              :muuntaja   muun/instance
              :middleware [rrm-muuntaja/format-middleware
                           rrm-parameter/parameters-middleware
                           rrc/coerce-exceptions-middleware
                           rrc/coerce-request-middleware
                           rrc/coerce-response-middleware]}})
    (ring/routes
      (ring/create-resource-handler {:path "/"})
      (ring/create-default-handler))))

(def app
  (wrap-cors app-route
             :access-control-allow-origin [#".*"]
             :access-control-allow-methods [:get :post :put :delete :options]
             :access-control-allow-headers ["Content-Type"]))

(defn start []
  (jetty/run-jetty app {:port 1234, :join? false})
  (println "start!"))

(comment
  (-> (app-route {:uri     "/api/tong/main/inout-new" :request-method :post
                  :form    {:amount 1000 :base-date "20240803" :comment "hi"}
                  :headers {"Content-Type" "application/json"}})
      :body slurp json/read-str)

  (-> (app-route {:uri "/api/bucket/asdf/divides" :request-method :get})
      :body slurp json/read-str)
  (-> (app-route {:uri "/api/tong/zxcv/inouts" :request-method :get})
      :body slurp json/read-str))

