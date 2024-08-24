(ns tbuck.clj.main
  (:require [tbuck.clj.server :as server]
            [tbuck.clj.terminal :as terminal]))


(defn -main [& [mode]]
  (if (= "terminal" mode)
    (terminal/start)
    (server/start)))



