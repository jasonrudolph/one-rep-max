(ns one.repmax.sets.view
  (:require-macros [one.repmax.snippets :as snippets])
  (:refer-clojure :exclude [format set])
  (:require [clojure.browser.event :as event]
            [domina :as d]
            [domina.css :as css]
            [goog.events.EventType :as event-type]
            [goog.i18n.DateTimeFormat :as date-time-format]
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
    (d/set-value! (css/sel "#exercise-id") (:_id exercise))
    (d/append! content (:set-history-div snippets))
    (add-event-listener-for-persisting-set)
    (add-event-listener-for-returning-to-exercise-list)))

(defmethod render :new-set/history-initialized [{:keys [new]}]
  (let [recent-sets (-> new :new-set :history)
        recent-sets-by-day (partition-by #(->yyyy-mm-dd (object-id->creation-ts (:_id %))) recent-sets)
        most-recent-sets-by-day (take 4 recent-sets-by-day)
        sets-in-chronological-order (sort-by :_id (flatten most-recent-sets-by-day))]
    (doseq [s sets-in-chronological-order]
      (render-new-set-in-history s))))

(defmethod render :new-set/persisted [{:keys [message]}]
  (render-new-set-in-history (:set message)))

(defn- render-new-set-in-history [set]
  (let [date (object-id->creation-ts (:_id set))
        set-list-section (set-history-list-for date)
        set-number (-> set-list-section (css/sel "li") d/nodes count inc)
        new-list-item (set-list-item (assoc set :number set-number))
        set-list (css/sel set-list-section "ol")]
    (d/append! set-list new-list-item)
    (reverse-sort! (css/sel set-list "li") :id)))

; TODO Once Domina issue #30 is resolved, rework this function to be
;      reverse-sort-children!, which takes a parent node and a child
;      sort attribute. See https://github.com/levand/domina/pull/30
(defn- reverse-sort! [content sort-attr]
  (let [nodes (d/nodes content)
        parent-node (.-parentNode (first nodes))
        unsorted-nodes (d/detach! nodes)
        sorted-nodes (sort-by #(d/attr % sort-attr) unsorted-nodes)]
    (doseq [node sorted-nodes]
      (d/prepend! parent-node node))))

(defn- object-id->creation-ts
  "Returns a JavaScript Date object representing the creation timestamp for the
   document with the given MongoDB object ID.

   When a document is created, MongoDB assigns a unique object ID to the
   document. The object ID includes the timestamp (in UTC) at which the
   document was created.

   Example:

     (object-id->creation-ts \"4ff17f0327823e0001000002u\")

     ;;=> #<Mon Jul 02 2012 06:59:15 GMT-0400 (EDT)>
  "
  [oid]
  (let [timestamp-as-hex (subs oid 0 8)
        timestamp-in-seconds (js/parseInt timestamp-as-hex 16)
        timestamp-in-milliseconds (* 1000 timestamp-in-seconds)]
    (js/Date. timestamp-in-milliseconds)))

(defn- set-history-list-for [date]
  (let [set-list-id (set-history-list-id-for date)
        existing-set-list (d/by-id set-list-id)]
    (or existing-set-list (create-set-list date))))

(defn- create-set-list [date]
  (let [parent-node (d/by-id "recent-sets-by-day")
        set-list (d/clone (:set-history-list snippets))
        set-list-id (set-history-list-id-for date)
        set-list-date (css/sel set-list "header time")]
    (d/set-attr! set-list :id set-list-id)
    (d/set-attr! set-list-date "datetime" (->yyyy-mm-dd date))
    (d/set-text! set-list-date (.toLocaleDateString date))
    (d/append! parent-node set-list)
    (reverse-sort! (css/sel parent-node "section") :id)
    set-list))

(defn- ->yyyy-mm-dd [date]
  (format-date "yyyy-MM-dd" date))

(defn- format-date [format date]
  (let [formatter (goog.i18n.DateTimeFormat. format)]
    (.format formatter date)))

(defn- set-history-list-id-for [date]
  (str "set-history-for-" (->yyyy-mm-dd date)))

(defn- set-list-item [{:keys [_id weight reps number]}]
  (let [datetime (object-id->creation-ts _id)
        li (d/clone (:set-history-list-item snippets))]
    (-> li (css/sel ".value.weight") (d/set-text! weight))
    (-> li (css/sel ".value.reps") (d/set-text! reps))
    (-> li (css/sel ".created-at time") (d/set-attr! :datetime (.toISOString datetime)))
    (-> li (css/sel ".created-at time") (d/set-text! (format-date goog.i18n.DateTimeFormat.Format.MEDIUM_TIME datetime)))
    (-> li (css/sel ".set-number .value") (d/set-text! number))
    (-> li (d/set-attr! :id (set-dom-id _id)))
    li))

(defn- set-dom-id [set-id]
  (str "set-" set-id))

(defn- add-event-listener-for-persisting-set []
  (event/listen (d/by-id "new-set-form-button")
                event-type/CLICK
                #(dispatch/fire :action {:action :new-set/create
                                         :exercise-id (d/value (d/by-id "exercise-id"))
                                         :weight (d/value (d/by-id "weight-input"))
                                         :reps (d/value (d/by-id "reps-input"))})))

(defn- add-event-listener-for-returning-to-exercise-list []
  (event/listen (d/by-id "back-button")
                event-type/CLICK
                #(dispatch/fire :action {:action :new-set/back})))

;;; Register reactors

(dispatch/react-to #{:model-change}
                   (fn [_ event] (render event)))

