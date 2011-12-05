(ns start.dev-server
  "Serve a friendly ClojureScript environment with code reloading and
   the ClojureScript application in both development and advanced
   compiled mode."
  (:use [ring.adapter.jetty :only (run-jetty)]
        [ring.middleware.file :only (wrap-file)]
        [ring.middleware.file-info :only (wrap-file-info)]
        [ring.middleware.params :only (wrap-params)]
        [ring.middleware.stacktrace :only (wrap-stacktrace)]
        [ring.util.response :only (file-response)]
        [compojure.core :only (defroutes GET POST ANY)]
        [cljs.repl :only (repl)]
        [cljs.repl.browser :only (repl-env)]
        [library.templates :only (load-html apply-templates)]
        [library.host-page :only (application-host)]
        [start.api :only (remote-routes)]
        [start.config])
  (:require [net.cgrand.enlive-html :as html]
            [library.reload :as reload])
  (:import java.io.File))

(defn make-host-page [request]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (application-host config request)})

(defroutes app-routes
  remote-routes
  (GET "/development" request (make-host-page request))
  (GET "/production" request (make-host-page request) )
  (GET "/design*" {{file :*} :route-params} (load-html (.substring file 1)))
  (ANY "*" request (file-response "404.html" {:root "public"})))

(defn js-encoding [handler]
  (fn [request]
    (let [{:keys [headers body] :as response} (handler request)]
      (if (and (= (get headers "Content-Type") "text/javascript")
               (= (type body) File))
        (assoc-in response [:headers "Content-Type"]
                  "text/javascript; charset=utf-8")
        response))))

(defn rewrite-design-uris [handler]
  (fn [{:keys [uri] :as request}]
    (if (or (.startsWith uri "/design/css")
            (.startsWith uri "/design/javascripts")
            (.startsWith uri "/design/images"))
      (handler (assoc request :uri (.substring uri 7)))
      (handler request))))

(def app (-> app-routes
             (reload/watch-cljs "src/cljs" config)
             (wrap-file "public")
             rewrite-design-uris
             wrap-file-info
             apply-templates
             js-encoding
             wrap-params
             wrap-stacktrace
             (reload/reload-clj (:reload-clj config))))

(defn run-server [] (run-jetty (var app) {:join? false :port 8080}))

(defn cljs-repl [] (repl (repl-env)))

(comment
  ;; Start the server.
  (use 'start.dev-server :reload-all)
  (run-server)
  ;; Start a REPL.
  (cljs-repl)
  )
