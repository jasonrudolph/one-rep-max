(defproject one "1.0.0-SNAPSHOT"
  :description "Getting Started with ClojureScript."
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [ring "1.0.0-RC1"]
                 [compojure "0.6.4"]
                 [enlive "1.0.0"]
                 [org.mozilla/rhino "1.7R3"]
                 [com.google.javascript/closure-compiler "r1592"]
                 [org.clojure/google-closure-library "0.0-790"]]
  :dev-dependencies [[jline "0.9.94"]
                     [marginalia "0.7.0-SNAPSHOT"]
                     [lein-marginalia "0.7.0-SNAPSHOT"]]
  :git-dependencies [["https://github.com/clojure/clojurescript.git"
                      "986ef5807bd1ce6e8d9f98f297923db7898dd89d"]
                     ["https://github.com/levand/domina.git"
                      "6a5fcc59a5a9edfd53e2312ef1fdad9d55e869d1"]]
  :repl-init one.sample.repl
  :source-path "src/app/clj"
  :extra-classpath-dirs [".lein-git-deps/clojurescript/src/clj"
                         ".lein-git-deps/clojurescript/src/cljs"
                         ".lein-git-deps/domina/src/cljs"
                         "src/app/cljs"
                         "src/app/shared"
                         "src/app/cljs-macros"
                         "src/lib/clj"
                         "src/lib/cljs"
                         "templates"])
