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
        [start.templates :only (load-html construct-html)])
  (:require [net.cgrand.enlive-html :as html]
            [start.reload :as reload])
  (:import java.io.File))

(def script-snippet (html/html-snippet "<script type='text/javascript'></script>"))

(defn script [f] (html/transform script-snippet [:script] f))

(defn application-view [& scripts]
  (html/transform (construct-html (html/html-resource "application.html"))
                  [:body]
                  (apply html/append scripts)))

(defn application-host [request]
  (let [body (html/emit*
              (if (= (:uri request) "/development")
                (application-view (script (html/set-attr :src "javascripts/out/goog/base.js"))
                                  (script (html/set-attr :src "javascripts/main.js"))
                                  (script (html/content "goog.require('start.core');"))
                                  (script (html/content "goog.require('start.model');"))
                                  (script (html/content "start.core.start();start.core.repl();")))
                (application-view (script (html/set-attr :src "javascripts/mainp.js"))
                                  (script (html/content "start.core.start();")))))]
    {:status 200
     :headers {"Content-Type" "text/html; charset=utf-8"}
     :body body}))

(defn design-view [params]
  (load-html (str (get params "page") ".html")))

(defroutes app-routes
  remote-routes
  (GET "/development" request (application-host request))
  (GET "/production" request (application-host request) )
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

(defn apply-templates [handler]
  (fn [request]
    (let [{:keys [headers body] :as response} (handler request)]
      (if (and (= (type body) File)
               (.endsWith (.getName body) ".html"))
        (let [new-body (html/emit* (construct-html (html/html-snippet (slurp body))))]
          {:status 200
           :headers {"Content-Type" "text/html; charset=utf-8"}
           :body new-body})
        response))))

(def app (-> app-routes
             (reload/watch-cljs "src/cljs" {:output-to "public/javascripts/main.js"
                                            :output-to-prod "public/javascripts/mainp.js"
                                            :output-dir "public/javascripts/out"
                                            :top-level-package "start"})
             (wrap-file "public")
             wrap-file-info
             apply-templates
             ensure-encoding
             wrap-params
             wrap-stacktrace
             (reload/reload-clj ["/start/templates"
                                 "/start/api"
                                 "/start/core"])))

(defn run-server [] (run-jetty (var app) {:join? false :port 8080}))

(defn cljs-repl [] (repl (repl-env)))

(comment
  ;; Start the server.
  (use 'start.core :reload-all)
  (run-server)
  ;; Start a REPL.
  (cljs-repl)
  )

 
