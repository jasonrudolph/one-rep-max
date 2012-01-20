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
  :repositories {"oss-sonatype-staging"
                 "https://oss.sonatype.org/content/groups/staging/"}
  :main ^:skip-aot one.sample.launchpad
  :source-path "src/app/clj"
  :extra-classpath-dirs ["lib/clojurescript/src/clj"
                         "lib/clojurescript/src/cljs"
                         "lib/domina/src/cljs"
                         "src/app/cljs"
                         "src/app/cljs-macros"
                         "src/lib/clj"
                         "src/lib/cljs"
                         "templates"])
