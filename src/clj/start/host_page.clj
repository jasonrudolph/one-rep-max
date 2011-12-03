(ns start.host-page
  "Create an HTML page to host a ClojureScript application."
  (:use [start.templates :only (construct-html)]
        [start.snippets :only (render)])
  (:require [net.cgrand.enlive-html :as html]))

(def script-snippet (html/html-snippet "<script type='text/javascript'></script>"))

(defn- script [f] (html/transform script-snippet [:script] f))

(defn- application-view [& scripts]
  (html/transform (construct-html (html/html-resource "application.html"))
                  [:body]
                  (apply html/append scripts)))

(defn application-host [config request]
  (render
   (if (= (:uri request) "/development")
     (apply application-view (script (html/set-attr :src "javascripts/out/goog/base.js"))
            (script (html/set-attr :src "javascripts/main.js"))
            (map #(script (html/content %)) (:dev-js config)))
     (apply application-view (script (html/set-attr :src (str "javascripts/"
                                                              (:prod-js-file-name config))))
            (map #(script (html/content %)) (:prod-js config))))))
