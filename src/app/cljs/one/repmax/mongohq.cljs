(ns one.repmax.mongohq
  (:refer-clojure :exclude [sort])
  (:require [clojure.walk :as walk]
            [goog.json :as gjson]
            [one.browser.remote :as remote]))

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

(defn- clj->json
  "Returns a JSON string representing the given ClojureScript data structure.
   See also: clj->js"
  [x]
  (.stringify js/JSON (clj->js x)))

;;; Plumbing for handling remote requests

(def ^{:private true} request-id (atom 0))
(defn- next-request-id [] (swap! request-id inc))

(defn- map->query-string
  "Returns a query string representation of the given map of query parameters.

   For example, the following example map of parameters:

     {:_apikey \"some-key\", :name \"some-name\"}

   Produces the following query string:

     \"_apikey=some-key&name=some-name\"
  "
  [params]
  (.toString (.createFromMap goog.Uri.QueryData (clj->js params))))

(defn- with-params [url params]
  (str url "?" (map->query-string params)))

(defn- request-headers [content-type]
  (if (= content-type :json)
    {"Content-Type" "application/json"}))

(defn- request-content [content-type content]
  (if (= content-type :json)
    (gjson/serialize (clj->js content))
    content))

(defn- response-body->clj [response]
  (js->clj (JSON/parse (:body response))))

; TODO Log all requests
(defn- request
  [url & {:keys [method content content-type on-success on-error]
          :or   {method     "GET"
                 on-success (fn [data])
                 on-error   (fn [data])}}]
  (remote/request (next-request-id)
                  url
                  :method method
                  :headers (request-headers content-type)
                  :content (request-content content-type content)
                  :on-success #(on-success (response-body->clj %))
                  :on-error #(on-error ("error" (response-body->clj %)))))

(def ^{:private true} root-url "https://api.mongohq.com")

;;; Working with Databases

(def database-name "one-rep-max")

(def ^{:private true} root-databases-url (str root-url "/databases"))

(def ^{:private true} root-database-url (str root-databases-url "/" database-name))

(defn find-databases [api-key on-success on-error]
  (request (with-params root-databases-url {:_apikey api-key})
           :on-success on-success
           :on-error on-error))

(defn find-database [api-key on-success on-error]
  (request (with-params root-database-url {:_apikey api-key})
           :on-success on-success
           :on-error on-error))

;;; Working with Collections

(def ^{:private true} root-collections-url (str root-database-url "/collections"))

(defn- root-collection-url [collection-name]
  (str root-collections-url "/" collection-name))

(defn find-collections [api-key on-success on-error]
  (request (with-params root-collections-url {:_apikey api-key})
           :on-success on-success
           :on-error on-error))

(defn create-collection [api-key collection-name on-success on-error]
  (request (with-params root-collections-url {:_apikey api-key})
           :method "POST"
           :content (str "name=" collection-name)
           :on-success on-success
           :on-error on-error))

;;; Working with Documents

(defn- root-documents-url [collection-name]
  (str (root-collection-url collection-name) "/documents"))

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

   Optional keyword arguments:
   * :limit The total number of documents to fetch.
            (Defaults to infinity, which tells the function to fetch *all*
            documents in this collection.)
   * :query A Clojure map representing your MongoDB query. Uses the same
            structure as native MongoDB queries using MongoDB's 'find' comamnd.
            (Defaults to nil.)
   * :sort  A Clojure map describing how to sort the results. Uses the same
            structure as native MongoDB queries using MongoDB's 'sort' command.
            (Defaults to nil.)

   MongoHQ returns a maximum of 100 documents per request. If the given
   collection has more than 100 documents that match the given :query, and the
   given :limit is greater than 100, this function makes multiple requests to
   MongoHQ. It will make as many requests as needed to fetch the requested
   number of documents, or all documents that match the given :query,
   whichever comes first.
  "
  [api-key collection-name on-success & {:keys [limit query sort]
                                         :or   {limit js/Infinity}}]
  (let [url (root-documents-url collection-name)
        url-params {:_apikey api-key
                    :q       (clj->json query)
                    :sort    (clj->json sort)}
        url-fn (with-partial-params url url-params)]
    (find-documents-request url-fn
                            :limit-overall limit
                            :on-success on-success)))

(defn- with-partial-params
  "Takes a base URL string and a Clojure map of request parameters.

   Returns a function that takes a Clojure map of additional request parameters
   and returns a URL (as a string) containing the combined set of request
   parameters.

   For example:

    ((with-partial-params \"http://google.com\" {:q \"clojure\"}) {:hl :en})

   Produces the following URL string:

     \"http://google.com?q=clojure&hl=en\"
  "
  [url params]
  #(with-params url (conj params %)))

(def ^{:doc "The maximum number of documents that MongoHQ will return for a
             single request (i.e., the maximum allowed value for the 'limit'
             parameter)."}
  max-docs-per-request 100)

; TODO Add support for on-error callback
(defn- find-documents-request
  "Issues a request to the given URL (provided via the url-fn argument) to find documents.

   * url-fn             A function that takes a map of query parameters and returns the target
                        URL with the given query parameters. (See also: with-partial-params.)

   Optional keyword argments:
   * :skip              Used for pagination in MongoHQ. Indicates the number of documents to
                        'skip' over when determining which documents to return from a query.
                        Defaults to 0.
   * :limit-per-request The maximum number of documents to return in this request. Defaults
                        to max-docs-per-request (i.e., the maximum limit supported by MongoHQ).
   * :limit-overall     The total number of documents being requested.
   * :previous-docs     A sequence of documents produced by previous calls to this same
                        function. Since we may need to call MongoHQ multiple times to 'paginate'
                        through the list of documents, we can pass the results from previous calls
                        to subsequent calls. After the final request to MongoHQ, all results are
                        combined and passed to the on-success function.
                        Defaults to an empty Vector.
   * :on-success        The function that will be invoked upon successful completion of the
                        request. The function must accept a single argument: a sequence of
                        documents.
  "
  [url-fn & {:keys [skip limit-per-request limit-overall previous-docs on-success]
             :or   {skip 0
                    limit-per-request max-docs-per-request
                    limit-overall js/Infinity
                    previous-docs []
                    on-success (fn [data])}}]
  (request (url-fn {:skip skip, :limit limit-per-request})
           :on-success (find-documents-callback url-fn
                                                skip
                                                limit-per-request
                                                limit-overall
                                                previous-docs
                                                on-success)))

(defn- find-documents-callback
  "Returns a callback function to be invoked after a successful request to
   MongoHQ to find documents for a collection.

   Ensures that we get the requested number of documents (as specified by the
   limit-overall argument) *or* all documents in the collection (whichever is
   smaller) before we invoke the original callback provided by the caller. To do
   so, it determines whether the another request is needed in order to fetch
   more documents.

   If we have received the overall requested number or documents
   (limit-overall), *or* if this specific request returned fewer documents than
   requested (limit-per-request), then we know that we have reached the end of
   the 'resultset' for this query. In this case, we combine the previous results
   with the results from this request, and we pass the combined results to the
   original callback provided by the caller.

   On the other hand, if this request returned the number of documents
   specified in limit-per-request, then there may be other documents remaining
   in the 'resultset' for this query.  In this case, we issue a subsequent
   request to MongoHQ for the next 'page' of results in this 'resultset'. We
   continue this pattern until we have fetched the requested number of documents
   (limit-overall) or all documents in the collection (whichever is smaller).
  "
  [url-fn skip limit-per-request limit-overall previous-docs on-success]
  (fn [new-docs]
    (let [docs (concat previous-docs new-docs)
          fetched-all-docs? (or
                              (>= (count docs) limit-overall)
                              (< (count new-docs) limit-per-request))]
      (if fetched-all-docs?
        (on-success (take limit-overall (documents->maps docs)))
        (find-documents-request url-fn
                                :skip (+ skip limit-per-request)
                                :limit-per-request limit-per-request
                                :limit-overall limit-overall
                                :previous-docs docs
                                :on-success on-success)))))

(defn create-document [api-key collection-name document on-success on-error]
  (request (with-params (root-documents-url collection-name) {:_apikey api-key})
           :method "POST"
           :content-type :json
           :content {"document" document, "safe" true}
           :on-success #(on-success (walk/keywordize-keys %))
           :on-error on-error))

