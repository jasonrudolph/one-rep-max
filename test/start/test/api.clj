(ns start.test.api
  (:use [start.api])
  (:use [clojure.test]))

(deftest test-add-name
  (binding [database (atom #{})]
    (let [r (remote {:fn :add-name :args {:name "A"}})]
      (is (= r {:exists false}))))
  (binding [database (atom #{"A"})]
    (let [r (remote {:fn :add-name :args {:name "A"}})]
      (is (= r {:exists true})))))
