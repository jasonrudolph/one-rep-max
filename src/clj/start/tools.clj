(ns start.tools
  (:use [start.config]
        [cljs.closure :only (build)]
        [start.host-page :only (application-host)])
  (:require [clojure.java.io :as io]))

(defn build-project []
  (build "src/cljs" (assoc (cljs-build-opts config)
                      :optimizations :advanced
                      :output-to (str "out/" (production-js config))))
  (spit "out/public/index.html" (application-host config {:uri "/production"})))
