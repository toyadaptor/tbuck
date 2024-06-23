(ns tbuck.cljs.app
  (:require [reagent.core :as reagent]
            [reitit.core :as r]
            [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]
            [reitit.coercion.spec :as rss]
            [reagent.dom :as rdom]
            [tbuck.cljs.state :refer [s-piece s-pieces]]
            [tbuck.cljs.actions :as action]))

(defonce match (reagent/atom nil))

(defn log-fn [& params]
      (fn [_]
          (apply js/console.log params)))


(defn inout-list-component []
      (reagent/create-class
        {:display-name        "pieces"
         :component-did-mount (fn []
                                  (action/get-pieces))
         :reagent-render      (fn []
                                  [:div
                                   #_[:span.icon [:i.fas.fa-clock]]
                                   [:ul
                                    (for [piece @s-pieces]
                                         ^{:key piece}
                                         [:li [:a {:href (rfe/href ::piece-one {:id (piece :id)})}
                                               (piece :subject)]
                                          #_[:a [:span.tag.p-1.ml-2 " tag."]]])]])}))



(defn bucket-list-component []
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
                                    [:strong (@s-piece :subject)]
                                    [:h5.subtitle.mb-2 (@s-piece :summary)]
                                    #_[:small.has-text-grey (@s-piece :mtime)]
                                    #_(when (@s-piece :music)
                                            [:a {:on-click #(change-music {:path  (str "https://b.monologue.me" (-> @s-piece :music :path))
                                                                           :title (-> @s-piece :music :title)
                                                                           :page  (@s-piece :id)
                                                                           :start true})}

                                             [:span.icon [:i.fas.fa-external-link-square-alt]]])
                                    [:div.content.mt-5
                                     {:dangerouslySetInnerHTML
                                      {:__html (@s-piece :content-parsed)}}]])}))



(defn buckets-component []
      (reagent/create-class
        {:display-name        "main recent one"
         :component-did-mount (fn []
                                  (action/get-piece-recent-one))

         :reagent-render      (fn []
                                  [:div
                                   [:strong (@s-piece :subject)]
                                   [:h5.subtitle.mb-2 (@s-piece :summary)]
                                   #_[:small.has-text-grey (@s-piece :mtime)]
                                   [:div.content.mt-5
                                    {:dangerouslySetInnerHTML
                                     {:__html (@s-piece :content-parsed)}}]])}))


(def routes
  [["/pieces" {:name ::pieces
               :view inout-list-component}]
   ["/main" {:name ::main
             :view buckets-component}]
   ["/piece/:id" {:name       ::piece-one
                  :parameters {:path {:id string?}}
                  :view       bucket-list-component}]])


(defn page-template []
      [:div.container.p-3
       [:header
        [:div.columns
         [:div.column.is-one-fifth
          [:header.mb-5
           [:figure.image
            [:img {:src "assets/roomel_coffee.jpg"}]]]]
         [:div.column.has-text-right
          [:p.title "tbuck"]
          [:p.span "total xxxx,xxx,xxx"]
          [:p.span "마지막 입출금 2024-06-22"]]]]


       [:div.columns
        [:div.column.is-one-fifth
         [inout-list-component]]
        [:div.column
         [:section.section.mt-0.pt-0
          [:h5.subtitle.mb-2 "여행"]
          [:h5.subtitle.mb-2 "주담대"]
          [:h5.subtitle.mb-2 "비상금"]
          [:h5.subtitle.mb-2 "여유"]
          (if @match
            (let [view (:view (:data @match))]
                 [view @match])
            [buckets-component])]]]

       ;[:pre (with-out-str (cljs.pprint/pprint @match))]
       [:footer
        [:p [:span.icon-text
             [:span.icon [:i.fas.fa-copyright]]
             [:span "Mel family"]]]]])



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