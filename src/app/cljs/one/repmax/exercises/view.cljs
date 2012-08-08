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

;; TODO Replace the multimethods for :datastore-configuration/ready,
;; :new-exercise/back, and :new-set/back with a single fn that fires when the
;; state changes to :exercises. In all cases, we render the "exercise list"
;; view with the current contents of :exercises from the state atom.
(defmethod render :datastore-configuration/ready [_]
  (render-exercise-list-view []))

(defmethod render :exercises/initialized-from-datastore [{:keys [new]}]
  (render-exercise-list (:exercises new)))

(defmethod render :exercises/search [{:keys [new]}]
  (render-filtered-exercise-list (-> new :exercise-search :exercise-ids)))

(defmethod render :new-set/back [{:keys [new]}]
  (render-exercise-list-view (:exercises new)))

(defmethod render :new-exercise/new [_]
  (render-new-exercise-view []))

(defmethod render :new-exercise/persisted [{:keys [message]}]
  (render-new-exercise-confirmation (:exercise message)))

(defmethod render :new-exercise/back [{:keys [new]}]
  (render-exercise-list-view (:exercises new)))

;;; Manipulating the "Exercise List" view

(defn- render-exercise-list-view [exercises]
  (let [header (d/by-id "header")
        content (d/by-id "content")]
    (d/swap-content! header (:exercises-header snippets))
    (d/swap-content! content (:exercises-content snippets))
    (add-event-listener-for-creating-new-exercise)
    (add-event-listener-for-exercise-search)
    (render-exercise-list exercises)))

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
      (add-event-listener-for-exercise-click exercise-id))
    li))

(defn- add-event-listener-for-creating-new-exercise []
  (event/listen (d/by-id "new-exercise-button")
                event-type/CLICK
                #(dispatch/fire :action {:action :new-exercise/new})))

(defn- add-event-listener-for-exercise-click [node exercise-id]
  (event/listen node
                event-type/CLICK
                #(dispatch/fire :action {:action :new-set/new, :exercise-id exercise-id})))

(defn- add-event-listener-for-exercise-search []
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

;;; Manipulating the "New Exercise Form" view

(defn- render-new-exercise-view []
  (let [header (d/by-id "header")
        content (d/by-id "content")]
    (d/swap-content! header (:new-exercise-header snippets))
    (d/set-html! content (:new-exercise-form snippets))
    (add-event-listener-for-persisting-exercise)
    (add-event-listener-for-returning-to-exercise-list)))

(defn- render-new-exercise-confirmation [exercise]
  ; TODO Replace with traditional confirmation message
  (js/alert (str "Added \"" (:name exercise) "\"")))

(defn- add-event-listener-for-persisting-exercise []
  (let [field (d/by-id "exercise-name-input")]
  (event/listen (d/by-id "new-exercise-form-button")
                event-type/CLICK
                #(dispatch/fire :action {:action :new-exercise/create, :name (d/value field)}))))

(defn- add-event-listener-for-returning-to-exercise-list []
  (event/listen (d/by-id "back-button")
                event-type/CLICK
                #(dispatch/fire :action {:action :new-exercise/back})))

;;; Register reactors

(dispatch/react-to #{:model-change}
                   (fn [_ event] (render event)))

