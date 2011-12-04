(ns start.logging
  (:use [library.logging :only (get-logger fancy-output info)])
  (:require [library.dispatch :as dispatch]))

(def logger (get-logger "events"))

(def fancy (fancy-output "main"))

(dispatch/respond-to identity
                     (fn [t d] (info logger (str (pr-str t) " - " (pr-str d)))))
