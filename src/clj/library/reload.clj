(ns library.reload
  "Clojure and ClojureScript code reloading. When any watched Clojure
   file changes, all watched Clojure files will be recompiled. If any
   ClojureScript file changes or if any template file changes, all
   ClojureScript files will be recompiled. Recompilation only happens
   on page reloads."
  (:use [cljs.closure :only (build)]
        [library.config])
  (:require [clojure.java.io :as io]))

(defonce last-compile (atom {}))

(defn any-modified [k files]
  (let [newest (apply max
                      (map #(.lastModified %) files))]
    (when (> newest (get @last-compile k 0))
      newest)))

(defn any-modified-cljs [dir k]
  (let [files (filter #(.isFile %) (into (file-seq (io/file dir))
                                         (file-seq (io/file "templates"))))]
    (pr-str files)
    (any-modified k files)))

(defn watch-cljs [handler dir config]
  (fn [request]
    (let [k (:uri request)
          ts (any-modified-cljs dir k)]
      (when ts
        (swap! last-compile assoc k ts)
        (let [build-opts (cljs-build-opts config)]
          (doseq [file (file-seq (io/file (str (:output-dir build-opts) "/"
                                               (:top-level-package config))))]
            (.setLastModified file 0))
          (build dir (if (= (:uri request) "/production")
                       (assoc build-opts :optimizations :advanced
                              :output-to (production-js config))
                       build-opts)))))
    (handler request)))

(defn any-modified-clj [files]
  (any-modified "clj"
                (map #(-> (str % ".clj")
                          (.substring 1)
                          io/resource
                          (.getFile)
                          io/file)
                     files)))

(defn reload-clj [handler files]
  (fn [request]
    (when-let [ts (any-modified-clj files)]
      (swap! last-compile assoc "clj" ts)
      (let [ns (ns-name *ns*)]
        (apply load files)))
    (handler request)))
