(ns one.repmax.test.integration
  (:use [clojure.test]
        [clojure.java.io :only (resource)]
        [one.test :only (cljs-eval cljs-wait-for within-browser-env)]
        [one.repmax.dev-server :only (run-server)]))

(use-fixtures :once (fn [f] (within-browser-env f
                                               :url "http://localhost:8080/development"
                                               :start-server run-server)))

; TODO Automate setup of seed data, so that we always start in a known good state

; TODO Update README to explain need to: cp resources/datastore.example.clj resources/datastore.clj

(def datastore-configuration ;; reads resource file from classpath to load credentials for testing
  (binding [*read-eval* false]
    (-> "datastore.clj"
      resource
      slurp
      read-string)))

(defmacro ds-config-form* [api-key database]
  `(cljs-eval one.repmax.test.integration-util
              (d/set-value! (d/by-id "api-key-input") ~api-key)
              (d/set-value! (d/by-id "database-input") ~database)
              (clojure.browser.dom/click-element :datastore-configuration-form-button)))

(defn ds-config-form [config]
  (ds-config-form* (:api-key config) (:database config)))

(deftest test-initialize-datastore
         (ds-config-form datastore-configuration)
         (cljs-wait-for #(> % 0) one.repmax.datastore-configuration.view (count (d/nodes (css/sel "#exercise-list ol li"))))
         (let [exercise-labels (cljs-eval one.repmax.datastore-configuration.view
                                          (into #{} (map #(.-innerHTML %) (d/nodes (css/sel "#exercise-list ol li .list-item-label")))))]
           (is (exercise-labels "Overhead Squat"))))
