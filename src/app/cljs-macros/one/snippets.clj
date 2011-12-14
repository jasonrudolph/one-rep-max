(ns start.snippets
  "Macros for including HTML snippets in the ClojureScript application
  at compile time."
  (:use [library.templates :only (render)])
  (:require [net.cgrand.enlive-html :as html]))

(defn- snippet [file id]
  (render (html/select (html/html-resource file) id)))

(defmacro snippets
  "Expands to a map of HTML snippets which are extracted from the
  design templates."
  []
  {:form (snippet "form.html" [:div#content])
   :greeting (snippet "greeting.html" [:div#content])})
