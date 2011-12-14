(ns one.sample.test.integration
  "Tests which cross the client server boundary."
  (:use [clojure.test]
        [one.sample.api :only (*database*)]
        [one.test :only (cljs-eval cljs-wait-for)]))

(deftest test-enter-new-name
  (reset! *database* #{})
  (cljs-eval one.sample.view
             (dispatch/fire :form)
             (dom/set-value :name-input "Ted")
             (dom/click-element :greet-button))
  (cljs-wait-for #(= % :greeting) one.sample.model (:state @state))
  (is (= (cljs-eval one.sample.view (.innerHTML (dom/get-element :name)))
         "Ted"))
  (is (= (cljs-eval one.sample.model @state)
         {:state :greeting, :name "Ted", :exists false}))
  (is (true? (contains? @*database* "Ted"))))

(deftest test-enter-existing-name
  (reset! *database* #{"Ted"})
  (cljs-eval one.sample.view
             (dispatch/fire :form)
             (dom/set-value :name-input "Ted")
             (dom/click-element :greet-button))
  (cljs-wait-for #(= % :greeting) one.sample.model (:state @state))
  (is (= (cljs-eval one.sample.view (.innerHTML (dom/get-element :name)))
         " again Ted"))
  (is (= (cljs-eval one.sample.model @state)
         {:state :greeting, :name "Ted", :exists true}))
  (is (true? (contains? @*database* "Ted"))))
