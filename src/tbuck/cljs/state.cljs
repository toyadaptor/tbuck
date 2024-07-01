(ns tbuck.cljs.state
  (:require [clojure.string :as str]
            [clojure.edn :as edn]
            [cljs-time.core :as time]
            [cljs-time.format :as timef]
            [reagent.core :refer [atom]]))


(defonce s-main (atom {}))
(defonce s-pieces (atom []))
(defonce s-tong-inouts (atom {}))


(def custom-formatter (timef/formatter "yyyy-MM-dd'T'hh:mm:ss'Z"))
(def knot-time-format (timef/formatter "yyyy.MM.dd hh:mm:ss"))



(defn o2o [subject]
      (-> subject
          (str/replace #"0" "o")
          (str/replace #"^.*?/" "")))

(defn add-comma [num]
      (.toLocaleString num))

(defn set-main [{:keys [tong-name tong-amount is-valid-sum last-inout buckets]}]
      (reset! s-main {:tong-name tong-name
                      :tong-amount (str (add-comma tong-amount) " 원")
                      :is-valid-sum is-valid-sum
                      :last-inout last-inout
                      :buckets (->> buckets
                                    (map #(merge % {:amount (str (add-comma (:amount %)) " 원")})))}))

(defn set-tong-inouts [{:keys [inouts]}]
      (reset! s-tong-inouts {:inouts inouts}))

(defn set-pieces [res-data]
      (reset! s-pieces
              (map #(assoc % :subject (-> (% :subject) o2o)) res-data)))


