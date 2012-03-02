(ns one.repmax.model
  (:require [one.dispatch :as dispatch]))

(def ^{:doc "An atom containing a map which is the application's current state."}
  state (atom {}))

(add-watch state :state-change-key
           (fn [k r o n]
             (dispatch/fire :state-change n)))
