(ns library.config)

(defn cljs-build-opts [config]
  {:output-to (str (:js config) "/" (:dev-js-file-name config))
   :output-dir (str (:js config) "/out")})

(defn production-js [config]
  (str (:js config) "/" (:prod-js-file-name config)))
