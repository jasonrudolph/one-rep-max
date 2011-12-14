(ns ^{:doc "Contains the model for the sample application which is a single atom
  named 'state' and a watcher which fires a state-change event when
  the atom is modified."}
  one.sample.model
  (:require [library.dispatch :as dispatch]))

(def ^{:doc "An atom containing a map which is the application's current state."}
  state (atom {}))

(add-watch state :state-change-key
           (fn [k r o n]
             (dispatch/fire :state-change n)))

