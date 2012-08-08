(ns ^{:doc "Observe model changes involving the exercises and trigger
           side effects as appropriate."}
  one.repmax.exercises.observer
  (:require [one.dispatch :as dispatch]
            [one.repmax.mongohq :as mongo]))

(defmulti do-after (fn [{:keys [message]}] (:action message)))

(defmethod do-after :default [event])

; TODO Add on-error callback
(defmethod do-after :datastore-configuration/ready [{:keys [new]}]
  (mongo/find-documents (:datastore-configuration new)
                        "exercises"
                        (fn [data]
                          (dispatch/fire :action {:action :exercises/initialized-from-datastore, :exercises data}))
                        :sort {:name 1}))

(defmethod do-after :new-exercise/create [{:keys [new]}]
  (let [document {:name (-> new :new-exercise :name)}]
    (mongo/create-document (:datastore-configuration new)
                           "exercises"
                           document
                           #(create-exercise-success-callback document %)
                           #(create-exercise-error-callback document %))))

(defn create-exercise-success-callback [document response]
  (let [exercise-id (:_id response)
        exercise (assoc document :_id exercise-id)]
    (dispatch/fire :action {:action :new-exercise/persisted, :exercise exercise})))

(defn create-exercise-error-callback [document response]
  (dispatch/fire :action {:action :new-exercise/create-failed, :exercise document, :error response}))

;;; Register reactors

(dispatch/react-to #{:model-change}
                   (fn [_ event] (do-after event)))
