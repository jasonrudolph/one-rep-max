(ns ^{:doc "Contains the entry point for the ClojureScript sample application."}
  one.sample.core
  (:require [clojure.browser.repl :as repl]
            [one.dispatch :as dispatch]
            [one.sample.view :as view]))

;; TODO: Add marginalia comment to explain what :export is for.

(defn ^:export repl
  "Connects to a ClojureScript REPL running on localhost port 9000.

  This allows a browser-connected REPL to send JavaScript to the
  browser for evaluation. This function should be called from a script
  in the development host HTML page."
  []
  (repl/connect "http://localhost:9000/repl"))

(defn ^:export start
  "Start the application by firing a :form event which will cause the
  form view to be displayed.

  This function must be called from the host HTML page to start the
  application."
  []
  (dispatch/fire :init))
