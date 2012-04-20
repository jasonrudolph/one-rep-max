(ns one.repmax.repl
  "The starting namespace for the project. This is the namespace that
  users will land in when they start a Clojure REPL. It exists to
  provide convenience functions like 'go' and 'dev-server'."
  (:use [cljs.repl.browser :only (repl-env)])
  (:require [cljs.repl]
            [one.repmax.dev-server :as dev]
            [clojure.java.browse :as browse]))

(defn cljs-repl []
  (cljs.repl/repl (repl-env)))

(defn go
  "Start a browser-connected REPL and launch a browser to talk to it."
  []
  (dev/run-server)
  (future (Thread/sleep 3000)
          (browse/browse-url "http://localhost:8080/development"))
  (cljs-repl))

(defn dev-server
  "Start the development server and open the host application in the
  default browser."
  []
  (dev/run-server)
  (future (Thread/sleep 3000)
          (browse/browse-url "http://localhost:8080")))

(println)
(println "Type (go) to launch the development server and setup a browser-connected REPL.")
(println "Type (dev-server) to launch only the development server.")
(println)
