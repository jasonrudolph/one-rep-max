(defproject one-rep-max "0.0.1"
  :description "To some extent: A ClojureScript + MongoHQ app for tracking workout data. But mostly: Some dude on the internet trying his hand at ClojureScript."
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [ring "1.0.0-RC1"]
                 [compojure "0.6.4"]
                 [enlive "1.0.0"]
                 [org.mozilla/rhino "1.7R3"]
                 [goog-jar "1.0.0"]
                 [com.google.javascript/closure-compiler "r1592"]
                 [org.clojure/google-closure-library "0.0-790"]
                 [org.clojars.gilbertl/vimclojure "2.1.2"]]
  :dev-dependencies [[jline "0.9.94"]
                     [marginalia "0.7.0-SNAPSHOT"]
                     [lein-marginalia "0.7.0-SNAPSHOT"]]
  :git-dependencies [["https://github.com/clojure/clojurescript.git"
                      "3ea593825f60c228f6a384be52bcf2fc4e417567"]
                     ["https://github.com/levand/domina.git"
                      "a328080ca4a754e808454f589caae90fac951d10"]]
  :repl-init one.repmax.repl
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
