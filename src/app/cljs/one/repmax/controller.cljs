(ns ^{:doc "Respond to user actions."}
  one.repmax.controller
  (:use [one.repmax.model :only (state)])
  (:require [one.dispatch :as dispatch]
            [goog.uri.utils :as uri]))

(defmulti action
  "Accepts a map containing information about an action to perform.

  Dispatches on the value of the `:type` key.

  The `:init` action will initialize the appliation's state."
  :type)

(defmethod action :init [_]
  (reset! state {:state :exercise-list}))

(dispatch/react-to #{:init}
                   (fn [t d] (action (assoc d :type t))))
