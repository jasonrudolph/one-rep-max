(ns one.test.dispatch
  "Event dispatching tests. All of the tests in the namespace are testing
  ClojureScript code which is being evaluated in the browser."
  (:use [clojure.test]
        [one.test :only (cljs-eval within-browser-env)]))

;; Set up ClojureScript testing. This means that all ClojureScript
;; code will be evaluated in the `one.dispatch` namespace in a fresh
;; browser evaluation environment.

#_(js-ns one.dispatch
       within-browser-env "http://localhost:8080/fresh")

#_(def js-test-namespace 'one.dispatch)

#_(def js-functions (atom []))

#_(use-fixtures :once
              (partial within-browser-env "http://localhost:8080/fresh"
                       (fn [] (do (cljs-eval cljs.user (load-namespace one.dispatch))
                                 (doseq [f @js-functions]
                                   (f))))))

;; Define ClojureScript helper functions which will be available in the
;; evaluation environment.

#_(js-defn with-reaction
  "This test helper function is a ClojureScript function. It
  accepts a function which will be called in a context where a reaction
  is defined."
  [f]
  (let [recorded-reactions (atom [])
        reaction (react-to #{:do-something} (fn [t d] (swap! recorded-reactions conj [t d])))]
    (f reaction)
    (delete-reaction reaction)
    (deref recorded-reactions)))

#_(swap! js-functions conj
  (fn []
    (cljs-eval one.dispatch
               (defn with-reaction [f]
                 (let [recorded-reactions (atom [])
                       reaction (react-to #{:do-something} (fn [t d] (swap! recorded-reactions conj [t d])))]
                   (f reaction)
                   (delete-reaction reaction)
                   (deref recorded-reactions))))))

(deftest create-reaction
  (is (= [:max-count :event-pred :reactor]
         (cljs-eval one.test.dispatch (let [reaction (react-to #{:do-something} (constantly true))]
               (keys reaction))))))

(deftest reaction-catches-event
  (is (= [[:do-something nil]]
         (cljs-eval one.test.dispatch (with-reaction (fn [_] (fire :do-something)))))))

(deftest reaction-catches-two-events
  (is (= [[:do-something nil] [:do-something nil]]
         (cljs-eval one.test.dispatch (with-reaction (fn [_]
                              (fire :do-something)
                              (fire :do-something)))))))

(deftest reaction-catches-only-its-events
  (is (= []
         (cljs-eval one.test.dispatch (with-reaction (fn [_] (fire :something-else)))))))

(deftest reaction-catches-event-with-data
  (is (= [[:do-something 1]]
         (cljs-eval one.test.dispatch (with-reaction (fn [_] (fire :do-something 1)))))))

(deftest deleted-reaction-does-nothing
  (is (= [[:do-something 1]]
         (cljs-eval one.test.dispatch (with-reaction (fn [reaction]
                              (fire :do-something 1)
                              (delete-reaction reaction)
                              (fire :do-something 2)))))))

(deftest reactions-are-deleted-when-max-count-becomes-zero
  (is (= #{[1 :do-something 1] [2 :do-something 1] [2 :do-something 2]}
         (cljs-eval one.test.dispatch (let [recorded-reactions (atom #{})
                   reaction-once (react-to 1 #{:do-something}
                                           #(swap! recorded-reactions conj [1 %1 %2]))
                   reaction-twice (react-to 2 #{:do-something}
                                            #(swap! recorded-reactions conj [2 %1 %2]))]
               (fire :do-something 1)
               (fire :do-something 2)
               (fire :do-something 3)
               @recorded-reactions)))))

(comment

  (dev-server)

  (use 'one.test)
  (use 'clojure.test)
  (use 'one.test.dispatch :reload-all)

  (def ee (browser-eval-env))
  (evaluate-cljs ee 'cljs.user '(+ 1 1))

  (evaluate-cljs ee 'cljs.user
                 '(ns one.test.dispatch
                    (:use [one.dispatch :only [fire react-to delete-reaction]])))

  (evaluate-cljs ee 'one.test.dispatch '(+ 1 1))

  (binding [*eval-env* ee]
    (cljs-eval one.test.dispatch (defn something [x] (inc x))))

  (evaluate-cljs ee 'one.test.dispatch '(defn something [x] (inc x)))

  (evaluate-cljs ee 'one.test.dispatch '(something 3))

  (evaluate-cljs ee 'one.test.dispatch
                 '(defn with-reaction [f]
                    (let [recorded-reactions (atom [])
                          reaction (react-to #{:do-something} (fn [t d] (swap! recorded-reactions conj [t d])))]
                      (f reaction)
                      (delete-reaction reaction)
                      (deref recorded-reactions))))

  (binding [*eval-env* ee]
    (run-tests 'one.test.dispatch))

  )
