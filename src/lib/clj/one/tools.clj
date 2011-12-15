(ns one.tools
  "Support for building deployment artifacts for a project."
  (:use [cljs.closure :only (build)]
        [one.host-page :only (application-host)]
        [one.config :only (cljs-build-opts production-js)])
  (:require [clojure.java.io :as io]))

(defn build-project
  "Emit both a JavaScript file containing the compiled ClojureScript
  application and the host HTML page."
  [config]
  (build (:app-root config) (assoc (cljs-build-opts config)
                              :optimizations :advanced
                              :output-to (str "out/" (production-js config))))
  (spit "out/public/index.html" (application-host config :production)))
