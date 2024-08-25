(ns tbuck.clj.server
  (:require
    [clojure.data.json :as json]
    [reitit.core :as reitit]
    [tbuck.clj.api :as api]
    [muuntaja.core :as muun]
    [reitit.ring :as ring]
    [reitit.ring.coercion :as rrc]
    [reitit.ring.middleware.muuntaja :as rrm-muuntaja]
    [reitit.ring.middleware.parameters :as rrm-parameter]
    [reitit.coercion.spec]
    [ring.adapter.jetty :as jetty]
    [ring.middleware.cors :refer [wrap-cors]]
    [buddy.auth :refer [authenticated?]]
    [buddy.auth.middleware :refer [wrap-authentication]]
    [buddy.auth.backends.token :refer [jws-backend]]
    [buddy.sign.jwt :as jwt]))


(def secret "super-secret-key")


(def users
  {"user"  {:password "pass" :roles [:user]}
   "admin" {:password "adminpass" :roles [:admin]}})

(defn generate-token [username]
  (jwt/sign {:user  username
             :roles (get-in users [username :roles])}
            secret))

(defn authenticate [username password]
  (if-let [user (get users username)]
    (if (= password (:password user))
      (generate-token username)
      nil)))
(defn login [username password]
  (if-let [token (authenticate username password)]
    {:status 200
     :body   {:token token}}
    {:status 401
     :body   {:error "Invalid username or password"}}))

(comment


  (authenticate "user" "pass")
  (login {:body {:username "user" :password "pass"}}))

(def app-public-router
  (ring/router
    [["/api"
      ["/login" {:post {:body-params {:username string?
                                      :password string?}
                        :handler     (fn [{{:keys [username password]} :body-params}]
                                       (login username password))}}]]]
    {:data {:coercion   reitit.coercion.spec/coercion
            :muuntaja   muun/instance
            :middleware [rrm-muuntaja/format-middleware
                         rrm-parameter/parameters-middleware
                         rrc/coerce-exceptions-middleware
                         rrc/coerce-request-middleware
                         rrc/coerce-response-middleware]}}))

(def app-private-router
  (ring/router
    [["/api"
      ["/main" {:get {:parameters {}
                      :responses  {200 {}}
                      :handler    (fn [{:keys []}]
                                    {:status 200
                                     :body   (api/main "main")})}}]
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

      ["/buckets" {:get {:responses {200 {}}
                         :handler   (fn [_]
                                      (api/bucket-list "main"))}}]

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
      ["/inout/:ono/divide-new-ready"
       ; divide 를 위한 정보 조회
       {:get {:parameters {:path {:ono int?}}
              :responses  {200 {}}
              :handler    (fn [{{{:keys [ono]} :path} :parameters}]
                            (api/inout-info-for-divide-new ono))}}]

      ["/inout/:ono/divide-new"
       ; divide new
       {:post {:parameters  {:path {:ono int?}}
               :body-params {:divides map?}
               :responses   {200 {:body map?}}
               :handler     (fn [{{{:keys [ono]} :path} :parameters
                                  {:keys [divides]}     :body-params}]
                              (api/divide-new "main" ono divides))}}]


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
                         rrc/coerce-response-middleware]}}))

(def app-route
  (ring/ring-handler
    (ring/routes
      app-public-router
      app-private-router)
    (ring/routes
      (ring/create-resource-handler {:path "/"})
      (ring/create-default-handler))))

(def app
  (-> app-route
      (wrap-cors :access-control-allow-origin [#".*"]
                 :access-control-allow-methods [:get :post :put :delete :options]
                 :access-control-allow-headers ["Content-Type"])
      (wrap-authentication (jws-backend {:secret secret}))))



(defn start []
  (jetty/run-jetty app {:port 1234, :join? false})
  (println "start!"))

(comment
  (reitit.core/routes app-router)
  (app-route {:uri            "/api/inout/51/divide-new"
              :request-method :post
              :headers        {"Content-Type" "application/json"}})



  (app-route {:uri     "/api/buckets" :request-method :get
              :headers {"Content-Type" "application/json"}})

  (-> (app-route {:uri     "/api/tong/main/inout-new" :request-method :post
                  :form    {:amount 1000 :base-date "20240803" :comment "hi"}
                  :headers {"Content-Type" "application/json"}})
      :body slurp json/read-str)

  (-> (app-route {:uri "/api/bucket/asdf/divides" :request-method :get})
      :body slurp json/read-str)
  (-> (app-route {:uri "/api/tong/zxcv/inouts" :request-method :get})
      :body slurp json/read-str))



