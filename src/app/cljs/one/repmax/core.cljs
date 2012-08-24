(ns ^{:doc "Contains the entry point for the application."}
  one.repmax.core
  (:require [one.browser.repl-client :as repl-client]
            [one.dispatch :as dispatch]
            [one.repmax.cookies :as cookies]))

(defn ^:export repl
  "Connects to a ClojureScript REPL running on localhost port 9000.

  This allows a browser-connected REPL to send JavaScript to the
  browser for evaluation. This function should be called from a script
  in the development host HTML page."
  []
  (repl-client/repl))

(defn ^:export start
  "Start the application by firing an event to kick off the initialization
  of the datastore.

  This function must be called from the host HTML page to start the
  application."
  []
  (dispatch/fire :action {:action :datastore-configuration/initialize}))

(defn ^:export auto-authenticate [] ; TODO Consider generalizing; perhaps replace with a general-purpose configuration fn
  (dispatch/react-to #{:model-change}
                     (fn [_ event-data]
                       (let [old-state (-> event-data :old :state)
                             new-state (-> event-data :new :state)]
                         (if (and (= :datastore-configuration new-state) (not= :datastore-configuration old-state))
                           (dispatch/fire :action {:action :datastore-configuration/update
                                                   :api-key (cookies/get-cookie :api-key)
                                                   :database (cookies/get-cookie :database)})))))) ; TODO Consider extracting fn
