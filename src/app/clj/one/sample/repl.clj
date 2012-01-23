(ns one.sample.repl
  "The starting namespace for the project. This is the namespace that
  users will land in when they start a Clojure REPL. It exists both to
  process commands passed on the command line (such as 'build') and to
  provide convenience functions like 'go'."
  (:use [clojure.repl])
  (:require [one.tools :as tools]
            [one.sample.dev-server :as dev]
            [clojure.java.browse :as browse]))

(defn go
  "Start a browser-connected REPL and launch a browser to talk to it."
  []
  (dev/run-server)
  (future (Thread/sleep 3000)
          (browse/browse-url "http://localhost:8080/development"))
  (tools/cljs-repl))

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
