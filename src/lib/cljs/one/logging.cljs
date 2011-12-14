(ns ^{:doc "Basic wrapper around Google Closure's logging API.

  This library can be improved to support more of the features
  provided by Google Closure's logging."}
  one.logging
  (:require [goog.debug.Console :as console]
            [goog.debug.FancyWindow :as fancy]
            [goog.debug.Logger :as logger]))

(def ^{:doc "Maps log level keywords to goog.debug.Logger.Levels."}
  levels {:severe goog.debug.Logger.Level.SEVERE
          :warning goog.debug.Logger.Level.WARNING
          :info goog.debug.Logger.Level.INFO
          :config goog.debug.Logger.Level.CONFIG
          :fine goog.debug.Logger.Level.FINE
          :finer goog.debug.Logger.Level.FINER
          :finest goog.debug.Logger.Level.FINEST})

(defn get-logger
  "Given a name, return an existing logger if one exists or create a
  new logger."
  [name]
  (goog.debug.Logger/getLogger name))

(defn severe
  "Given a logger and a message, write the message to the log with a
  logging level of severe."
  [logger s] (.severe logger s))

(defn warning
  "Given a logger and a message, write the message to the log with a
  logging level of warning."
  [logger s] (.warning logger s))

(defn info
  "Given a logger and a message, write the message to the log with a
  logging level of info."
  [logger s] (.info logger s))

(defn config
  "Given a logger and a message, write the message to the log with a
  logging level of config."
  [logger s] (.config logger s))

(defn fine
  "Given a logger and a message, write the message to the log with a
  logging level of fine."
  [logger s] (.fine logger s))

(defn finer
  "Given a logger and a message, write the message to the log with a
  logging level of finer."
  [logger s] (.finer logger s))

(defn finest
  "Given a logger and a message, write the message to the log with a
  logging level of finest."
  [logger s] (.finest logger s))

(defn set-level
  "Set the logging level of logger to level.

  The level argument must be a keyword."
  [logger level]
  (.setLevel logger (get levels level goog.debug.Logger.Level.INFO)))

(defn console-output
  "Direct log messages to the browser's console window."
  []
  (doto (goog.debug.Console.)
    (.setCapturing true)))

(defn fancy-output
  "Open a fancy logging window and direct log messages to it."
  [name]
  (doto (goog.debug.FancyWindow. name)
    (.setEnabled true)
    (.init ())))
