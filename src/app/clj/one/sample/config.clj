(ns one.sample.config
  "This namespace contains the configuration for a ClojureScript One
  application."
  (:require [net.cgrand.enlive-html :as html]
            [one.reload :as reload]))

;; Set the location where all generated JavaScript will be stored.

(def public-js "public/javascripts")

;; Create transformations which will change the host page based on the
;; environment that we are running in.

(defn- production-transform [h]
  (html/transform h
                  [:ul#navigation]
                  (html/substitute (html/html-snippet ""))))

;; Configure code reloading for Clojure, ClojureScript and shared
;; code.

(def watch-clj (reload/clj-files-in ["src/app/cljs-macros" "src/app/clj" "src/lib/clj"]
                                    "host_page.clj"
                                    "templates.clj"
                                    "api.clj"
                                    "application.clj"
                                    "snippets.clj"))

(def watch-cljs (reload/cljs {:js public-js}
                             ["src/app/cljs"]
                             (reload/shared "src/app/shared")
                             ["one"]))

(def watch-templates (reload/dir-trigger "templates" watch-cljs))

;; Application configuration.

(def ^{:doc "Configuration for the sample application."}
  config {;; Something which implements the Compilable protocol. Used
          ;; by one.tools to build the production application.
          :cljs-sources watch-cljs
          ;; The location where all generated JavaScript will be
          ;; stored. Use in one.tools to determine where to output
          ;; compiled JavaScript.
          :js public-js
          :dev-js-file-name "main.js"
          :prod-js-file-name "mainp.js"
          :dev-js ["goog.require('one.sample.core');"
                   "goog.require('one.sample.model');"
                   "goog.require('one.sample.controller');"
                   "goog.require('one.sample.history');"
                   "goog.require('one.sample.logging');"
                   "one.sample.core.start();one.sample.core.repl();"]
          :prod-js ["one.sample.core.start();"]
          :prod-transform production-transform
          :reloadables [watch-clj watch-cljs watch-templates]})
