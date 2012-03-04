(ns one.repmax.model
  (:require [one.dispatch :as dispatch]
            [one.repmax.data :as data]))

(def ^{:doc "An atom containing a map which is the application's current state."}
  state (atom {}))

(add-watch state :state-change-key
           (fn [k r o n]
             (dispatch/fire :state-change n)))

(dispatch/react-to #{:init}
  (fn [t d]
    (initialize-exercise-list)))

;;; Managing the Exercise List

(def ^{:private true}
  last-exercise-search-number (atom 0))

(def ^{:private true}
  exercise-search-results (atom {:search-number @last-exercise-search-number, :search-string nil, :exercises []}))

(add-watch exercise-search-results :exercise-search-results-change-key
           (fn [_ _ old-results new-results]
             (if-not (= old-results new-results)
               (dispatch/fire :exercise-search-results-ready (:exercises new-results))))) ; TODO Consider renaming to :exercise-search-results-changed or :exercise-list-changed

(defn initialize-exercise-list []
  (find-exercises))

(defn find-exercises
  ([] (find-exercises nil))
  ([name]
   (let [search-number (swap! last-exercise-search-number inc)]
     (data/find-exercises name
       (fn [exercises]
         (receive-exercise-search-results search-number name exercises))))))

(defn- receive-exercise-search-results [search-number search-string exercises]
  (swap! exercise-search-results
         (fn [previous-search-results new-search-results]
           (if (> (:search-number new-search-results) (:search-number previous-search-results))
             new-search-results
             previous-search-results))
         {:search-number search-number, :search-string search-string, :exercises exercises}))

(dispatch/react-to #{:exercise-search-field-changed}
                   (fn [_ d] (find-exercises d)))

