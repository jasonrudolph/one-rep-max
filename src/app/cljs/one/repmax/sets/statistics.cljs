(ns one.repmax.sets.statistics
  (:refer-clojure :exclude [set]))

(defn one-rep-max
  "Returns the calculated one-repetition maximum (1RM) based on the given set.

   Uses the Epley formula to calculate the 1RM:

   http://en.wikipedia.org/wiki/One-repetition_maximum#Epley_Fomula
  "
  [set]
  (let [weight (:weight set)
        reps (:reps set)
        value (+ (/ (* weight reps) 30) weight)]
    (.round js/Math value)))
