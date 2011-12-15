(ns one.reload
  "Contains functions which implement Clojure and ClojureScript code
  reloading.

  When any watched Clojure file changes, all watched Clojure files
  will be recompiled. If any ClojureScript file changes or if any
  template file changes, all ClojureScript files will be recompiled.

  Recompilation only happens on page reloads."
  (:use [cljs.closure :only (build)]
        [one.config])
  (:require [clojure.java.io :as io]))

(defonce ^:private
  last-compile (atom {}))

(defn- any-modified
  [k files]
  (let [newest (apply max
                      (map #(.lastModified %) files))]
    (when (> newest (get @last-compile k 0))
      newest)))

(defn- descendants-ending-with
  "Return a seq of File objects that are descendants of dir that end
  with extension ext."
  [dir ext]
  (filter #(.endsWith (.getName %) ext) (file-seq (io/file dir))))

(defn- any-modified-cljs
  [dir k]
  (let [files (filter #(.isFile %) (into (descendants-ending-with dir ".cljs")
                                         (file-seq (io/file "templates"))))]
    (any-modified k files)))

(defn watch-cljs 
  "Ring middleware which watches dir for changes to ClojureScript
  source files and template HTML files. When changes are detected,
  recompiles only the ClojureScript and template files (not the
  Clojure files) using a build configuration derived from config."
  [handler config]
  (fn [request]
    (let [k (:uri request)
          ts (any-modified-cljs (:src-root config) k)]
      (when ts
        (swap! last-compile assoc k ts)
        (let [build-opts (cljs-build-opts config)]
          (doseq [file (file-seq (io/file (str (:output-dir build-opts) "/"
                                               (:top-level-package config))))]
            (.setLastModified file 0))
          (build (:app-root config) (if (= (:uri request) "/production")
                                      (assoc build-opts :optimizations :advanced
                                             :output-to (production-js config))
                                      build-opts)))))
    (handler request)))

(defn- any-modified-clj
  [files]
  (any-modified "clj"
                (map #(-> (str % ".clj")
                          (.substring 1)
                          io/resource
                          (.getFile)
                          io/file)
                     files)))

(defn reload-clj
  "Ring middleware which watches a list of Clojure files for changes
  and recompiles all of them when any of the files change."
  [handler files]
  (fn [request]
    (when-let [ts (any-modified-clj files)]
      (swap! last-compile assoc "clj" ts)
      (let [ns (ns-name *ns*)]
        (apply load files)))
    (handler request)))
