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

(defmulti ^:private validate
  "Accepts a form id and a value and returns a map
  with :value, :status and :error keys. Status will be set to
  either :valid or :error. If there was an error, then there will be
  an error message associated with the :error key."
  (fn [id _] id))

(defmethod validate "name-input" [_ v]
  (cond (= (count v) 0)
        {:status :empty :value v}
        (= (count v) 1)
        {:status :error :value v
         :error "Are you sure about that? Names must have at least two characters."}
        :else {:status :valid :value v}))

(defn- form-status
  "Calculates the status of the whole form based on the status of each
  field. Retuns :finished or :editing."
  [m]
  (if (every? #(= % :valid) (map :status (vals (:fields m))))
    :finished
    :editing))

(defn- set-field-value
  "Accepts a field-id and value. Validates the field and updates the
  greeting form atom."
  [field-id value]
  (swap! greeting-form
         (fn [old]
           (let [field (get (:fields old) field-id {})
                 validated (validate field-id value)
                 new (assoc-in old [:fields field-id] validated)]
             (assoc new :status (form-status new))))))

(defn- set-editing
  "Update the form state for a given field to indicate that the form
  is still being edited."
  [id]
  (swap! greeting-form
         (fn [old]
           (-> old
               (assoc-in [:fields id :status] :editing)
               (assoc :status :editing)))))

(dispatch/react-to (fn [e] (= (first e) :field-changed))
                   (fn [[_ id] value]
                     (set-field-value id value)))

(dispatch/react-to (fn [e] (= (first e) :editing-field))
                   (fn [[_ id] _] (set-editing id)))

