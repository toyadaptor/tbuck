(ns tbuck.cljs.app
  (:require [reagent.core :as reagent]
            [reitit.core :as r]
            [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]
            [reitit.coercion.spec :as rss]
            [reagent.dom :as rdom]
            [tbuck.cljs.state :refer [s-main s-tong-inouts s-bucket-divides s-inout-divides s-buckets s-divide-new-ready s-login]]
            [tbuck.cljs.actions :as action]
            [tbuck.cljs.util :refer [today-in-yyyymmdd get-auth-cookie]]))

(defonce match (reagent/atom nil))

(defonce is-inout-divides-modal (reagent/atom false))
(defonce is-inout-new-modal (reagent/atom false))
(defonce is-divide-new-modal (reagent/atom false))



(defonce input-inout-new (reagent/atom {:amount    ""
                                        :base-date ""
                                        :comment   ""}))

(defonce input-divide-new-buckets (reagent/atom {}))
(defonce input-divide-new-remain (reagent/atom 0))
(defonce input-divide-new-sum-check (reagent/atom false))

(defonce input-login (reagent/atom {}))

(defn calculate-sum [data]
      (- (js/parseInt (-> @s-divide-new-ready :inout :amount))
         (reduce + (map (comp js/parseInt :val) (vals data)))))

(add-watch input-divide-new-buckets :update-sum
           (fn [_ _ _ new-state]
               (reset! input-divide-new-remain (calculate-sum new-state))))

(add-watch input-divide-new-remain :update-sum-check
           (fn [_ _ _ new-state]
               (reset! input-divide-new-sum-check (= new-state 0))))



(defn open-inout-new-modal []
      (swap! input-inout-new assoc :base-date (today-in-yyyymmdd))
      (reset! is-inout-new-modal true))

(defn close-inout-new-modal []
      (swap! input-inout-new assoc :amount "" :base-date (today-in-yyyymmdd) :comment "")
      (reset! is-inout-new-modal false))



(defn open-divide-new-modal [ono]
      (action/get-divide-new-ready
        ono
        (fn []
            (reset! input-divide-new-buckets (into {} (map (fn [[k v]] {k (merge v {:val 0})})
                                                           (:buckets @s-divide-new-ready))))
            (reset! is-divide-new-modal true))))

(defn close-divide-new-modal []
      (reset! input-divide-new-buckets nil)
      (reset! input-divide-new-remain 0)
      (reset! is-divide-new-modal false))

(defn log-fn [& params]
      (fn [_]
          (apply js/console.log params)))



(def account-component
  (reagent/create-class
    {:display-name   "account"
     :reagent-render (fn []
                         [:div
                          [:div.columns
                           [:div.column
                            [:a.is-pulled-right {:on-click #(action/do-logout (fn []
                                                                                  (rfe/push-state ::login)))} "logout"]]]
                          [:div.columns.mt-5
                           [:div.column
                            [:span.is-pulled-left.title "Account"]
                            [:span.is-pulled-right
                             [:a.is-pulled-right {:href (rfe/href ::main)} "메인으로"]]]]

                          [:div.columns
                           [:div.column
                            [:div.columns.mt-5
                             [:div.column
                              [:span.is-pulled-left.is-italic (or (:tong-name @s-main) "no account")]
                              [:span.is-pulled-right.has-text-weight-bold.is-size-3 (:tong-amount @s-main)]]]

                            [:div.columns.mt-5
                             [:div.column
                              [:div.field.is-grouped.is-pulled-left
                               [:div.control
                                [:a.button.is-small {:href (rfe/href ::tong {:tid "main"})}
                                 [:span.icon-text
                                  [:span.icon [:i.fas.fa-history]]]]]
                               [:div.control
                                [:a.button.is-small
                                 {:on-click #(open-inout-new-modal)}
                                 [:span.icon-text
                                  [:span.icon [:i.fas.fa-plus]]]]]]
                              [:span.is-pulled-right.has-text-weight-bold (str "마지막 입출금 " (:last-inout @s-main))]]]
                            #_[:div.columns.mt-5
                               [:div.column
                                [:span.is-pulled-left.is-italic "buckets 합계 일치 여부"]
                                [:span.is-pulled-right.has-text-weight-bold (if (:is-valid-sum @s-main)
                                                                              "정상" "확인 필요")]]]]]

                          ;; inout new
                          [:div.modal {:id "inout-new-modal" :class (if @is-inout-new-modal "is-active" "")}
                           [:div.modal-background]
                           [:div.modal-card
                            [:header.modal-card-head
                             [:p.modal-card-title "입출금 추가"]
                             [:button.delete {:aria-label "close"
                                              :on-click   #(reset! is-inout-new-modal false)}]]
                            [:section.modal-card-body
                             [:div.columns
                              [:div.column
                               [:p.title.is-4 ""]
                               [:div.field
                                [:label.label "금액"]
                                [:div.control
                                 [:input.input {:type         "tel"
                                                :value        (:amount @input-inout-new)
                                                :place-holder "0 이면 버켓 리밸런싱"
                                                :on-change    #(swap! input-inout-new assoc :amount (-> % .-target .-value))}]]]

                               [:div.field
                                [:label.label "날짜"]
                                [:div.control
                                 [:input.input {:type      "tel"
                                                :value     (:base-date @input-inout-new)
                                                :on-change #(swap! input-inout-new assoc :base-date (-> % .-target .-value))}]]]

                               [:div.field
                                [:label.label "메모"]
                                [:div.control
                                 [:input.input {:type      "text"
                                                :value     (:comment @input-inout-new)
                                                :on-change #(swap! input-inout-new assoc :comment (-> % .-target .-value))}]]]]]]

                            [:footer.modal-card-foot
                             [:div.buttons
                              [:button.button.is-info
                               {:on-click #(do (action/create-tong-inout "main" @input-inout-new close-inout-new-modal))} "저장"]
                              [:button.button
                               {:on-click #(reset! is-inout-new-modal false)} "닫기"]]]]]

                          [:br]])}))


(defn bucket-page [match]
      (let [{:keys [path]} (:parameters match)]
           (reagent/create-class
             {:display-name        "Bucket >"
              :component-did-mount (fn [this]

                                       (action/get-bucket-divides (:bid path)))
              :reagent-render      (fn []
                                       [:div
                                        [account-component]

                                        [:div.columns
                                         [:div.column
                                          [:p.title.is-4 (str "Buckets > " (:bucket-name (:bucket @s-bucket-divides)))]]]

                                        [:div.columns
                                         [:div.column
                                          [:p (:comment (:bucket @s-bucket-divides))]]]

                                        [:div.columns
                                         [:div.column
                                          [:p.title.is-3 (:amount (:bucket @s-bucket-divides))]]]

                                        [:div.columns
                                         [:div.column
                                          [:table.table.is-fullwidth.is-striped
                                           [:thead
                                            [:th "amount"]
                                            [:th "comment"]
                                            [:th]]
                                           [:tbody
                                            (for [div (:divides @s-bucket-divides)]
                                                 ^{:key div}
                                                 [:tr
                                                  [:td (:amount div)]
                                                  [:td (:base-date div)
                                                   [:br]
                                                   (:comment div)]
                                                  [:td


                                                   [:a.button
                                                    {:on-click #(do (action/get-bucket-divides-detail (:dno div))
                                                                    (reset! is-inout-divides-modal true))}
                                                    [:span.icon-text
                                                     [:span.icon [:i.fas.fa-layer-group]]]]]])]]]]])})))









(defn tong-page [match]
      (let [{:keys [path]} (:parameters match)]
           (reagent/create-class
             {:display-name        "tong"
              ;:component-did-update (fn [this [_ prev-argv]]
              ;                          (let [[_ new-argv] (reagent/argv this)
              ;                                nid (-> new-argv :parameters :path :id)
              ;                                prev-id (-> prev-argv :parameters :path :id)]
              ;                               (if (not= nid prev-id)
              ;                                 (action/get-piece nid))))
              ;
              :component-did-mount (fn [this]
                                       (action/get-tong-inouts (:tid path)))

              :reagent-render      (fn []
                                       [:div
                                        [account-component]

                                        [:div.columns
                                         [:div.column.table-container
                                          [:p.is-pulled-left.title "입출금 이력"]]]

                                        [:div.columns
                                         [:div.column
                                          [:table.table.is-fullwidth.is-striped
                                           [:thead
                                            [:th "amount"]
                                            [:th "comment"]
                                            [:th]]
                                           [:tbody
                                            (for [inout (:inouts @s-tong-inouts)]
                                                 ^{:key inout}
                                                 [:tr
                                                  [:td (:amount inout)]
                                                  [:td (:base-date inout)
                                                   [:br]
                                                   (:comment inout)]
                                                  [:td
                                                   (if (:is_divide inout)
                                                     ; true - history
                                                     [:div.field.is-grouped
                                                      [:div.control
                                                       [:a.button.is-small
                                                        {:on-click #(do (action/get-tong-inouts-detail (:ono inout))
                                                                        (reset! is-inout-divides-modal true))}
                                                        [:span.icon-text
                                                         [:span.icon [:i.fas.fa-layer-group]]]]]]
                                                     ; false - divide
                                                     [:div.field.is-grouped
                                                      [:div.control
                                                       [:a.button.is-small.is-info
                                                        {:on-click #(do (action/get-bucket-list)
                                                                        (open-divide-new-modal (:ono inout)))}

                                                        [:span.icon-text
                                                         [:span.icon [:i.fas.fa-divide]]]]]
                                                      ; false - remove
                                                      [:div.control
                                                       [:a.button.is-small.is-danger
                                                        {:on-click #(when (js/confirm (str (:comment inout) " - 삭제할까?"))
                                                                          (action/remove-tong-inout "main" (:ono inout))
                                                                          (js/console.log "ok"))}

                                                        [:span.icon-text
                                                         [:span.icon [:i.fas.fa-trash]]]]]])]])]]]]
                                        ;; divide new
                                        (when @is-divide-new-modal
                                              [:div.modal {:id "inout-new-modal" :class (if @is-divide-new-modal "is-active" "")}
                                               [:div.modal-background]
                                               [:div.modal-card
                                                [:header.modal-card-head
                                                 [:p.modal-card-title "버켓 분배"]
                                                 [:button.delete {:aria-label "close"
                                                                  :on-click   #(reset! is-divide-new-modal false)}]]
                                                [:section.modal-card-body
                                                 [:div.columns
                                                  [:div.column
                                                   [:p.title.is-4 "입출금 정보"]

                                                   [:div.field.is-horizontal
                                                    [:div.field-label.is-normal
                                                     [:label.label "날짜"]]
                                                    [:div.field-body
                                                     [:div.field
                                                      [:div.control
                                                       [:input.input {:disabled "disabled" :value (-> @s-divide-new-ready :inout :base-date)}]]]]]

                                                   [:div.field.is-horizontal
                                                    [:div.field-label.is-normal
                                                     [:label.label "amount"]]
                                                    [:div.field-body
                                                     [:div.field
                                                      [:div.control
                                                       [:input.input {:disabled "disabled" :value (.toLocaleString (-> @s-divide-new-ready :inout :amount))}]]]]]

                                                   [:div.field.is-horizontal
                                                    [:div.field-label.is-normal
                                                     [:label.label "comment"]]
                                                    [:div.field-body
                                                     [:div.field
                                                      [:div.control
                                                       [:input.input {:disabled "disabled" :value (-> @s-divide-new-ready :inout :comment)}]]]]]

                                                   [:hr]

                                                   [:p.title.is-4 "버켓 분배 정보"]

                                                   [:div.field.is-horizontal
                                                    [:div.field-label.is-normal
                                                     [:label.label.is-size-3 "남음"]]
                                                    [:div.field-body
                                                     [:div.field
                                                      [:div.control
                                                       [:p.is-size-3 (.toLocaleString @input-divide-new-remain)]]]]]


                                                   [:div
                                                    (for [[k bucket] @input-divide-new-buckets]
                                                         ^{:key k}

                                                         [:div.field.is-horizontal
                                                          [:div.field-label.is-normal
                                                           [:div.control
                                                            [:label.label (:bucket-name bucket)]]]

                                                          [:div.field-body
                                                           [:div.field
                                                            [:div.control
                                                             [:input.input {:type     "number"
                                                                            :disabled "disabled"
                                                                            :style    {:text-align "right"}
                                                                            :value    (.toLocaleString (:amount bucket))}]]]

                                                           [:div.field
                                                            [:div.control
                                                             [:input.input {:type      "tel"
                                                                            :value     (:val bucket)
                                                                            :style     {:text-align "right"}
                                                                            :on-change #(swap! input-divide-new-buckets assoc-in [k :val] (-> % .-target .-value))}]]]]])]]]]



                                                [:footer.modal-card-foot
                                                 [:div.buttons
                                                  [:button.button.is-info
                                                   {:disabled (if @input-divide-new-sum-check "" "disabled")
                                                    :on-click #(do (action/create-bucket-divide (-> @s-divide-new-ready :inout :ono)
                                                                                                @input-divide-new-buckets
                                                                                                (fn [response]
                                                                                                    (if (= 200 (:status response))
                                                                                                      (do (close-divide-new-modal)
                                                                                                          (action/get-main)
                                                                                                          (action/get-tong-inouts "main"))
                                                                                                      (js/alert (-> response :body :error-text))))))}
                                                   "저장"]


                                                  [:button.button
                                                   {:on-click #(close-divide-new-modal)} "닫기"]]]]])])})))




(defn main-component []
      (reagent/create-class
        {:display-name        "main"
         :component-did-mount (fn []
                                  (action/get-main))

         :reagent-render      (fn []
                                  [:div
                                   [account-component]

                                   [:p.title "Buckets"]
                                   [:ul.mt-5
                                    (for [bucket (:buckets @s-main)]
                                         ^{:key bucket}
                                         [:li.mt-5
                                          [:div.columns
                                           [:div.column
                                            [:span.is-pulled-left.has-text-weight-bold.is-size-5 [:a {:href (rfe/href ::bucket {:bid (:bid bucket)})}
                                                                                                  (:bucket-name bucket)]]

                                            [:span.is-pulled-right.has-text-weight-bold.is-size-4 (:amount bucket)]]]])]])}))





(defn login-component []
      (reagent/create-class
        {:display-name        "main"
         :component-did-mount (fn [])
         :reagent-render      (fn []
                                  [:div
                                   [:p.title "Login"]

                                   [:div.columns
                                    [:div.column
                                     [:div.field.is-horizontal
                                      [:div.field-label.is-normal
                                       [:label.label "username"]]
                                      [:div.field-body
                                       [:div.field
                                        [:div.control
                                         [:input.input
                                          {:on-change #(swap! input-login assoc :username (-> % .-target .-value))}]]]]]
                                     [:div.field.is-horizontal
                                      [:div.field-label.is-normal
                                       [:label.label "password"]]
                                      [:div.field-body
                                       [:div.field
                                        [:div.control
                                         [:input.input
                                          {:type      "password"
                                           :on-change #(swap! input-login assoc :password (-> % .-target .-value))}]]]]]]]

                                   [:div.columns
                                    [:div.column
                                     [:button.button.is-fullwidth.is-info
                                      {:on-click #(action/do-login (:username @input-login)
                                                                   (:password @input-login)
                                                                   (fn [response]
                                                                       (if (= 200 (:status response))

                                                                         (do
                                                                           (js/console.log "ok")
                                                                           (rfe/push-state ::main))


                                                                         (js/alert (-> response :body :error-text)))))} "submit"]]]])}))

















(def routes
  [["/main" {:name ::main
             :view main-component}]
   ["/login" {:name ::login
              :view login-component}]
   ["/tong/:tid" {:name       ::tong
                  :parameters {:path {:tid string?}}
                  :view       tong-page}]
   ["/bucket/:bid" {:name       ::bucket
                    :parameters {:path {:bid string?}}
                    :view       bucket-page}]])

(defn page-template []
      [:div.container.p-3
       [:header
        [:div.columns
         [:div.column.is-one-fifth
          [:header
           ;[:p.title "TBUCK"]
           [:figure.image
            [:img {:src "assets/roomel_coffee.jpg"}]]]]]]

       [:div.columns
        [:div.column
         [:section.section.pl-3.pr-3
          (if @s-login
            (if @match
              (let [view (:view (:data @match))]
                   [view @match])
              [main-component])
            [login-component])]]]


       [:footer
        [:p [:span.icon-text
             [:span.icon [:i.fas.fa-copyright]]
             [:span "Mel family"]]]]



       ;; common modal
       [:div.modal {:id "ono-modal" :class (if @is-inout-divides-modal "is-active" "")}
        [:div.modal-background]
        [:div.modal-card
         [:header.modal-card-head
          [:p.modal-card-title "입출금 분배"]
          [:button.delete {:aria-label "close"
                           :on-click   #(reset! is-inout-divides-modal false)}]]
         [:section.modal-card-body
          [:div.columns
           [:div.column
            [:p.title.is-4 "입출금"]
            [:table.table.is-fullwidth.is-striped
             [:thead
              [:th "amount"]
              [:th "comment"]
              [:th "date"]]
             [:tbody
              (let [inout (:inout @s-inout-divides)]
                   [:tr
                    [:td (:amount inout)]
                    [:td (-> @s-inout-divides :inout :comment)]
                    [:td (-> @s-inout-divides :inout :base-date)]])]]]]

          [:div.columns.mt-5
           [:div.column
            [:p.title.is-4 "버켓"]
            [:table.table.is-fullwidth.is-striped
             [:thead
              [:th "bucket"]
              [:th "amount"]
              [:th "comment"]]

             [:tbody
              (for [div (:divides @s-inout-divides)]
                   ^{:key div}
                   [:tr
                    [:td (:bucket-name div)]
                    [:td (:amount div)]
                    [:td (:comment div)]])]]]]]

         [:footer.modal-card-foot
          [:div.buttons
           [:button.button.is-fullwidth
            {:on-click #(reset! is-inout-divides-modal false)} "닫기"]]]]]])


(defn ^:dev/after-load start []
      (rfe/start!
        (rf/router routes {:data {:coercion rss/coercion}})
        (fn [m] (reset! match m))
        {:use-fragment true})

      (rdom/render
        [page-template]
        (.getElementById js/document "app")))


(defn ^:export main
      []
      (start))





