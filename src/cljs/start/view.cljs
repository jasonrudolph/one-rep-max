(ns start.view
  (:require-macros [start.snippets :as snippets])
  (:require [clojure.browser.dom :as dom]
            [clojure.browser.event :as event]
            [library.dispatch :as dispatch]
            [goog.dom.classes :as gclasses]))

(defn on-click
  ([id event-id]
     (on-click id event-id nil))
  ([id event-id d]
     (event/listen-once (dom/get-element id)
                        "click"
                        #(dispatch/fire event-id (if (fn? d) (d) d)))))

(def snippets (snippets/snippets))

(defmulti render :state)

(defmethod render :form [{:keys [state error name]}]
  (dom/replace-node :content
                    (dom/html->dom (get snippets state)))
  (when error (do (dom/set-text :name-input-error error)
                  (gclasses/add (dom/get-element :input-field) "error")))
  (when name (dom/set-value :name-input name))
  (on-click :greet-button :greeting #(hash-map :name (dom/get-value :name-input))))

(defmethod render :greeting [{:keys [state name exists]}]
  (dom/replace-node :content
                    (dom/html->dom (get snippets state)))
  (dom/set-text :name (if exists (str " again " name) name))
  (on-click :content :form))

(dispatch/respond-to :state-change (fn [_ m] (render m)))
