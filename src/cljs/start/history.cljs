(ns start.history
  (:require [library.dispatch :as dispatch]
            [library.browser.history :as history]))

(defn nav-handler [{:keys [token navigation?]}]
  (when navigation?
    (dispatch/fire token)
    #_(swap! state assoc :state token)))

(def history (history/history nav-handler))

(dispatch/respond-to #{:form :greeting}
                     (fn [t _] (history/set-token history t)))
