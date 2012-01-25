(ns ^{:doc "Make network requests.

  Adapted from Bobby Calderwood's Trail framework:
  <https://github.com/bobby/trail>"}
  one.browser.remote
  (:require [cljs.reader :as reader]
            [clojure.browser.net :as net]
            [clojure.browser.event :as event]
            [goog.net.XhrManager :as manager]))

(def ^:private responders (atom {}))

(defn- add-responders [id success error]
  (when (or success error)
    (swap! responders assoc id {:success success :error error})))

(extend-type goog.net.XhrManager

  event/EventType
  (event-types [this]
    (into {}
          (map
           (fn [[k v]]
             [(keyword (. k (toLowerCase)))
              v])
           (js->clj goog.net.EventType)))))

(def ^:private
  *xhr-manager*
  (goog.net.XhrManager. nil
                        nil
                        nil
                        0
                        5000))

(defn request
  "Asynchronously make a network request for the resource at url. If
  provided via the `:on-success` and `:on-error` keyword arguments, the
  appropriate one of `on-success` or `on-error` will be called on
  completion. They will be passed a map containing the keys `:id`,
  `:body`, `:status`, and `:event`. The entry for `:event` contains an
  instance of the `goog.net.XhrManager.Event`.

  Other allowable keyword arguments are `:method`, `:content`, `:headers`,
  `:priority`, and `:retries`. `:method` defaults to \"GET\" and `:retries`
  defaults to `0`."
  [id url & {:keys [method content headers priority retries on-success
                    on-error]
             :or   {method   "GET"
                    retries  0}}]
  (try
    (add-responders id on-success on-error)
    (.send *xhr-manager*
           id
           url
           method
           content
           headers
           priority
           ;; This next one is a callback, and we could use it to get
           ;; rid of the atom and figure out success/failure ourselves
           nil
           retries)
    (catch js/Error e
      nil)))

(defn- response-success [e]
  (when-let [{success :success} (get @responders (:id e))]
    (success e)
    (swap! responders dissoc (:id e))))

(defn- response-error [e]
  (when-let [{error :error} (get @responders (:id e))]
    (error e)
    (swap! responders dissoc (:id e))))

(defn- response-received
  [f e]
  (f {:id     (.-id e)
      :body   (. e/xhrIo (getResponseText))
      :status (. e/xhrIo (getStatus))
      :event  e}))

(event/listen *xhr-manager* "success" (partial response-received response-success))
(event/listen *xhr-manager* "error"   (partial response-received response-error))
