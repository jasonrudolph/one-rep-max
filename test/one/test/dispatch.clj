(ns one.test.dispatch
  "Event dispatching tests. All of the tests in the namespace are testing
  ClojureScript code which is being evaluated in the browser."
  (:use [clojure.test]
        [one.test :only (cljs-eval within-browser-env js *eval-env* *js-test-ns*)]))

;; Setup the ClojureScript environment
;; ===================================
;; All of this, and the ns form above, could be replaced by a single
;; macro.

(defn setup-js-environment []
  
  (cljs-eval cljs.user
             (ns one.test.dispatch
               (:use [one.dispatch :only [fire react-to delete-reaction]])))
  
  (cljs-eval one.test.dispatch
             (defn with-reaction [f]
               (let [recorded-reactions (atom [])
                     reaction (react-to #{:do-something}
                                        (fn [t d] (swap! recorded-reactions conj [t d])))]
                 (f reaction)
                 (delete-reaction reaction)
                 (deref recorded-reactions)))))

(use-fixtures :once
              (fn [f] (within-browser-env (fn [] (binding [*js-test-ns* 'one.test.dispatch]
                                                 (f)))
                                         :init setup-js-environment)))

;; Define tests
;; ============

(deftest create-reaction
  (is (= #{:max-count :event-pred :reactor}
         (js (let [reaction (react-to #{:do-something} (constantly true))]
               (set (keys reaction)))))))

(deftest reaction-catches-event
  (is (= [[:do-something nil]]
         (js (with-reaction (fn [_] (fire :do-something)))))))

(deftest reaction-catches-two-events
  (is (= [[:do-something nil] [:do-something nil]]
         (js (with-reaction (fn [_]
                              (fire :do-something)
                              (fire :do-something)))))))

(deftest reaction-catches-only-its-events
  (is (= []
         (js (with-reaction (fn [_] (fire :something-else)))))))

(deftest reaction-catches-event-with-data
  (is (= [[:do-something 1]]
         (js (with-reaction (fn [_] (fire :do-something 1)))))))

(deftest deleted-reaction-does-nothing
  (is (= [[:do-something 1]]
         (js (with-reaction (fn [reaction]
                              (fire :do-something 1)
                              (delete-reaction reaction)
                              (fire :do-something 2)))))))

(deftest reactions-are-deleted-when-max-count-becomes-zero
  (is (= #{[1 :do-something 1] [2 :do-something 1] [2 :do-something 2]}
         (js (let [recorded-reactions (atom #{})
                   reaction-once (react-to 1 #{:do-something}
                                           #(swap! recorded-reactions conj [1 %1 %2]))
                   reaction-twice (react-to 2 #{:do-something}
                                            #(swap! recorded-reactions conj [2 %1 %2]))]
               (fire :do-something 1)
               (fire :do-something 2)
               (fire :do-something 3)
               @recorded-reactions)))))

(comment

  ;; Try running these tests from a Clojure REPL.
  
  (dev-server)
  (require 'one.test)
  (require 'clojure.test)
  (require 'one.test.dispatch)

  (def ee (one.test/browser-eval-env))

  ;; go to the development or fresh page

  (binding [one.test/*eval-env* ee]
    (clojure.test/run-tests 'one.test.dispatch))

  ;; From a ClojureScript REPL, you may run these tests with:

  (run-tests 'one.test.dispatch)
  )
