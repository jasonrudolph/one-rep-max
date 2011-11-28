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
        [cljs.closure :only (build)]
        [cljs.repl :only (repl)]
        [cljs.repl.browser :only (repl-env)]
        [start.api :only (remote-routes)])
  (:require [net.cgrand.enlive-html :as html])
  (:import java.io.File))

(def script-snippet (html/html-snippet "<script type='text/javascript'></script>"))

(defn script [f] (html/transform script-snippet [:script] f))

(defn application-view [& scripts]
  (html/transform (html/html-resource "application.html")
                  [:body]
                  (apply html/append scripts)))

(defn design-view [params]
  (file-response (str (get params "page") ".html") {:root "templates"}))

(defn application-host [request]
  (html/emit*
   (if (= (:uri request) "/development")
     (application-view (script (html/set-attr :src "javascripts/out/goog/base.js"))
                       (script (html/set-attr :src "javascripts/main.js"))
                       (script (html/content "goog.require('start.core');"))
                       (script (html/content "goog.require('start.model');"))
                       (script (html/content "start.core.start();start.core.repl();")))
     (application-view (script (html/set-attr :src "javascripts/main.js"))
                       (script (html/content "start.core.start();"))))))

(defroutes app-routes
  remote-routes
  (GET "/development" request (application-host request))
  (GET "/production" request (application-host request))
  (GET "/design" {params :params} (design-view params))
  (ANY "*" [] (file-response "404.html" {:root "public"})))

(defn watch-cljs [handler dir opts]
  (fn [request]
    (let [out (:output-dir opts)]
      ;; Always recompile project files
      (doseq [file (file-seq (File. "public/javascripts/out/start"))]
        (.setLastModified file 0))
      (build dir (if (= (:uri request) "/production")
                   (assoc opts :optimizations :advanced)
                   opts))
      (handler request))))

(defn ensure-encoding [handler]
  (fn [request]
    (let [{:keys [headers body] :as response} (handler request)]
      (if (and (= (get headers "Content-Type") "text/javascript")
               (= (type body) java.io.File))
        (assoc-in response [:headers "Content-Type"]
                  "text/javascript; charset=utf-8")
        response))))

(defn reload-clj [handler files]
  (fn [request]
    (let [ns (ns-name *ns*)]
      (apply load files))
    (handler request)))

(def app (-> app-routes
             (watch-cljs "src/cljs" {:output-to "public/javascripts/main.js"
                                     :output-dir "public/javascripts/out"})
             (wrap-file "public")
             wrap-file-info
             ensure-encoding
             wrap-params
             wrap-stacktrace
             #_(reload-clj ["/start/templates"
                          "/start/api"
                          "/start/core"])))

(defn run-server [] (run-jetty (var app) {:join? false :port 8080}))

(defn cljs-repl [] (repl (repl-env)))

(comment
  ;; Start the server.
  (use 'start.core :reload-all)
  (run-server)
  ;; Start a REPL.
  (cljs-repl))

 
