(ns one.repmax.sets.view
  (:require-macros [one.repmax.snippets :as snippets])
  (:require [clojure.browser.event :as event]
            [domina :as d]
            [domina.css :as css]
            [one.dispatch :as dispatch]))

(def snippets (snippets/snippets))

(defmulti render (fn [{:keys [message]}] (:action message)))

(defmethod render :default [event])

(defmethod render :new-set/new [{:keys [new]}]
  (let [exercise (-> new :new-set :exercise)
        header (d/by-id "header")
        content (d/by-id "content")]
    (d/swap-content! header (:new-set-header snippets))
    (d/set-text! (css/sel "#header h1") (:name exercise))
    (d/set-html! content (:new-set-form snippets))
    (d/set-value! (css/sel "#exercise-id") (:_id exercise))
    (add-event-listener-for-persisting-set)))

(defn- add-event-listener-for-persisting-set []
  (event/listen (d/by-id "new-set-form-button")
                "click"
                #(dispatch/fire :action {:action :new-set/create
                                         :exercise-id (d/value (d/by-id "exercise-id"))
                                         :weight (d/value (d/by-id "weight-input"))
                                         :reps (d/value (d/by-id "reps-input"))})))

;;; Register reactors

(dispatch/react-to #{:model-change}
                   (fn [_ event] (render event)))

