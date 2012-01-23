(ns script.build
  (:require [clojure.java.io :as io]
            [one.tools :as tools]
            [one.sample.config :as config]))

(defn -main []
  (println "Creating out/public...")
  (.mkdir (io/file "out"))
  (tools/copy-recursive-into "public" "out")
  (tools/delete "out/public/index.html"
                "out/public/design.html"
                "out/public/javascripts")
  (.mkdir (io/file "out/public/javascripts"))
  (println "Create advanced compiled JavaScript...")
  (tools/build-project config/config))
