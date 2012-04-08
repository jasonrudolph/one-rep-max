(ns ^{:doc "Observe model changes involving the datastore configuration and
           trigger side effects as appropriate."}
  one.repmax.datastore-configuration.observer
  (:require [one.dispatch :as dispatch]
            [one.repmax.cookies :as cookies]
            [one.repmax.mongohq :as mongo]))

(defmulti do-initialization-step
  (fn [& args] (first args)))

(defmethod do-initialization-step :verify-credentials [_ api-key]
  (mongo/find-databases api-key
                        #(do
                           (cookies/set-cookie :api-key api-key)
                           (dispatch/fire :action {:action :datastore-configuration/credentials-verified, :api-key api-key}))
                        #(initialization-error-callback api-key %)))

(defmethod do-initialization-step :verify-database [_ api-key]
  (mongo/find-database api-key
                       #(dispatch/fire :action {:action :datastore-configuration/database-verified, :api-key api-key})
                       #(initialization-error-callback api-key %)))

(defmethod do-initialization-step :verify-collections [_ api-key]
  (mongo/find-collections api-key
                          #(find-collections-success-callback api-key %)
                          #(initialization-error-callback api-key %)))

(defmethod do-initialization-step :ready [_ api-key]
  (dispatch/fire :action {:action :datastore-configuration/ready}))

;; No-op. No action necessary when we transition to the :initialization-failed state.
(defmethod do-initialization-step :default [_ _])

;; TODO Enhance function to recurse through the list of necessary collections
;;      and create each one. Fire action :datastore-configuration/collections-verified.
(defn find-collections-success-callback [api-key collections]
  (if (contains-collection? collections "exercises") ;; TODO Get collection name from elsewhere
    (dispatch/fire :action {:action :datastore-configuration/collections-verified, :api-key api-key})
    (mongo/create-collection api-key "exercises" ;; TODO Get collection name from elsewhere
                             #(dispatch/fire :action {:action :datastore-configuration/collections-verified, :api-key api-key})
                             #(dispatch/fire :action {:action :datastore-configuration/initialization-failed, :api-key api-key}))))

(defn contains-collection? [collections collection-name]
  (some #(= collection-name (% "name")) collections))

(defn initialization-error-callback [api-key response]
  (dispatch/fire :action {:action :datastore-configuration/initialization-failed
                          :api-key api-key
                          :error response}))

;;; Register reactors

(dispatch/react-to #{:model-change}
                   (fn [_ event]
                     (let [old-config-state (-> event :old :datastore-configuration :state)
                           new-config-state (-> event :new :datastore-configuration :state)]
                       (if-not (= old-config-state new-config-state)
                         (do-initialization-step new-config-state (-> event :new :datastore-configuration :api-key))))))
