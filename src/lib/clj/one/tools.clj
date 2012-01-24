(ns one.tools
  "Support for building deployment artifacts for a project."
  (:use [cljs.closure :only (build)]
        [one.host-page :only (application-host)]
        [one.config :only (cljs-build-opts production-js)]
        [cljs.repl :only (repl)]
        [cljs.repl.browser :only (repl-env)])
  (:require [clojure.java.io :as io]))

(defn build-project
  "Emit both a JavaScript file containing the compiled ClojureScript
  application and the host HTML page."
  [config]
  (build (:app-root config) (assoc (cljs-build-opts config)
                              :optimizations :advanced
                              :output-to (str "out/" (production-js config))))
  (spit "out/public/index.html" (application-host config :production)))

(defn cljs-repl
  "Start a ClojureScript REPL which can connect to the development
  version of the application. The REPL will not work until the
  development page connects to it, so you will need to either open or
  refresh the development page after calling this function."
  []
  (repl (repl-env)))

(defn copy-recursive-into
  "Recursively copy the files in src to dest."
  [src dest]
  (doseq [file (remove #(.isDirectory %) (file-seq (io/file src)))]
    (let [dest-file (io/file dest file)]
      (.mkdirs (.getParentFile dest-file))
      (io/copy file dest-file))))

(defn delete
  "Delete one or more files or directories. Directories are recursively
  deleted."
  [& paths]
  (doseq [path paths
          file (reverse (file-seq (io/file path)))]
    (.delete file)))
