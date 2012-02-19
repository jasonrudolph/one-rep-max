(ns one.tools
  "Support for building deployment artifacts for a project."
  (:use [cljs.closure :only (build)]
        [one.host-page :only (application-host)]
        [cljs.repl :only (repl)]
        [cljs.repl.browser :only (repl-env)]
        [one.core :only (*configuration*)])
  (:require [clojure.java.io :as io]))

(defn- cljs-build-opts
  "Return output directory options."
  []
  {:output-to (str (:js *configuration*) "/" (:dev-js-file-name *configuration*))
   :output-dir (str (:js *configuration*) "/out")
   :libs (:libs *configuration*)
   :externs (:externs *configuration*)
   :foreign-libs (:foreign-libs *configuration*)})

(defn- production-js
  "Return the path to the production Javascript file."
  []
  (str (:js *configuration*) "/" (:prod-js-file-name *configuration*)))

(defn build-project
  "Emit both a JavaScript file containing the compiled ClojureScript
  application and the host HTML page."
  []
  (build (:cljs-sources *configuration*) (assoc (cljs-build-opts)
                                  :optimizations :advanced
                                  :output-to (str "out/" (production-js))))
  (spit "out/public/index.html" (application-host :production)))

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
