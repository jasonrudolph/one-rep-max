(ns one.repmax.url-blueprint
  (:use [one.repmax.util :only [clj->js]]))

(defn- map->query-string
  "Returns a query string representation of the given map of query parameters.

   For example, the following example map of parameters:

     {:_apikey \"some-key\", :name \"some-name\"}

   Produces the following query string:

     \"_apikey=some-key&name=some-name\"
  "
  [params]
  (.toString (.createFromMap goog.Uri.QueryData (clj->js params))))

(defn- apply-segments [base-url segments]
  (reduce (fn [url-string [segment-name segment-value]]
            (let [pattern (re-pattern (str ":" (name segment-name)))]
              (clojure.string/replace url-string pattern segment-value)))
          base-url
          segments))

(defn ->url
  "Returns a URL (as a string) for the given 'URL blueprint' map.

   The format of a URL blueprint map is as follows:

   * :base-url (required) - a string containing the scheme, host, port, and
                            path, with optional placeholder 'segments'
   * :params   (optional) - a map of parameters that will appear in the query
                            string of the URL
   * :segments (optional) - a map of segment names and values that will be
                            plugged into the base-url

   Examples:

     (->url {:base-url \"http://example.com\"})

     ;;=> \"http://example.com\"

     (->url {:base-url \"http://example.com/databases/:db/collections/:collection\"
             :params   {\"_api-key\" \"secret\"}
             :segments {:db \"one-rep-max\", :collection \"sets\"}})

     ;; => \"http://example.com/databases/one-rep-max/collections/sets?_api-key=secret\"
  "
  [blueprint]
  (let [base (apply-segments (:base-url blueprint) (:segments blueprint))
        params (get blueprint :params {})]
    (if (empty? params)
      base
      (str base "?" (map->query-string params)))))
