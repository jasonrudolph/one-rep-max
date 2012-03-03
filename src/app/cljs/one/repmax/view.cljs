(ns ^{:doc "Render the views for the application."}
  one.repmax.view
  (:use [domina :only (append! by-id set-html! swap-content!)])
  (:require-macros [one.repmax.snippets :as snippets])
  (:require [one.dispatch :as dispatch]))

(def ^{:doc "A map which contains chunks of HTML which may be used
  when rendering views."}
  snippets (snippets/snippets))

(defn render-exercise-list [exercises]
  (js/alert "Hello! I have a list of exercises ready for you. Replace me with logic to render the list.")
  (.log js/console exercises))

(dispatch/react-to #{:exercise-search-results-ready}
                   (fn [_ exercises] (render-exercise-list exercises)))

(defmulti render
  "Accepts a map which represents the current state of the application
  and renders a view based on the value of the `:state` key."
  :state)

(defmethod render :exercise-list [_]
  (let [header (by-id "header")
        content (by-id "content")]
    (swap-content! header (:exercises-header snippets))
    (set-html! content (:exercises-search snippets))
    (append! content (:exercises-list snippets))))

(dispatch/react-to #{:state-change} (fn [_ m] (render m)))
