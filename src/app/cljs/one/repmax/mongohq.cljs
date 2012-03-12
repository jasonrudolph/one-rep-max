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

(defn find-documents
  "Finds all documents in the given collection. Invokes the given callback,
   passing in a sequence of documents (as Clojure maps).

   MongoHQ returns a maximum of 100 documents per request. If the given collection
   has more than 100 documents, this function makes multiple requests to MongoHQ.
   It will make as many requests as needed to fetch all the documents in the
   collection.
  "
  [collection-name on-success]
  (let [url (root-documents-url collection-name)]
    (find-documents-request url :on-success on-success)))

(defn- find-documents-request
  "Issues a JSONP request to the given URL to find documents.

   Optional keyword argments:
   * :skip            Used for pagination in MongoHQ. Indicates the number of documents to
                      'skip' over when determining which documents to return from a query.
                      Defaults to 0.
   * :limit           The maximum number of documents to return in this request.
                      Defaults to 100 (i.e., the maximum limit supported by MongoHQ).
   * previous-results A sequence of documents produced by previous calls to this same
                      function. Since we may need to call MongoHQ multiple times to 'paginate'
                      through the list of documents, we can pass the results from previous calls
                      to subsequent calls. After the final request to MongoHQ, all results are
                      combined and passed to the on-success function.
                      Defaults to an empty Vector.
   * on-success       The function that will be invoked upon successful completion of the
                      request. The function must accept a single argument: a sequence of
                      documents.
  "
  [url & {:keys [skip limit previous-results on-success]
          :or   {skip 0
                 limit 100
                 previous-results []
                 on-success (fn [data])}}]
  (jsonp-request url
                 {:_apikey *api-key* :skip skip :limit limit}
                 (find-documents-callback url
                                          skip
                                          limit
                                          previous-results
                                          on-success)))

(defn- find-documents-callback
  "Returns a callback function to be invoked after a successful request to MongoHQ to find
   documents for a collection.

   Ensures that we get *all* documents in the collection before we invoke the original
   callback provided by the caller. To do so, it determines whether the another request is
   needed in order to fetch more documents.

   If this request returned fewer documents than allowed by the given limit, then we know
   that we have reached the end of the 'resultset' for this query. In this case, we combine
   the previous results with the results from this request, and we pass the combined results
   to the original callback provided by the caller.

   On the other hand, if this request returned the number of documents specified in the
   limit, then there may be other documents remaining in the 'resultset' for this query.
   In this case, we issue a subsequent request to MongoHQ for the next 'page' of results
   in this 'resultset'. We continue this pattern until we have fetched all documents in
   the collection.
  "
  [url skip limit previous-results on-success]
  (fn [new-results]
    (let [all-results (apply conj previous-results new-results)]
      (if (< (count new-results) limit)
        (on-success (documents->maps all-results))
        (find-documents-request url
                                :skip (+ skip limit)
                                :limit limit
                                :previous-results all-results
                                :on-success on-success)))))

