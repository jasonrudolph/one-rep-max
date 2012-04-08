(ns ^{:doc "Observe model changes involving the exercises and trigger
           side effects as appropriate."}
  one.repmax.exercises.observer
  (:require [one.dispatch :as dispatch]
            [one.repmax.mongohq :as mongo]))

(defmulti do-after (fn [{:keys [message]}] (:action message)))

(defmethod do-after :default [event])

; TODO Add on-error callback
(defmethod do-after :datastore-configuration/ready [{:keys [new]}]
  (mongo/find-documents (-> new :datastore-configuration :api-key)
                        "exercises"
                        (fn [data]
                          (dispatch/fire :action {:action :exercises/initialized-from-datastore, :exercises data}))))

;;; Register reactors

(dispatch/react-to #{:model-change}
                   (fn [_ event] (do-after event)))
