(ns start.api
  (:use [compojure.core :only (defroutes POST)]))

(def next-id (atom 0))

(def database (atom {}))

(add-watch database :print-changes
           (fn [k r o n]
             (prn n)))

(defmulti remote :fn)

(defmethod remote :default [data]
  {:status :error :message "Unknown endpoint."})

(defroutes remote-routes
  (POST "/remote" {{data "data"} :params} (pr-str (remote (read-string data)))))
