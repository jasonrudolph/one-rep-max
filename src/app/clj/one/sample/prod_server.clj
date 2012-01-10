(ns one.sample.prod-server
  "Production server serves the backend API. This is only required if
   there is a back end API."
  (:use [ring.adapter.jetty :only (run-jetty)]
        [ring.middleware.file :only (wrap-file)]
        [ring.middleware.file-info :only (wrap-file-info)]
        [ring.middleware.params :only (wrap-params)]
        [ring.util.response :only (file-response)]
        [compojure.core :only (defroutes ANY)]
        [one.sample.api :only (remote-routes)]))

(def ^:private root "out/public")

(defroutes app-routes
  remote-routes
  (-> (ANY "*" request (file-response "404.html" {:root root}))
      (wrap-file root)
      wrap-file-info))

(def ^:private app (-> app-routes
                       wrap-params))

(defn -main [] (let [port (Integer/parseInt (get (System/getenv) "PORT" "8080"))]
                 (run-jetty (var app) {:join? false :port port})))
