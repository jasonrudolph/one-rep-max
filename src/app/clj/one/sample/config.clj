(ns one.sample.config
  "Contains configuration for the sample application."
  (:require [net.cgrand.enlive-html :as html]))

(defn- production-transform [h]
  (html/transform h
                  [:ul#navigation]
                  (html/substitute (html/html-snippet ""))))

(def ^{:doc "Configuration for the sample application."}
  config {:src-root "src"
          :app-root "src/app/cljs"
          :top-level-package "one"
          :js "public/javascripts"
          :dev-js-file-name "main.js"
          :prod-js-file-name "mainp.js"
          :dev-js ["goog.require('one.sample.core');"
                   "goog.require('one.sample.model');"
                   "goog.require('one.sample.controller');"
                   "goog.require('one.sample.history');"
                   "goog.require('one.sample.logging');"
                   "one.sample.core.start();one.sample.core.repl();"]
          :prod-js ["one.sample.core.start();"]
          :reload-clj ["/one/host_page"
                       "/one/reload"
                       "/one/templates"
                       "/one/sample/api"
                       "/one/sample/config"
                       "/one/sample/dev_server"]
          :prod-transform production-transform})
