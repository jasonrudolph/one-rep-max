(ns script.serve
  (:require [one.sample.prod-server :as prod]))

(defn -main []
  (prod/run-server))
