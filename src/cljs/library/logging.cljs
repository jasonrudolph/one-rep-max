(ns library.logging
  (:require [goog.debug.Console :as console]
            [goog.debug.FancyWindow :as fancy]
            [goog.debug.Logger :as logger]))

(def levels {:severe goog.debug.Logger.Level.SEVERE
             :warning goog.debug.Logger.Level.WARNING
             :info goog.debug.Logger.Level.INFO
             :config goog.debug.Logger.Level.CONFIG
             :fine goog.debug.Logger.Level.FINE
             :finer goog.debug.Logger.Level.FINER
             :finest goog.debug.Logger.Level.FINEST})

(defn get-logger [name]
  (goog.debug.Logger/getLogger name))

(defn severe [logger s] (.severe logger s))
(defn warning [logger s] (.warning logger s))
(defn info [logger s] (.info logger s))
(defn config [logger s] (.config logger s))
(defn fine [logger s] (.fine logger s))
(defn finer [logger s] (.finer logger s))
(defn finest [logger s] (.finest logger s))

(defn set-level [logger level]
  (.setLevel logger (get levels level goog.debug.Logger.Level.INFO)))

(defn console-output []
  (doto (goog.debug.Console.)
    (.setCapturing true)))

(defn fancy-output [name]
  (doto (goog.debug.FancyWindow. name)
    (.setEnabled true)
    (.init ())))
