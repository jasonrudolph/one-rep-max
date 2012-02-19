(ns one.host-page
  "Functions to create an HTML page that hosts a ClojureScript
  application."
  (:use [one.core :only (*configuration*)]
        [one.templates :only (load-html construct-html render)]
        [compojure.core :only (defroutes GET)])
  (:require [net.cgrand.enlive-html :as html]))

(def ^:private script-snippet
  (html/html-snippet "<script type='text/javascript'></script>"))

(defn- script
  [f]
  (html/transform script-snippet [:script] f))

(defn- application-view
  [& scripts]
  (html/transform (construct-html (html/html-resource "application.html"))
                  [:body]
                  (apply html/append scripts)))

(defn application-host
  "Given a configuration map and an environment, return HTML (as a
  string) that can host a ClojureScript application. The environment
  must be either `:development` or `:production` - any other value results
  in an exception. The generated HTML is based on the contents of
  application.html, which is loaded as an Enlive resource.

  In production mode, the HTML (as a sequence of Enlive nodes) is
  transformed via the `:prod-transform` function from the config map.

  This function is normally called in two situations:

  1. From a Ring application to dynamically generate the application
     HTML.

  2. From the build script to create static deployment artifacts."
  [environment]
  (render
   (case environment
     :development
     (apply application-view (script (html/set-attr :src "javascripts/out/goog/base.js"))
            (script (html/set-attr :src "javascripts/main.js"))
            (map #(script (html/content %)) (:dev-js *configuration*)))

     :production
     (let [tfn (get *configuration* :prod-transform identity)]
       (tfn (apply application-view
                   (script (html/set-attr :src (str "javascripts/"
                                                    (:prod-js-file-name *configuration*))))
                   (map #(script (html/content %)) (:prod-js *configuration*)))))

     :fresh
     (apply application-view (script (html/set-attr :src "javascripts/out/goog/base.js"))
            (script (html/set-attr :src "javascripts/fresh.js"))
            (script (html/content "goog.require('one.browser.repl_client');"))
            (script (html/content "one.browser.repl_client.repl();"))))))

(defn- environment [uri]
  (case uri
    "/development" :development
    "/production" :production
    "/fresh" :fresh))

(defn- make-bare-page [body]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body body})

(defn make-host-page [request]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (application-host (environment (:uri request)))})

(defn- serve-design-file [file-path]
  (when (.endsWith file-path ".html")
    (make-bare-page (load-html (.substring file-path 1)))))

(defroutes default-one-routes
  (GET "/development" request (make-host-page request))
  (GET "/production" request (make-host-page request))
  (GET "/fresh" request (make-host-page request))
  (GET "/design*" {{file :*} :route-params} (serve-design-file file)))
