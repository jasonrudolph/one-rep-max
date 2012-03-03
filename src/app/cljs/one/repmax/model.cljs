(ns one.repmax.model
  (:require [one.dispatch :as dispatch]
            [one.repmax.data :as data]))

(def ^{:doc "An atom containing a map which is the application's current state."}
  state (atom {}))

(add-watch state :state-change-key
           (fn [k r o n]
             (dispatch/fire :state-change n)))

(dispatch/react-to #{:init}
  (fn [t d]
    (initialize-exercise-list)))

(defn initialize-exercise-list []
  (data/find-exercises
    (fn [exercises]
      (dispatch/fire :exercise-search-results-ready exercises))))
