(ns one.repmax.config
  "Contains configuration for the One Rep Max application."
  (:use [one.reload :only [dependency clojure-reloads clojurescript-reloads watched-directory shared]])
  (:require [net.cgrand.enlive-html :as html]))

;; Set the location where all generated JavaScript will be stored.

(def public-js "public/javascripts")

;; Create transformations which will change the host page based on the
;; environment that we are running in.

(defn- production-transform [h]
  (html/transform h
                  [:#dev-links]
                  (html/substitute (html/html-snippet ""))))

;; Configure code reloading for Clojure, ClojureScript and shared
;; code.

(def clj-reloads (clojure-reloads ["src/app/clj" "src/lib/clj"]
                                  "host_page.clj"
                                  "templates.clj"
                                  "config.clj"))

(def cljs-reloads (clojurescript-reloads ["src/app/cljs"]
                                         :packages ["one"]
                                         :shared (shared "src/app/shared")
                                         :js "public/javascripts"))

(def macro-reloads (dependency (clojure-reloads ["src/app/cljs-macros"]
                                                "snippets.clj")
                               clj-reloads
                               cljs-reloads))

(def templates (watched-directory "templates" cljs-reloads))

;; Application configuration.

(def ^{:doc "Configuration for the One Rep Max application."}
  config {;; Something which implements the Compilable protocol. Used
          ;; by one.tools to build the production application.
          :cljs-sources cljs-reloads
          ;; The location where all generated JavaScript will be
          ;; stored. Use in one.tools to determine where to output
          ;; compiled JavaScript.
          :js public-js
          :dev-js-file-name "main.js"
          :prod-js-file-name "mainp.js"
          :dev-js ["goog.require('one.repmax.core');"
                   "goog.require('one.repmax.model');"
                   "goog.require('one.repmax.datastore_configuration.observer');"
                   "goog.require('one.repmax.datastore_configuration.view');"
                   "goog.require('one.repmax.exercises.observer');"
                   "goog.require('one.repmax.exercises.view');"
                   "goog.require('one.repmax.sets.observer');"
                   "goog.require('one.repmax.sets.statistics');"
                   "goog.require('one.repmax.sets.view');"
                   "goog.require('one.repmax.logging');"
                   "goog.require('one.repmax.test.integration_util');"
                   "one.repmax.core.start();one.repmax.core.repl();"]
          :prod-js ["one.repmax.core.auto_authenticate();one.repmax.core.start();"]
          :prod-transform production-transform
          :reloadables [clj-reloads cljs-reloads macro-reloads templates]})
