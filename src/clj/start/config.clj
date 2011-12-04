(ns start.config
  (:require [net.cgrand.enlive-html :as html]))

(defn production-transform [h]
  (html/transform h
                  [:ul#navigation]
                  (html/substitute (html/html-snippet ""))))

(def config {:top-level-package "start"
             :js "public/javascripts"
             :dev-js-file-name "main.js"
             :prod-js-file-name "mainp.js"
             :dev-js ["goog.require('start.core');"
                      "goog.require('start.model');"
                      "goog.require('start.history');"
                      "goog.require('start.logging');"
                      "start.core.start();start.core.repl();"]
             :prod-js ["start.core.start();"]
             :reload-clj ["/library/host_page"
                          "/library/reload"
                          "/library/templates"
                          "/start/api"
                          "/start/config"
                          "/start/dev_server"]
             :prod-transform production-transform})
