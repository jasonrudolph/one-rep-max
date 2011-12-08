(ns ^{:doc "Wraps Google Closure's history functionality."}
  library.browser.history
  (:require [clojure.browser.event :as event]
            [goog.History :as history]))

(extend-type goog.History
  
  event/EventType
  (event-types [this]
    (into {}
          (map
           (fn [[k v]]
             [(keyword (. k (toLowerCase)))
              v])
           (js->clj goog.history.EventType)))))

(defn history
  [callback]
  (let [h (goog.History.)]
    (do (event/listen h "navigate"
                      (fn [e]
                        (callback {:token (keyword (.token e))
                                   :type (.type e)
                                   :navigation? (.isNavigation e)})))
        (.setEnabled h true)
        h)))

(defn set-token
  [history token]
  (.setToken history (name token)))

