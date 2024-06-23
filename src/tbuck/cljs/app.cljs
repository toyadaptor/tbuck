(ns tbuck.cljs.app
  (:require [reagent.core :as reagent]
            [reitit.core :as r]
            [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]
            [reitit.coercion.spec :as rss]
            [reagent.dom :as rdom]
            [tbuck.cljs.state :refer [s-main s-pieces]]
            [tbuck.cljs.actions :as action]))

(defonce match (reagent/atom nil))

(defn log-fn [& params]
      (fn [_]
          (apply js/console.log params)))


(defn bucket-detail []
      (reagent/create-class
        {:display-name         "piece one"
         :component-did-update (fn [this [_ prev-argv]]
                                   (let [[_ new-argv] (reagent/argv this)
                                         nid (-> new-argv :parameters :path :id)
                                         prev-id (-> prev-argv :parameters :path :id)]
                                        (if (not= nid prev-id)
                                          (action/get-piece nid))))

         :component-did-mount  (fn [this]
                                   (let [[_ new-argv] (reagent/argv this)
                                         nid (-> new-argv :parameters :path :id)]
                                        (action/get-piece nid)))

         :reagent-render       (fn []
                                   [:div
                                    [:strong (@s-main :subject)]
                                    [:h5.subtitle.mb-2 (@s-main :summary)]
                                    #_[:small.has-text-grey (@s-main :mtime)]
                                    #_(when (@s-main :music)
                                            [:a {:on-click #(change-music {:path  (str "https://b.monologue.me" (-> @s-main :music :path))
                                                                           :title (-> @s-main :music :title)
                                                                           :page  (@s-main :id)
                                                                           :start true})}

                                             [:span.icon [:i.fas.fa-external-link-square-alt]]])
                                    [:div.content.mt-5
                                     {:dangerouslySetInnerHTML
                                      {:__html (@s-main :content-parsed)}}]])}))



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
                                            [:span.is-pulled-left.has-text-weight-bold.is-size-5 [:a (str (:bucket-name bucket))]]
                                            [:span.is-pulled-right.has-text-weight-bold.is-size-4 (:amount bucket)]]]])]])}))









(def routes
  [["/main" {:name ::main
             :view main-component}]
   ["/bucket/:bid" {:name       ::bucket-detail
                    :parameters {:path {:bid string?}}
                    :view       bucket-detail}]])


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
         [:span.is-pulled-right [:a "입출금 이력"]]]]



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


         [:div.columns.mt-5
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


       #_[:div.modal.is-active
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