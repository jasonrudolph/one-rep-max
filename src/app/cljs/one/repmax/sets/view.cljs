(ns one.repmax.sets.view
  (:require-macros [one.repmax.snippets :as snippets])
  (:refer-clojure :exclude [set])
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
    (d/add-class! content "inset")
    (d/set-html! content (:new-set-form snippets))
    (d/set-value! (css/sel "#exercise-id") (:_id exercise))
    (add-event-listener-for-persisting-set)))

(defmethod render :new-set/persisted [{:keys [message]}]
  (let [set (:set message)
        date (js/Date.) ; TODO Determine/use the :created-at value from the set
        set-list-section (set-history-list-for date)
        set-number (-> set-list-section (css/sel "li") d/nodes count inc)
        new-list-item (set-list-item (assoc set :number set-number))]
    (d/prepend! (css/sel set-list-section "ol")  new-list-item)))

(defn- set-history-list-for [date]
  (let [set-list-id (set-history-list-id-for date)
        existing-set-list (d/by-id set-list-id)]
    (or existing-set-list (create-set-list date))))

(defn- create-set-list [date]
  (let [content (d/by-id "content")
        set-list (d/clone (:set-history-list snippets))
        set-list-id (set-history-list-id-for date)
        set-list-date (css/sel set-list "header time")]
    (d/set-attr! set-list :id set-list-id)
    (d/set-attr! set-list-date "datetime" (iso-date-string date))
    (d/set-text! set-list-date (.toDateString date))
    (d/append! content set-list)
    set-list))

(defn- iso-date-string [date]
  (re-find #"\d{4}\-\d{2}\-\d{2}" (.toISOString date)))

(defn- set-history-list-id-for [date]
    (str "set-history-for-" (iso-date-string date)))

(defn- set-list-item [set]
  (let [li (d/clone (:set-history-list-item snippets))]
    (-> li (css/sel ".value.weight") (d/set-text! (:weight set)))
    (-> li (css/sel ".value.reps") (d/set-text! (:reps set)))
    (-> li (css/sel ".set-number .value") (d/set-text! (:number set)))
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

