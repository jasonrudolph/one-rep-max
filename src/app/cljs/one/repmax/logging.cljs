(ns ^{:doc "When this library is loaded, create a logger named 'events'.

           See comments at end of file for example usage.

           For more information see one.logging."}
  one.repmax.logging
  (:require [one.dispatch :as dispatch]
            [one.logging :as log]))

(def ^{:doc "The logger that receives all application-specific events."}
  logger (log/get-logger "events"))

(defn log!
  "Create a new reaction that will react to ALL events and log them to the
  given logger.

  WARNING: For events that have a large amount of data (which is the case for
  most :model-change events in One Rep Max), sending these events to the logger
  has a significant negative performance impact on the application. You are
  recommended to log events ONLY For debugging purposes."
  [logger]
  (dispatch/react-to (constantly true)
                     (fn [t d] (log/info logger (str (pr-str t) " - " (pr-str d))))))

(comment

  ;; register a reaction that will react to ALL events and log them to the 'events' logger
  (def logging-reaction (log! logger))

  ;; send the the log output to the console
  (log/start-display (log/console-output))

  ;; stop logging events
  (dispatch/delete-reaction logging-reaction)

  ;; change the logging level
  (log/set-level logger :fine)

  )
