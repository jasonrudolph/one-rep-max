(ns ^{:doc "Observe model changes involving the datastore configuration and
           trigger side effects as appropriate."}
  one.repmax.datastore-configuration.observer
  (:require [one.dispatch :as dispatch]
            [one.repmax.cookies :as cookies]
            [one.repmax.mongohq :as mongo]))

(defmulti do-initialization-step
  (fn [& args] (first args)))

(defmethod do-initialization-step :verify-credentials [_ config]
  (mongo/find-databases config
                        #(do
                           (cookies/set-cookie :api-key (:api-key config))
                           (cookies/set-cookie :database (:database config))
                           (dispatch/fire :action {:action :datastore-configuration/credentials-verified, :configuration config}))
                        #(initialization-error-callback config %)))

(defmethod do-initialization-step :verify-database [_ config]
  (mongo/find-database config
                       #(dispatch/fire :action {:action :datastore-configuration/database-verified, :configuration config})
                       #(initialization-error-callback config %)))

(defmethod do-initialization-step :verify-collections [_ config]
  (mongo/find-collections config
                          #(find-collections-success-callback config %)
                          #(initialization-error-callback config %)))

(defmethod do-initialization-step :ready [_ _]
  (dispatch/fire :action {:action :datastore-configuration/ready}))

;; No-op. No action necessary when we transition to the :initialization-failed state.
(defmethod do-initialization-step :default [_ _])

;; TODO Enhance function to recurse through the list of necessary collections
;;      and create each one. Fire action :datastore-configuration/collections-verified.
(defn find-collections-success-callback [config collections]
  (if (contains-collection? collections "exercises") ;; TODO Get collection name from elsewhere
    (dispatch/fire :action {:action :datastore-configuration/collections-verified, :configuration config})
    (mongo/create-collection config "exercises" ;; TODO Get collection name from elsewhere
                             #(dispatch/fire :action {:action :datastore-configuration/collections-verified, :configuration config})
                             #(dispatch/fire :action {:action :datastore-configuration/initialization-failed, :configuration config}))))

(defn contains-collection? [collections collection-name]
  (some #(= collection-name (% "name")) collections))

(defn initialization-error-callback [config response]
  (dispatch/fire :action {:action :datastore-configuration/initialization-failed
                          :configuration config
                          :error response}))

;;; Register reactors

(dispatch/react-to #{:model-change}
                   (fn [_ event]
                     (let [old-config-state (-> event :old :datastore-configuration :state)
                           new-config-state (-> event :new :datastore-configuration :state)]
                       (if-not (= old-config-state new-config-state)
                         (let [config (dissoc (-> event :new :datastore-configuration) :state)]
                           (do-initialization-step new-config-state config))))))
