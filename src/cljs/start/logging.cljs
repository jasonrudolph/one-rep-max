(ns start.logging
  (:require [library.dispatch :as dispatch]
            [library.logging :as log]))

(def logger (log/get-logger "events"))

(dispatch/respond-to identity
                     (fn [t d] (log/info logger (str (pr-str t) " - " (pr-str d)))))

(comment

  ;; log to the console
  (log/console-output)
  ;; log to to the "fancy" window
  (log/fancy-output "main")
  ;; change the logging level
  (log/set-level logger :fine)
  )
