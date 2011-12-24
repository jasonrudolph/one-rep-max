(ns one.host-page
  "Functions to create an HTML page that hosts a ClojureScript
  application."
  (:use [one.templates :only (construct-html render)])
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
  [config environment]
  (render
   (case environment
     :development
     (apply application-view (script (html/set-attr :src "javascripts/out/goog/base.js"))
            (script (html/set-attr :src "javascripts/main.js"))
            (map #(script (html/content %)) (:dev-js config)))

     :production
     (let [tfn (get config :prod-transform identity)]
       (tfn (apply application-view
                   (script (html/set-attr :src (str "javascripts/"
                                                    (:prod-js-file-name config))))
                   (map #(script (html/content %)) (:prod-js config))))))))
