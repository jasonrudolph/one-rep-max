(ns one.repmax.mongohq
  (:require [goog.net.Jsonp :as jsonp]
            [clojure.walk :as walk]))

; TODO Dynamically obtain user-provided API key
(def ^:private *api-key* "SECRET")

; clj->js is from Chris Granger's excellent fetch library
; https://github.com/ibdknox/fetch/blob/30e938c/src/fetch/util.cljs
(defn- clj->js
  "Recursively transforms ClojureScript maps into Javascript objects,
   other ClojureScript colls into JavaScript arrays, and ClojureScript
   keywords into JavaScript strings."
  [x]
  (cond
    (string? x) x
    (keyword? x) (name x)
    (map? x) (.-strobj (reduce (fn [m [k v]]
                                 (assoc m (clj->js k) (clj->js v))) {} x))
    (coll? x) (apply array (map clj->js x))
    :else x))

; TODO Add support for on-error callback
(defn- jsonp-request [url params on-success]
  (let [jsonp (goog.net.Jsonp. url)]
    (.send jsonp
           (clj->js params)
           (fn [data] (on-success (js->clj data))))))

(def ^{:private true} root-url "https://api.mongohq.com/databases/one-rep-max")

(defn- root-documents-url [collection-name]
  (str root-url "/collections/" collection-name "/documents"))

(defn- simplify-object-ids
  "Takes a seq of maps, where each map is a Mongo document in raw format as
   returned by MongoHQ.

   Returns a seq of the same documents, with the value of the '_id' key converted
   to a simple string (instead of a map).

   Given the following example document in raw format:

     {'_id' {'$oid' '4f5119158a9e6d00010038e8'}, 'name' 'some name value'}

   This document would be simplified as:

     {'_id' '4f5119158a9e6d00010038e8', 'name' 'some name value'}
  "
  [documents]
  (map #(assoc % "_id" ((% "_id") "$oid")) documents))

(defn- documents->maps [documents]
  (-> documents
    simplify-object-ids
    walk/keywordize-keys))

(defn find-documents [collection-name on-success]
  (let [url (root-documents-url collection-name)]
    (jsonp-request url
                   {:_apikey *api-key*}
                   (fn [data] (on-success (documents->maps data))))))

