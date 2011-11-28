(ns ^{:doc "Taken from Bobby Calderwood's Trail framework."}
  library.browser.remote
  (:require [cljs.reader :as reader]
            [clojure.browser.net :as net]
            [clojure.browser.event :as event]
            [goog.net.XhrManager :as manager]))

(def responders (atom {}))

(defn add-responders [id success error]
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

(def ^{:private true}
  *xhr-manager*
  (goog.net.XhrManager. nil
                        nil
                        nil
                        0
                        5000))

(defn request
  [id url & {:keys [method content headers priority retries on-success on-error]
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
           nil
           retries)
    (catch js/Error e
      nil)))

(defmulti response-success :id)

(defmethod response-success :default [e]
  (when-let [{success :success} (get @responders (:id e))]
    (success e)
    (swap! responders dissoc (:id e))))

(defmulti response-error :id)

(defmethod response-error :default [e]
  (when-let [{error :error} (get @responders (:id e))]
    (error e)
    (swap! responders dissoc (:id e))))

(defn- response-received
  [type e]
  ((if (= type :success)
     response-success
     response-error)
   {:id     (.id e)
    :body   (. e/xhrIo (getResponseText))
    :status (. e/xhrIo (getStatus))
    :event  e}))

(event/listen *xhr-manager* "success" (partial response-received :success))
(event/listen *xhr-manager* "error"   (partial response-received :error))
