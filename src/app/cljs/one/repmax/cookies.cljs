(ns one.repmax.cookies
  (:require [goog.net.cookies :as gcookies]))

(def number-of-seconds-in-one-year (* 60 60 24 365))

(defn get-cookie [name]
  (let [value (.get goog.net.cookies name)]
    (if (= js/undefined value)
      nil
      value)))

(defn set-cookie [name value]
  (.set goog.net.cookies name value number-of-seconds-in-one-year))

