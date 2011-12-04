(ns start.test.suite
  "Test suites for this application."
  (use [clojure.java.browse :only (browse-url)]
       [clojure.test]
       [cljs.repl :only (-setup -tear-down)]
       [cljs.repl.browser :only (repl-env)]
       [library.test :only (pause *eval-env*)]
       [start.dev-server :only (run-server)]       
       [start.test.api]
       [start.test.integration]))

(defn run-all
  "Run all of the tests for this application. Start the development
   server and connect to the browser so that ClojureScript code can be
   evaluated from tests."
  []
  (let [server (run-server)
        eval-env (repl-env)]
    (-setup eval-env)
    (browse-url "http://localhost:8080/development")
    (pause 1000)
    (binding [*eval-env* eval-env]
      (run-tests 'start.test.api
                 'start.test.integration))
    (-tear-down eval-env)
    (.stop server)
    (System/exit 0)))
