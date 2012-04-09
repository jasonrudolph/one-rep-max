(defproject one "1.0.0-SNAPSHOT"
  :description "Getting Started with ClojureScript."
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [ring "1.0.0-RC1"]
                 [compojure "0.6.4"]
                 [enlive "1.0.0"]
                 [org.mozilla/rhino "1.7R3"]
                 [com.google.javascript/closure-compiler "r1592"]
                 [org.clojure/google-closure-library "0.0-790"]
                 [org.clojars.gilbertl/vimclojure "2.1.2"]]
  :dev-dependencies [[jline "0.9.94"]
                     [marginalia "0.7.0-SNAPSHOT"]
                     [lein-marginalia "0.7.0-SNAPSHOT"]]
  :git-dependencies [["https://github.com/jasonrudolph/clojurescript.git"
                      "559d1e1dd3a0ad9bb15075bb55591294c89bf1ee"]
                     ["https://github.com/levand/domina.git"
                      "76d6448f4147dc299bca42b35d22a87e8af951c3"]]
  :repl-init one.repmax.repl
  :source-path "src/app/clj"
  :extra-classpath-dirs [".lein-git-deps/clojurescript/src/clj"
                         ".lein-git-deps/clojurescript/src/cljs"
                         ".lein-git-deps/domina/src/cljs"
                         "src/app/cljs"
                         "src/app/cljs-macros"
                         "src/lib/clj"
                         "src/lib/cljs"
                         "templates"])
