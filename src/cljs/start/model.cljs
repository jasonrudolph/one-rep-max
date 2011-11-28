(ns start.model
  (:use [library.browser.remote :only (request)])
  (:require [cljs.reader :as reader]
            [clojure.browser.event :as event]
            [library.event-dispatch :as dispatch]))

(def state (atom nil))

(add-watch state :state-change
           (fn [k r o n]
             (dispatch/fire :state-change n)))

(defmulti action :type)

(defmethod action :form [_]
  (reset! state {:state :form}))

(defmethod action :greeting [{name :name}]
  (if (empty? name)
    (swap! state assoc :error "I can't greet you without knowing your name!")
    (swap! state assoc :state :greeting :name name)))

(dispatch/respond-to #{:form :greeting}
                     (fn [t d] (action (assoc d :type t))))

(comment

  ;; Test each state
  (swap! state assoc :state :form)
  (swap! state assoc :state :form :error "Something is wrong!")
  (swap! state assoc :state :greeting :name "James")
  )

