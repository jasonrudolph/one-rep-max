(ns start.reload
  (:use [cljs.closure :only (build)])
  (:require [clojure.java.io :as io]))

(defonce last-compile (atom {}))

(defn any-modified [k files]
  (let [newest (apply max
                      (map #(.lastModified %) files))]
    (when (> newest (get @last-compile k 0))
      newest)))

(defn any-modified-cljs [dir k]
  (any-modified k (filter #(.isFile %) (file-seq (io/file dir)))))

(defn watch-cljs [handler dir opts]
  (fn [request]
    (let [k (:uri request)
          ts (any-modified-cljs dir k)]
      (when ts
        (swap! last-compile assoc k ts)
        (let [out (:output-dir opts)]
          (doseq [file (file-seq (io/file (str (:output-dir opts) "/"
                                               (:top-level-package opts))))]
            (.setLastModified file 0))
          (build dir (if (= (:uri request) "/production")
                       (assoc opts :optimizations :advanced
                              :output-to (:output-to-prod opts))
                       opts)))))
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
