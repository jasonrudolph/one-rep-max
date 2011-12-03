(ns start.core
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
        [start.api :only (remote-routes)]
        [start.templates :only (load-html apply-templates)]
        [start.host-page :only (application-host)]
        [start.config])
  (:require [net.cgrand.enlive-html :as html]
            [start.reload :as reload])
  (:import java.io.File))

(defn make-host-page [request]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (application-host config request)})

(defn design-view [params]
  (load-html (str (get params "page") ".html")))

(defroutes app-routes
  remote-routes
  (GET "/development" request (make-host-page request))
  (GET "/production" request (make-host-page request) )
  (GET "/design" {params :params} (design-view params))
  (ANY "*" request (file-response "404.html" {:root "public"})))

(defn ensure-encoding [handler]
  (fn [request]
    (let [{:keys [headers body] :as response} (handler request)]
      (if (and (= (get headers "Content-Type") "text/javascript")
               (= (type body) File))
        (assoc-in response [:headers "Content-Type"]
                  "text/javascript; charset=utf-8")
        response))))

(def app (-> app-routes
             (reload/watch-cljs "src/cljs" config)
             (wrap-file "public")
             wrap-file-info
             apply-templates
             ensure-encoding
             wrap-params
             wrap-stacktrace
             (reload/reload-clj (:reload-clj config))))

(defn run-server [] (run-jetty (var app) {:join? false :port 8080}))

(defn cljs-repl [] (repl (repl-env)))

(comment
  ;; Start the server.
  (use 'start.core :reload-all)
  (run-server)
  ;; Start a REPL.
  (cljs-repl)
  )
