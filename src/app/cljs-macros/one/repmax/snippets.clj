(ns one.repmax.snippets
  "Macros for including HTML snippets in the ClojureScript application
  at compile time."
  (:use [one.templates :only (render)])
  (:require [net.cgrand.enlive-html :as html]))

(defn- snippet [file id]
  (render (html/select (html/html-resource file) id)))

(def ^{:private true}
  exercises-list-template
  (let [list-with-sample-items (html/select (html/html-resource "exercises.html") [:#exercise-list])
        empty-list (html/at list-with-sample-items [:div :ol] (html/content ""))]
    (render empty-list)))

(def ^{:private true}
  exercises-list-item-template
  (let [sample-list-item (html/select (html/html-resource "exercises.html") [:#exercise-list :ol [:li html/first-of-type]])
        list-item-without-content (html/at sample-list-item [:.list-item-label] (html/content ""))]
    (render list-item-without-content)))

(defmacro snippets
  "Expands to a map of HTML snippets which are extracted from the
  design templates."
  []
  {:exercises-header    (snippet "exercises.html" [:#header])
   :exercises-search    (snippet "exercises.html" [:#search-bar])
   :exercises-list      exercises-list-template
   :exercises-list-item exercises-list-item-template})
