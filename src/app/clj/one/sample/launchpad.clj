(ns one.sample.launchpad
  "The starting namespace for the project. This is the namespace that
  users will land in when they start a Clojure REPL. It exists both to
  process commands passed on the command line (such as 'build') and to
  provide convenience functions like 'go'."
  (:use [clojure.repl])
  (:require [one.sample.config :as config]
            [one.tools :as tools]
            [one.sample.dev-server :as dev]
            [one.sample.prod-server :as prod]
            [clojure.java.io :as io]
            [clojure.java.browse :as browse]))

(defn go
  "Start a browser-connected REPL and launch a browser to talk to it."
  []
  (dev/run-server)
  (future (Thread/sleep 3000)
          (browse/browse-url "http://localhost:8080/development"))
  (tools/cljs-repl))

(defmulti command (fn [args] (first args)))

(defmethod command "build" [_]
  (println "Creating out/public...")
  (.mkdir (io/file "out"))
  (tools/copy-recursive-into "public" "out")
  (tools/delete "out/public/index.html"
                "out/public/design.html"
                "out/public/javascripts")
  (.mkdir (io/file "out/public/javascripts"))
  (println "Create advanced compiled JavaScript...")
  (tools/build-project config/config))

(defmethod command nil [_]
  (prod/run-server))

(defn -main [& args]
  (command args))