(ns tbuck.clj.main
  (:require [tbuck.clj.server :as server]
            [tbuck.clj.terminal :as terminal])
  (:gen-class))


(defn -main [& [mode]]
  (if (= "terminal" mode)
    (terminal/start)
    (server/start)))



