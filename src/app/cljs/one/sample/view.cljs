(ns ^{:doc "Render the views for the application."}
  one.sample.view
  (:require-macros [one.sample.snippets :as snippets])
  (:require [clojure.browser.dom :as dom]
            [clojure.browser.event :as event]
            [one.dispatch :as dispatch]
            [goog.dom.classes :as gclasses]
            [goog.events.EventType :as event-type]))

(defn on-click
  "Helper function for adding click listeners to DOM elements.

  Accepts an id (the id of the DOM element to listen to), an
  event-id (the name of the event to fire when the DOM element is
  clicked) and an optional data object to pass along with the fired
  event."
  ([id event-id]
     (on-click id event-id nil))
  ([id event-id d]
     (event/listen-once (dom/get-element id)
                        "click"
                        #(dispatch/fire event-id (if (fn? d) (d) d)))))

(def ^{:doc "A map which contains chunks of HTML which may be used
  when rendering views."}
  snippets (snippets/snippets))

(defmulti render
  "Accepts a map which represents the current state of the application
  and renders a view based on the value of the :state key."
  :state)

(defmethod render :form [{:keys [state error name]}]
  (dom/replace-node :content
                    (dom/html->dom (get snippets state)))
  (when error (do (dom/set-text :name-input-error error)
                  (gclasses/add (dom/get-element :input-field) "error")))
  (when name (dom/set-value :name-input name))
  (.focus (dom/get-element :name-input) ())
  (on-click :greet-button :greeting #(hash-map :name (dom/get-value :name-input)))
  (event/listen (dom/get-element :input-field)
                event-type/CHANGE
                #(dispatch/fire :greeting {:name (dom/get-value :name-input)})))

(defmethod render :greeting [{:keys [state name exists]}]
  (dom/replace-node :content
                    (dom/html->dom (get snippets state)))
  (dom/set-text :name (if exists (str " again " name) name))
  (on-click :content :form))

(dispatch/react-to #{:state-change} (fn [_ m] (render m)))
