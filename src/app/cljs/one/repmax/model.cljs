(ns one.repmax.model
  (:refer-clojure :exclude [set])
  (:require [clojure.string :as string]
            [one.dispatch :as dispatch]
            [one.repmax.cookies :as cookies]
            [one.repmax.mongohq :as mongo]))

(def initial-state {:state :start
                    :datastore-configuration {:state :obtain-credentials, :api-key ""}
                    :exercises nil
                    :exercise-search {:query nil, :exercise-ids nil}})

(def ^{:doc "An atom containing a map which is the application's current state."}
  state (atom initial-state))

(defn state-map-for-model-change-message
  "Given a map that represents the application's state, return a map that is
  customized for communicating the application state in a :model-change
  message.

  It is unnecessary for every model-change message to include the entire
  collection of exercises. Doing so also results in enormous messages, which
  noticably degrades application performance. So, for the purposes of the
  :model-change message, we remove the :exercises from the state map."
  [m]
  (dissoc m :exercises))

(add-watch state :state-change-key
           (fn [_ _ o n]
             (dispatch/fire :model-change
                            {:old (state-map-for-model-change-message o)
                             :new (state-map-for-model-change-message n)
                             :message (:last-message n)})))

;;;; Receiving events that update state

(dispatch/react-to #{:action}
                   (fn [_ d] (receive-action-message d)))

;; TODO Add docs => State only changes as the result of an action message
(defn receive-action-message [message]
  (swap! state apply-message message))

(defn apply-message [state message]
  (-> state
    (update-model message)
    (assoc :last-message message)))

(defmulti update-model (fn [state message] (:action message)))

;;;; Managing the Datastore Configuration

; The :datastore-configuration map contains the following elements:
;
;    1. :api-key => the user-provided API key for use in accessing MongoHQ
;    2. :state => the overall state of the datastore configuration (i.e.,
;       the state of the datastore initialization/verification process)
;    3. :error => a map containing a description of the error that occured
;       during the initialization process (in the :text key) and the state
;       in which the error occured (in the :occured-in-state key); this map
;       is only present if an error has occured
;
;  In a successful progression through the initialization process, the
;  atom will move through the following states:
;
;    :obtain-credentials ;; => the start state; waiting for API key
;    :verify-credentials ;; => API key present; need to verify API key works
;    :verify-database    ;; => API key verified; need to verify database exists
;    :verify-collections ;; => database present; need to verify or create collections
;    :ready              ;; => collections verified; datastore ready for use
;
;  If any step fails along the way, the state changes to :initialization-failed.

(defmethod update-model :datastore-configuration/initialize [state _]
  (-> state
    (assoc :state :datastore-configuration)
    (assoc :datastore-configuration (datastore-configuration-from-cookies))))

(defn datastore-configuration-from-cookies []
  (let [api-key (cookies/get-cookie :api-key)]
    (if (nil? api-key)
      (:datastore-configuration initial-state)
      (datastore-configuration-for-new-api-key api-key))))

(defn datastore-configuration-for-new-api-key [api-key]
  {:api-key api-key :state :verify-credentials})

(defmethod update-model :datastore-configuration/update [state {:keys [api-key]}]
  (assoc state :datastore-configuration (datastore-configuration-for-new-api-key api-key)))

(defmethod update-model :datastore-configuration/credentials-verified [state _]
  (assoc-in state [:datastore-configuration :state] :verify-database))

(defmethod update-model :datastore-configuration/database-verified [state _]
  (assoc-in state [:datastore-configuration :state] :verify-collections))

(defmethod update-model :datastore-configuration/collections-verified [state _]
  (assoc-in state [:datastore-configuration :state] :ready))

(defmethod update-model :datastore-configuration/initialization-failed [state {:keys [error]}]
  (let [previous-datastore-configuration-state (-> state :datastore-configuration :state)]
    (-> state
      (assoc-in [:datastore-configuration :state] :initialization-failed)
      (assoc-in [:datastore-configuration :error] {:text error, :occured-in-state previous-datastore-configuration-state}))))

;;; Managing the Exercise List

(defmethod update-model :datastore-configuration/ready [state _]
  (assoc state :state :exercise-list))

(defmethod update-model :exercises/initialized-from-datastore [state {:keys [exercises]}]
  (assoc state :exercises exercises))

(defmethod update-model :exercises/search [state {:keys [name]}]
  (let [search-results (find-exercises name (:exercises state))
        exercise-ids (map #(:_id %) search-results)]
    (assoc state :exercise-search {:query name, :exercise-ids exercise-ids})))

(defn find-exercises [name exercises]
  (if (empty? name)
    exercises
    (filter-by-attribute exercises :name name)))

(defn find-exercise [id exercises]
  (first (filter #(= id (:_id %)) exercises)))

(defn filter-by-attribute [s attribute-name value-like]
  (let [normalize #(string/lower-case %)
        pattern (re-pattern (str ".*" (normalize value-like) ".*"))]
    (filter #(re-find pattern (normalize (attribute-name %))) s)))

;;; Managing the 'New Set' Form

(defmethod update-model :new-set/new [state {:keys [exercise-id]}]
  (let [exercise (find-exercise exercise-id (:exercises state))]
    (-> state
      (assoc :state :new-set)
      (assoc :new-set {:exercise exercise, :history []}))))

(defmethod update-model :new-set/history-initialized [state {:keys [set-history]}]
  (-> state
    (assoc-in [:new-set :history] set-history)))

(defmethod update-model :new-set/create [state {:keys [weight reps]}]
  (-> state
    (assoc-in [:new-set :weight] weight)
    (assoc-in [:new-set :reps] reps)
    (assoc-in [:new-set :state] :ready-to-persist)))

(defmethod update-model :new-set/persisted [state message]
  (let [set (set-map-for-exercise-history (:set message))]
    (-> state
      (update-in [:new-set :history] #(conj % set))
      (assoc-in [:new-set :weight] nil)
      (assoc-in [:new-set :reps] nil)
      (assoc-in [:new-set :state] :ready-for-new-set))))

(defmethod update-model :new-set/create-failed [state {:keys [error]}]
  (-> state
    (assoc-in [:new-set :state] :create-failed)
    (assoc-in [:new-set :error] error)))

(defn set-map-for-exercise-history
  "Given a map that represents all data for a single exercise set, return a map
  containing the data that we want to store in the state atom when this
  exercise set is added to the list of historical sets for this exercise.

  Since we always know that we are viewing the set history for a specific
  exercise, it is both unnecessary and inefficient to store the exercise ID
  inside the map that describes the set. So, do not include the :exercise-id
  entry in the returned map."
  [m]
  (dissoc m :exercise-id))

