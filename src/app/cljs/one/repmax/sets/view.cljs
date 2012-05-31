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
    (d/append! content (:recent-set-history-list snippets))
    (add-event-listener-for-persisting-set)))

(defmethod render :new-set/persisted [{:keys [message]}]
  (let [exercise-set (:set message)
        set-list (css/sel "#recent-set-history ol")
        set-number (-> set-list (css/sel "li") d/nodes count inc)
        new-list-item (set-list-item (assoc exercise-set :number set-number))]
    (d/append! set-list new-list-item)))

(defn- set-list-item [exercise-set]
  (let [li (d/clone (:recent-set-history-list-item snippets))]
    (-> li (css/sel ".value.weight") (d/set-text! (:weight exercise-set)))
    (-> li (css/sel ".value.reps") (d/set-text! (:reps exercise-set)))
    (-> li (css/sel ".set-number .value") (d/set-text! (:number exercise-set)))
    li))

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

