(ns tbuck.cljs.util
  (:require [clojure.string :as str]
            [clojure.edn :as edn]
            [cljs-time.core :as time]
            [cljs-time.format :as timef]))



(def custom-formatter (timef/formatter "yyyy-MM-dd'T'hh:mm:ss'Z"))
(def knot-time-format (timef/formatter "yyyy.MM.dd hh:mm:ss"))


(defn o2o [subject]
      (-> subject
          (str/replace #"0" "o")
          (str/replace #"^.*?/" "")))

(defn add-comma [num]
      (if num
        (.toLocaleString num)
        "-"))



(defn today-in-yyyymmdd []
      (let [today (time/today)
            formatter (timef/formatter "yyyyMMdd")]
           (timef/unparse formatter today)))