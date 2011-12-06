(ns start.model
  (:require [library.dispatch :as dispatch]))

(def state (atom nil))

(add-watch state :state-change-key
           (fn [k r o n]
             (dispatch/fire :state-change n)))

