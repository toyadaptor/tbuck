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
    ;[buddy.auth.backends.token :refer [jws-backend]]
    [buddy.core.nonce :as nonce]
    [buddy.core.codecs :as codecs]
    [buddy.auth.backends.token :refer [token-backend]]
    [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
    [buddy.sign.jwt :as jwt]))


;(def secret "super-secret-key")

;(def users
;  {"user"  {:password "pass" :roles [:user]}
;   "admin" {:password "adminpass" :roles [:admin]}})
;
;(defn generate-token [username]
;  (jwt/sign {:user  username
;             :roles (get-in users [username :roles])}
;            secret))
;
;(defn authenticate [username password]
;  (if-let [user (get users username)]
;    (if (= password (:password user))
;      (generate-token username)
;      nil)))
;(defn login [username password]
;  (if-let [token (authenticate username password)]
;    {:status 200
;     :body   {:token token}}
;    {:status 401
;     :body   {:error "Invalid username or password"}}))

(defn ok [d] {:status 200 :body d})
(defn bad-request [d] {:status 400 :body d})
(def authdata {:admin "secret"
               :test  "secret"})

(def tokens (atom {}))

(defn random-token
  []
  (let [randomdata (nonce/random-bytes 16)]
    (codecs/bytes->hex randomdata)))
(defn login
  [username password]
  (let [valid? (some-> authdata
                       (get (keyword username))
                       (= password))]
    (if valid?
      (let [token (random-token)]
        (swap! tokens assoc (keyword token) (keyword username))
        (ok {:token token}))
      (bad-request {:message "wrong auth data"}))))


;(def backend
;  (jws-backend {:secret secret
;                :options {:alg :hs512}}))
;
;(def auth-backend
;  (jws-backend {:secret "my-secret-key"
;                :options {:alg :hs512}}))
;
;(defn wrap-auth [handler]
;  (wrap-authentication handler auth-backend))
;
;(defn verify-token [token]
;  (try
;    (jwt/unsign token secret {:alg :hs512})
;    (catch Exception e
;      nil))) ;; 검증 실패 시 nil 반환

;(defn my-authfn [req token]
;  (println "my-authfn - " token)
;  (when token
;    (when-let [user (get @tokens (keyword token))]
;      user)
;    ))
(defn my-authfn
  [request token]
  (let [token (keyword token)]
    (get @tokens token nil)))

(def auth-backend
  (token-backend {:authfn my-authfn
                  :token-name "Token"
                  :optional? false
                  :unauthorized-handler (fn [] {:status 401 :body "Unauthorized"})}))

(comment
  (let [token "asdf"]
    (when-let [user (get @tokens (keyword token))]
      user))

  (my-authfn nil "af059835f20d2e5d12431424a98e162e")
  (my-authfn nil "asdf")

  (get @tokens (keyword "asdf"))
  (swap! tokens assoc (keyword "asdf") (keyword "admin"))
  @tokens)

(def app-router
  (ring/router
    [["/api"
      ["/login" {:post {:body-params {:username string?
                                      :password string?}
                        :handler     (fn [{{:keys [username password]} :body-params}]
                                       (login username password))}}]
      ["/main" {:get {:parameters {}
                      :responses  {200 {}}
                      :handler    (fn [{:keys [identity]}]
                                    {:status 200
                                     :body   {:identity identity} #_(api/main "main")})}}]
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
                         rrc/coerce-response-middleware
                         ]}}))



(def app-route
  (ring/ring-handler
    app-router
    (ring/create-resource-handler {:path "/"})
    (ring/create-default-handler)))


(defn require-authentication [handler]
  (fn [req]
    (if (some? (get-in req [:headers "authorization"]))
      (handler req)
      {:status 401 :body "Unauthorized"})))
(def app
  (-> app-route
      (wrap-cors :access-control-allow-origin [#".*"]
                 :access-control-allow-methods [:get :post :put :delete :options]
                 :access-control-allow-headers ["Content-Type"])
      (require-authentication)
      (wrap-authentication auth-backend)

      ))



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



