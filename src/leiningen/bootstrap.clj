(ns leiningen.bootstrap
  (:require leiningen.deps
            leiningen.git-deps))

(defn bootstrap
  "Bootstrap the project by running lein deps and lein git-deps."
  [project]
  (leiningen.deps/deps project)
  (leiningen.git-deps/git-deps project))
