(ns start.prod-server
  "Production server serves the backend API. This is only required if
   there is a back end API."
  (:use [ring.adapter.jetty :only (run-jetty)]
        [ring.middleware.file :only (wrap-file)]
        [ring.middleware.file-info :only (wrap-file-info)]
        [ring.middleware.params :only (wrap-params)]
        [ring.util.response :only (file-response)]
        [compojure.core :only (defroutes ANY)]
        [start.api :only (remote-routes)]))

(def root "out/public")

(defroutes app-routes
  remote-routes
  (ANY "*" request (file-response "404.html" {:root root})))

(def app (-> app-routes
             (wrap-file root)
             wrap-file-info
             wrap-params))

(defn -main [] (run-jetty (var app) {:join? false :port 8080}))
