(ns start.snippets
  "Macros for including html snippets at compile time. This technique
   allows designers to work in whatever way they feel comfortable."
  (:use [library.host-page :only (render)])
  (:require [net.cgrand.enlive-html :as html]))

(defn snippet [file id]
  (render (html/select (html/html-resource file) id)))

(defmacro snippets []
  {:form (snippet "form.html" [:div#content])
   :greeting (snippet "greeting.html" [:div#content])})
