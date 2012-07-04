(ns one.repmax.exercises.view
  (:require-macros [one.repmax.snippets :as snippets])
  (:require [clojure.browser.event :as event]
            [domina :as d]
            [domina.css :as css]
            [goog.events.EventType :as event-type]
            [one.dispatch :as dispatch]
            [one.repmax.fx :as fx]))

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

(defn- render-exercise-list [exercises]
  (let [content (css/sel "#exercise-list ol")]
    (d/destroy-children! content)
    (doseq [e exercises]
      (d/append! content (exercise-list-item e)))))

(defn- exercise-list-item [exercise]
  (let [li (d/clone (:exercises-list-item snippets))
        exercise-id (:_id exercise)]
    (-> li
      (d/set-attr! "id" (exercise-dom-id exercise-id)))
    (-> li (css/sel "span.list-item-label")
      (d/set-text! (:name exercise)))
    (-> li (d/single-node)
      (add-exercise-click-event-listener exercise-id))
    li))

(defn- add-exercise-click-event-listener [node exercise-id]
  (event/listen node
                event-type/CLICK
                #(dispatch/fire :action {:action :new-set/new, :exercise-id exercise-id})))

(defn- add-exercise-search-event-listener []
  (let [field (d/by-id "search-input")]
    (event/listen field
                  event-type/KEYUP
                  #(dispatch/fire :action {:action :exercises/search, :name (d/value field)}))))

(defn- render-filtered-exercise-list [exercise-ids]
  (fx/hide (css/sel "#exercise-list ol li"))
  (doseq [id exercise-ids]
    (fx/show (d/by-id (exercise-dom-id id)))))

(defn- exercise-dom-id [exercise-id]
  (str "exercise-" exercise-id))

;;; Register reactors

(dispatch/react-to #{:model-change}
                   (fn [_ event] (render event)))

