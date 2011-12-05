(ns start.api
  (:use [compojure.core :only (defroutes POST)]))

(defonce next-id (atom 0))

(defonce ^:dynamic database (atom #{}))

(defmulti remote :fn)

(defmethod remote :default [data]
  {:status :error :message "Unknown endpoint."})

(defmethod remote :add-name [data]
  (let [n (-> data :args :name)
        response {:exists (contains? @database n)}]
    (swap! database conj n)
    response))

(defroutes remote-routes
  (POST "/remote" {{data "data"} :params}
        (pr-str
         (remote
          (binding [*read-eval* false]
            (read-string data))))))
