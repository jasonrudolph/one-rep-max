(ns start.core
  (:require [clojure.browser.repl :as repl]
            [library.event-dispatch :as dispatch]
            [start.view :as view]))

(defn ^:export repl
  []
  (repl/connect "http://localhost:9000/repl"))

(defn ^:export start
  []
  (dispatch/fire :form))
