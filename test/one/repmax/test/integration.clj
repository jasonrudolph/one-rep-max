(ns one.repmax.test.integration
  (:use [clojure.test]
        [one.test :only (cljs-eval cljs-wait-for within-browser-env)]
        [one.repmax.dev-server :only (run-server)]))

(use-fixtures :once (fn [f] (within-browser-env f
                                               :url "http://localhost:8080/development"
                                               :start-server run-server)))

; TODO Automate setup of seed data, so that we always start in a known good state

(deftest test-initialize-datastore
         (cljs-eval one.repmax.test.integration-util
                    (d/set-value! (d/by-id "api-key-input") "FILL-ME-IN") ; TODO Read API key from file
                    (d/set-value! (d/by-id "database-input") "jasonrudolph-dev-one-rep-max") ; TODO Read DB name from file
                    (dom/click-element :datastore-configuration-form-button))
         (cljs-wait-for #(> % 0) one.repmax.test.integration-util (count (d/nodes (css/sel "#exercise-list ol li"))))
         (let [exercise-labels (cljs-eval one.repmax.test.integration-util
                                          (into #{} (map #(.-innerHTML %) (d/nodes (css/sel "#exercise-list ol li .list-item-label")))))]
           (is (exercise-labels "Overhead Squat"))))
