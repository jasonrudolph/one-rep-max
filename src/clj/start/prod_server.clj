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

(defn wrap-require-auth
  [app]
  (fn [req]
    (let [denied-response {:headers {"WWW-Authenticate" "Basic realm=\"Restricted\""}
                           :body "HTTP authentication required."
                           :status 401}]
      (if-let [auth ((:headers req) "authorization")]
        (if (= auth "Basic Y3Nzazppc2F3ZXNvbWU=")
          ;; username=cssk,password=isawesome
          (app req)
          denied-response)
        denied-response))))

(defroutes app-routes
  remote-routes
  (-> (ANY "*" request (file-response "404.html" {:root root}))
      (wrap-file root)
      wrap-file-info
      wrap-require-auth))

(def app (-> app-routes
             wrap-params))

(defn -main [] (let [port (Integer/parseInt (get (System/getenv) "PORT" "8080"))]
                 (run-jetty (var app) {:join? false :port port})))
