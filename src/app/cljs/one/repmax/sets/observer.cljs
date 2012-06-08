(ns ^{:doc "Observe model changes involving sets and trigger
           side effects as appropriate."}
  one.repmax.sets.observer
  (:refer-clojure :exclude [set])
  (:require [one.dispatch :as dispatch]
            [one.repmax.mongohq :as mongo]))

(defmulti do-after (fn [{:keys [message]}] (:action message)))

(defmethod do-after :default [event])

(defmethod do-after :new-set/create [{:keys [new]}]
  (let [document {:exercise-id (-> new :new-set :exercise :_id)
                  :weight      (-> new :new-set :weight)
                  :reps        (-> new :new-set :reps)}]
    (mongo/create-document (-> new :datastore-configuration :api-key)
                           "sets"
                           document
                           #(create-set-success-callback document %)
                           #(create-set-error-callback document %))))

(defn create-set-success-callback [document response]
  (let [set-id (:_id response)
        set (assoc document :_id set-id)]
    (dispatch/fire :action {:action :new-set/persisted, :set set})))

(defn create-set-error-callback [document response]
  (dispatch/fire :action {:action :new-set/create-failed, :set document, :error response}))

;;; Register reactors

(dispatch/react-to #{:model-change}
                   (fn [_ event] (do-after event)))
