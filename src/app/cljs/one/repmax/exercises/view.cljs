(ns one.repmax.exercises.view
  (:require-macros [one.repmax.snippets :as snippets])
  (:require [clojure.browser.event :as event]
            [domina :as d]
            [domina.css :as css]
            [goog.style :as style]
            [one.dispatch :as dispatch]))

(def snippets (snippets/snippets))

(defmulti render (fn [{:keys [message]}] (:action message)))

(defmethod render :default [event])

(defmethod render :datastore-configuration/ready [_]
  (let [header (d/by-id "header")
        content (d/by-id "content")]
    (d/swap-content! header (:exercises-header snippets))
    (d/set-html! content (:exercises-search snippets))
    (d/append! content (:exercises-list snippets))
    (add-exercise-search-event-listener)))

(defmethod render :exercises/initialized-from-datastore [{:keys [message]}]
  (render-exercise-list (:exercises message)))

(defmethod render :exercises/search [{:keys [new]}]
  (render-filtered-exercise-list (-> new :exercise-search :exercise-ids)))

;; TODO Time this approach (i.e., rebuilding the whole list in a "scorched Earth" fashion)
;;      versus hiding/showing *existing* list items after a search.
(defn- render-exercise-list [exercises]
  (let [content (css/sel "#exercise-list ol")]
    (d/destroy-children! content)
    (doseq [e exercises]
      (d/append! content (exercise-list-item e)))))

(defn- exercise-list-item [exercise]
  (let [li (d/clone (:exercises-list-item snippets))]
    (-> li
      (d/set-attr! "id" (exercise-dom-id (:_id exercise))))
    (-> li (css/sel "a")
      (d/set-attr! "href" (str "#")))
    (-> li (css/sel "span.list-item-label")
      (d/set-text! (:name exercise)))
    li))

(defn- add-exercise-search-event-listener []
  (let [field (d/by-id "search-input")]
    (event/listen field
                  "keyup"
                  #(dispatch/fire :action {:action :exercises/search, :name (d/value field)}))))

(defn- render-filtered-exercise-list [exercise-ids]
  (d/set-style! (css/sel "#exercise-list ol li") "display" "none")
  (doseq [id exercise-ids]
    (d/set-style! (d/by-id (exercise-dom-id id)) "display" nil)))

(defn- exercise-dom-id [exercise-id]
  (str "exercise-" exercise-id))

;;; Register reactors

(dispatch/react-to #{:model-change}
                   (fn [_ event] (render event)))

