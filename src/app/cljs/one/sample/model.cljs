(ns ^{:doc "Contains client-side state, validators for input fields
 and functions which react to changes made to the input fields."}
 one.sample.model
  (:require [one.dispatch :as dispatch]))

(def ^{:doc "An atom containing a map which is the application's current state."}
  state (atom {}))

(add-watch state :state-change-key
           (fn [k r o n]
             (dispatch/fire :state-change n)))

(def ^{:private true
       :doc "An atom containing the state of the greeting form and
  each of its fields."}
  greeting-form (atom {}))

(add-watch greeting-form :form-change-key
           (fn [k r o n]
             (dispatch/fire :form-change {:old o :new n})))

(defmulti ^:private new-status
  (fn [& args] (vec args)))

(def error-status
  {:status :error
   :error "Are you sure about that? Names must have at least two characters."})

(def editing-error-status
  {:status :editing-error
   :error "Names must have at least two characters."})

(defmethod new-status [:empty :focus :empty] [p e f]
  {:status :editing})

(defmethod new-status [:editing :finished :empty] [p e f]
  {:status :empty})

(defmethod new-status [:editing :change :empty] [p e f]
  {:status :editing})

(defmethod new-status [:editing :change :error] [p e f]
  {:status :editing})

(defmethod new-status [:editing :change :valid] [p e f]
  {:status :editing-valid})

(defmethod new-status [:editing :finished :error] [p e f]
  error-status)

(defmethod new-status [:editing-valid :change :error] [p e f]
  {:status :editing})

(defmethod new-status [:editing-valid :change :valid] [p e f]
  {:status :editing-valid})

(defmethod new-status [:editing-valid :finished :valid] [p e f]
  {:status :valid})

(defmethod new-status [:error :focus :error] [p e f]
  editing-error-status)

(defmethod new-status [:editing-error :change :error] [p e f]
  editing-error-status)

(defmethod new-status [:editing-error :finished :error] [p e f]
  error-status)

(defmethod new-status [:editing-error :change :valid] [p e f]
  {:status :editing-valid})

(defmethod new-status [:editing-error :change :empty] [p e f]
  {:status :editing-error})

(defmethod new-status [:editing-error :finished :empty] [p e f]
  {:status :empty})

(defmethod new-status [:valid :focus :valid] [p e f]
  {:status :editing-valid})

(defmethod new-status [:valid :finished :empty] [p e f]
  {:status :empty})

(defmethod new-status :default [p e f]
  {:status p})

(defmulti ^:private validate
  "Accepts a form id and a value and returns a map
  with `:value`, `:status`, and `:error` keys. Status will be set to
  either `:valid` or `:error`. If there was an error, then there will be
  an error message associated with the `:error` key."
  (fn [id _] id))

(defmethod validate "name-input" [_ v]
  (cond (= (count v) 0) :empty
        (= (count v) 1) :error
        :else :valid))

(defn- form-status
  "Calculates the status of the whole form based on the status of each
  field. Retuns `:finished` or `:editing`."
  [m]
  (if (every? #(or (= % :valid) (= % :editing-valid)) (map :status (vals (:fields m))))
    :finished
    :editing))

(defn- set-field-value
  "Accepts a field-id and value. Validates the field and updates the
  greeting form atom."
  [field-id type value]
  (swap! greeting-form
         (fn [old]
           (let [field (get (:fields old) field-id {})
                 field-status (assoc (new-status (-> old :fields field-id :status)
                                                 type
                                                 (validate field-id value))
                                :value value)
                 new (assoc-in old [:fields field-id] field-status)]
             (assoc new :status (form-status new))))))

(defn- set-editing
  "Update the form state for a given field to indicate that the form
  is still being edited."
  [id]
  (swap! greeting-form
         (fn [old]
           (let [field-map (-> old :fields id)
                 status (or (:status field-map) :empty)
                 field-status (new-status status
                                          :focus
                                          status)]
             (-> old
                 (assoc-in [:fields id] (assoc field-status :value (:value field-map)))
                 (assoc :status (form-status old)))))))

(dispatch/react-to (fn [e] (= (first e) :field-finished))
                   (fn [[_ id] value]
                     (set-field-value id :finished value)))

(dispatch/react-to (fn [e] (= (first e) :field-changed))
                   (fn [[_ id] value]
                     (set-field-value id :change value)))

(dispatch/react-to (fn [e] (= (first e) :editing-field))
                   (fn [[_ id] _]
                     (set-editing id)))

(dispatch/react-to #{:form-submit}
  (fn [t d]
    (let [form-data @greeting-form]
      (when (= (:status form-data) :finished)
        (dispatch/fire :greeting {:name (-> form-data :fields "name-input" :value)})))))
