(ns one.sample.dev-server
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
        [one.templates :only (load-html apply-templates render)]
        [one.host-page :only (application-host)]
        [one.sample.api :only (remote-routes)]
        [one.sample.config])
  (:require [net.cgrand.enlive-html :as html]
            [one.reload :as reload])
  (:import java.io.File))

(defn- environment [uri]
  (if (= uri "/development") :development :production))

(defn- make-host-page [request]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (application-host config (environment (:uri request)))})

(defroutes app-routes
  remote-routes
  (GET "/development" request (make-host-page request))
  (GET "/production" request (make-host-page request) )
  (GET "/design*" {{file :*} :route-params}
       (when (.endsWith file ".html")
         (load-html (.substring file 1))))
  (ANY "*" request (file-response "404.html" {:root "public"})))

(defn- js-encoding [handler]
  (fn [request]
    (let [{:keys [headers body] :as response} (handler request)]
      (if (and (= (get headers "Content-Type") "text/javascript")
               (= (type body) File))
        (assoc-in response [:headers "Content-Type"]
                  "text/javascript; charset=utf-8")
        response))))

(defn- rewrite-design-uris [handler]
  (fn [{:keys [uri] :as request}]
    (if (some true? (map #(.startsWith uri (str "/design/" %))
                         ["css" "javascripts" "images" "js" "favicon.ico"]))
      (handler (assoc request :uri (.substring uri 7)))
      (handler request))))

;; We need to use this instead of Enlive's html-snippet, because
;; html-snippet throws away the doctype
(defn- html-parse
  "Parse a string into a seq of Enlive nodes."
  [s]
  (html/html-resource (java.io.StringReader. s)))

(defn- active-menu-transform
  "Accepts the selected menu (a keyword) and the response and returns
  an updated response body with the correct menu activated."
  [menu response]
  (assoc response
    :body (render (html/transform (html-parse (:body response))
                                  [:ul#navigation (keyword (str "li." (name menu)))]
                                  (html/add-class "active")))))

(defn- set-active-menu
  "Middleware which will highlight the current active menu item."
  [handler]
  (fn [request]
    (let [response (handler request)
          uri (:uri request)]
      (cond (= uri "/") (active-menu-transform :home response)
            (and (.startsWith uri "/design") (.endsWith uri ".html")) (active-menu-transform :design response)
            (= uri "/development") (active-menu-transform :development response)
            (= uri "/production") (active-menu-transform :production response)
            :else response))))

(def ^:private app (-> app-routes
                       (reload/watch-cljs config)
                       (wrap-file "public")
                       rewrite-design-uris
                       wrap-file-info
                       apply-templates
                       js-encoding
                       wrap-params
                       set-active-menu
                       wrap-stacktrace
                       (reload/reload-clj (:reload-clj config))))

(defn run-server
  "Start the development server on port 8080."
  []
  (run-jetty (var app) {:join? false :port 8080}))
