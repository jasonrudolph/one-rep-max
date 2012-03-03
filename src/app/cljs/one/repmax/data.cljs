(ns ^{:doc "Provide access to persistent data."}
  one.repmax.data
  (:require [one.repmax.mongohq :as mongo]))

(def ^{:private true
       :doc "An atom containing a Vector of all exercises."}
  exercises (atom []))

; TODO Add on-error callback
(defn find-exercises
  ([on-success]
   (mongo/find-documents "exercises"
                         (fn [data]
                           (reset! exercises data)
                           (on-success data)))))

