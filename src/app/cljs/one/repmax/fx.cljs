(ns one.repmax.fx
  (:require [domina :as d]
            [goog.style :as style]))

(defn hide [content]
  (d/set-style! content "display" "none"))

(defn show [content]
  (d/set-style! content "display" nil))

