(ns tbuck.cljs.app
  (:require [reagent.core :as reagent]
            [reitit.core :as r]
            [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]
            [reitit.coercion.spec :as rss]
            [reagent.dom :as rdom]
            [tbuck.cljs.state :refer [s-main s-tong-inouts s-pieces s-bucket-divides]]
            [tbuck.cljs.actions :as action]))

(defonce match (reagent/atom nil))

(defn log-fn [& params]
      (fn [_]
          (apply js/console.log params)))


(defn bucket-page [match]
      (let [{:keys [path]} (:parameters match)]
           (reagent/create-class
             {:display-name         "Bucket >"
              :component-did-mount  (fn [this]

                                        (action/get-bucket-divides (:bid path)))
              :reagent-render       (fn []
                                        [:div
                                         [:div.columns
                                          [:div.column
                                           [:p.is-pulled-left.title.is-4 (str "Buckets > " (:bucket-name (:bucket @s-bucket-divides)))]
                                           [:a.is-pulled-right {:href (rfe/href ::main)} "메인으로"]]]

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
                                                   [:td [:button.button
                                                         {:on-click #(action/get-bucket-divides-detail (:dno div))}
                                                         "detail"]]])]]]]])})))




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
                                        [:div.columns
                                         [:div.column.table-container
                                          [:p.is-pulled-left.title "입출금 이력"]
                                          [:a.is-pulled-right {:href (rfe/href ::main)} "메인으로"]]]
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
                                                   [:td [:button.button
                                                         {:on-click #(action/get-tong-inouts-detail (:ono inout))}
                                                         "detail"]]])]]]]])})))





(defn main-component []
      (reagent/create-class
        {:display-name        "main"
         :component-did-mount (fn []
                                  (action/get-main))

         :reagent-render      (fn []
                                  [:div
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




(def routes
  [["/main" {:name ::main
             :view main-component}]
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
          [:header.mb-5
           #_[:p.title "T-BUCK"]
           [:figure.image
            [:img {:src "assets/roomel_coffee.jpg"}]]]]]]

       [:div.columns
        [:div.column
         [:span.is-pulled-left.title "Account"]
         [:span.is-pulled-right
          [:a {:href (rfe/href ::tong {:tid "main"})}
           "입출금 이력"]]]]

       [:div.columns
        [:div.column
         [:div.columns.mt-5
          [:div.column
           [:span.is-pulled-left.is-italic (or (:tong-name @s-main) "no account")]
           [:span.is-pulled-right.has-text-weight-bold.is-size-3 (:tong-amount @s-main)]]]


         [:div.columns.mt-5
          [:div.column
           [:span.is-pulled-left.is-italic "마지막 입출금 입력"]
           [:span.is-pulled-right.has-text-weight-bold (:last-inout @s-main)]]]


         #_[:div.columns.mt-5
            [:div.column
             [:span.is-pulled-left.is-italic "buckets 합계 일치 여부"]
             [:span.is-pulled-right.has-text-weight-bold (if (:is-valid-sum @s-main)
                                                           "정상" "확인 필요")]]]]]

       [:div.columns
        [:div.column
         [:section.section.pl-3.pr-3
          (if @match
            (let [view (:view (:data @match))]
                 [view @match])
            [main-component])]]]

       [:footer
        [:p [:span.icon-text
             [:span.icon [:i.fas.fa-copyright]]
             [:span "Mel family"]]]]

       [:div.modal {:id "ono-modal"}
        [:div.modal-background]
        [:div.modal-card
         [:header.modal-card-head
          [:p.modal-card-title ""]
          [:button.delete {:aria-label "close"}]]
         [:section.modal-card-body
          "asoidjfoasijdof"]

         [:footer.modal-card-foot
          [:div.buttons
           [:button.button "닫기"]]]]]
       #_[:div.modal.is-active {:id "dno-modal"}
          [:div.modal-background]
          [:div.modal-card
           [:header.modal-card-head
            [:p.modal-card-title "Modal title"]
            [:button.delete {:aria-label "close"}]]
           [:section.modal-card-body
            "asoidjfoasijdof"]

           [:footer.modal-card-foot
            [:div.buttons
             [:button.button "닫기"]]]]]])






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





