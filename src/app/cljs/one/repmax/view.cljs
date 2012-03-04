(ns ^{:doc "Render the views for the application."}
  one.repmax.view
  (:use [domina.xpath :only (xpath)])
  (:require-macros [one.repmax.snippets :as snippets])
  (:require [one.dispatch :as dispatch]
            [domina :as d]))

(def ^{:doc "A map which contains chunks of HTML which may be used
  when rendering views."}
  snippets (snippets/snippets))

(defn- render-exercise-list [exercises]
  (let [content (xpath "//div[@id='exercise-list']/ol")]
    (d/destroy-children! content)
    (doseq [e exercises]
      (d/append! content (exercise-list-item e)))))

(defn- exercise-list-item [exercise]
  (let [li (d/clone (:exercises-list-item snippets))
        dom-id (str "exercise-" (:_id exercise))]
    (-> li
      (d/set-attr! "id" dom-id))
    (-> li (xpath "a")
      (d/set-attr! "href" (str "#")))
    (-> li (xpath ".//span[@class='list-item-label']")
      (d/set-text! (:name exercise)))
    li))

(dispatch/react-to #{:exercise-search-results-ready}
                   (fn [_ exercises] (render-exercise-list exercises)))

(defmulti render
  "Accepts a map which represents the current state of the application
  and renders a view based on the value of the `:state` key."
  :state)

(defmethod render :exercise-list [_]
  (let [header (d/by-id "header")
        content (d/by-id "content")]
    (d/swap-content! header (:exercises-header snippets))
    (d/set-html! content (:exercises-search snippets))
    (d/append! content (:exercises-list snippets))))

(dispatch/react-to #{:state-change} (fn [_ m] (render m)))
