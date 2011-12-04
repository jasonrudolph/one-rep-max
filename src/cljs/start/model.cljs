(ns start.model
  (:use [library.browser.remote :only (request)])
  (:require [cljs.reader :as reader]
            [clojure.browser.event :as event]
            [library.dispatch :as dispatch]
            [goog.uri.utils :as uri]))

(def state (atom nil))

(add-watch state :state-change
           (fn [k r o n]
             (dispatch/fire :state-change n)))

(defmulti action :type)

(defmethod action :form [_]
  (reset! state {:state :form}))

(defn host []
  (uri/getHost (.toString window.location ())))

(defn remote [f data on-success]
  (request f (str (host) "/remote")
           :method "POST"
           :on-success #(on-success (reader/read-string (:body %)))
           :on-error #(swap! state assoc :error "Error communicating with server.")
           :content (str "data=" (pr-str {:fn f :args data}))))

(defn add-name-callback [name response]
  (swap! state (fn [old]
                 (assoc (assoc old :state :greeting :name name)
                   :exists (boolean (:exists response))))))


(defmethod action :greeting [{name :name}]
  (if (empty? name)
    (swap! state assoc :error "I can't greet you without knowing your name!")
    (remote :add-name {:name name} #(add-name-callback name %))))

(dispatch/respond-to #{:form :greeting}
                     (fn [t d] (action (assoc d :type t))))
