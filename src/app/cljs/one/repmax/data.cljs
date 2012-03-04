(ns ^{:doc "Provide access to persistent data."}
  one.repmax.data
  (:require [clojure.string :as string]
            [one.repmax.mongohq :as mongo]))

(defn- filter-by-attribute [s attribute-name value-like]
  (let [normalize #(string/lower-case %)
        pattern (re-pattern (str ".*" (normalize value-like) ".*"))]
    (filter #(re-find pattern (normalize (attribute-name %))) s)))

(def ^{:private true
       :doc "An atom containing a Vector of all exercises."}
  exercises (atom []))

(defn- initialize-exercises [on-success]
  (mongo/find-documents "exercises"
                        (fn [data]
                          (reset! exercises data)
                          (on-success))))

; TODO Add on-error callback
(defn find-exercises [name on-success]
  (if (empty? @exercises)
    (initialize-exercises #(find-exercises name on-success))
    (if (empty? name)
      (on-success @exercises)
      (on-success (filter-by-attribute @exercises :name name)))))

