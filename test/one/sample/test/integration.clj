(ns one.sample.test.integration
  "Tests which cross the client server boundary."
  (:use [clojure.test]
        [one.sample.api :only (*database*)]
        [one.test :only (cljs-eval cljs-wait-for)]
        [clojure.java.browse :only (browse-url)]
        [cljs.repl :only (-setup -tear-down)]
        [cljs.repl.browser :only (repl-env)]
        [one.test :only (*eval-env*)]
        [one.sample.dev-server :only (run-server)]))

(defn setup
  "Start the development server and connect to the browser so that
  ClojureScript code can be evaluated from tests."
  [f]
  (let [server (run-server)
        eval-env (repl-env)]
    (-setup eval-env)
    (browse-url "http://localhost:8080/development")
    (binding [*eval-env* eval-env]
      (f))
    (-tear-down eval-env)
    (.stop server)))

(use-fixtures :once setup)

(deftest test-enter-new-name
  (reset! *database* #{})
  (cljs-eval one.sample.view
             (dispatch/fire :init)
             (set-value! (by-id "name-input") "Ted")
             (fx/enable-button "greet-button")
             (clojure.browser.dom/click-element :greet-button))
  (cljs-wait-for #(= % :greeting) one.sample.model (:state @state))
  (is (= (cljs-eval one.sample.view (.-innerHTML (first (nodes (by-class "name")))))
         "Ted"))
  (is (= (cljs-eval one.sample.view (.-innerHTML (first (nodes (by-class "again")))))
         ""))
  (is (= (cljs-eval one.sample.model @state)
         {:state :greeting, :name "Ted", :exists false}))
  (is (true? (contains? @*database* "Ted"))))

(deftest test-enter-existing-name
  (reset! *database* #{"Ted"})
  (cljs-eval one.sample.view
             (dispatch/fire :init)
             (set-value! (by-id "name-input") "Ted")
             (fx/enable-button "greet-button")
             (clojure.browser.dom/click-element :greet-button))
  (cljs-wait-for #(= % :greeting) one.sample.model (:state @state))
  (is (= (cljs-eval one.sample.view (.-innerHTML (first (nodes (by-class "name")))))
         "Ted"))
  (is (= (cljs-eval one.sample.view (.-innerHTML (first (nodes (by-class "again")))))
         "again"))
  (is (= (cljs-eval one.sample.model @state)
         {:state :greeting, :name "Ted", :exists true}))
  (is (true? (contains? @*database* "Ted"))))
