(ns one.repmax.cookies
  (:require [goog.net.cookies :as gcookies]))

(def number-of-seconds-in-one-year (* 60 60 24 365))

(defn get-cookie [cookie-name]
  (let [normalized-cookie-name (name cookie-name)
        value (.get goog.net.cookies normalized-cookie-name)]
    (if (= js/undefined value)
      nil
      value)))

(defn set-cookie [cookie-name cookie-value]
  (let [normalized-cookie-name (name cookie-name)]
    (.set goog.net.cookies normalized-cookie-name cookie-value number-of-seconds-in-one-year)))

