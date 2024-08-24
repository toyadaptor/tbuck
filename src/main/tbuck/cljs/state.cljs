(ns tbuck.cljs.state
  (:require [clojure.string :as str]
            [clojure.edn :as edn]
            [cljs-time.core :as time]
            [cljs-time.format :as timef]
            [reagent.core :refer [atom]]
            [tbuck.cljs.util :refer [add-comma]]))


(defonce s-main (atom {}))
(defonce s-tong-inouts (atom {}))

(defonce s-buckets (atom []))
(defonce s-bucket-divides (atom {}))

(defonce s-inout-divides (atom {}))

(defonce s-divide-new-ready (atom {}))









(defn set-main [{:keys [tong-name tong-amount is-valid-sum last-inout buckets]}]
      (reset! s-main {:tong-name    tong-name
                      :tong-amount  (str (add-comma tong-amount) " 원")
                      :is-valid-sum is-valid-sum
                      :last-inout   last-inout
                      :buckets      (->> buckets
                                         (map #(merge % {:amount (str (add-comma (:amount %)) " 원")})))}))

(defn set-tong-inouts [{:keys [inouts]}]
      (reset! s-tong-inouts {:inouts (->> inouts
                                          (map #(merge % {:amount (if (= 0 (:amount %))
                                                                    "#조정#"
                                                                    (str (add-comma (:amount %)) " 원"))})))}))


(defn set-buckets [{:keys [buckets]}]
      (reset! s-buckets buckets))


(defn set-bucket-divides [{:keys [bucket divides]}]
      (reset! s-bucket-divides {:bucket  (merge bucket {:amount (str (add-comma (:amount bucket)) " 원")})
                                :divides (->> divides
                                              (map #(merge % {:amount (str (add-comma (:amount %)) " 원")})))}))


(defn set-inouts-detail [{:keys [inout divides]}]
      (reset! s-inout-divides {:inout   (merge inout {:amount (if (= 0 (:amount inout))
                                                                "#조정#"
                                                                (str (add-comma (:amount inout)) " 원"))})
                               :divides (->> divides
                                             (map #(merge % {:amount (str (add-comma (:amount %)) " 원")})))}))
(defn set-divides-detail [res]
      (reset! s-inout-divides res))


(defn set-divide-new-ready [{:keys [inout buckets]}]
      (reset! s-divide-new-ready {:inout   (merge inout {:amount (js/parseInt (:amount inout))})
                                  :buckets (into {}
                                                 (map (fn [[k v]] {k (assoc v :amount (js/parseInt (:amount v)))})
                                                      buckets))}))
