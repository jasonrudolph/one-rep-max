(ns one.repmax.datastore-configuration.view
  (:use [domina.xpath :only (xpath)])
  (:require-macros [one.repmax.snippets :as snippets])
  (:require [clojure.browser.event :as event]
            [domina :as d]
            [domina.css :as css]
            [one.dispatch :as dispatch]))

(def snippets (snippets/snippets))

; TODO #disable and #enable are very general-purpose DOM manipulation functions.
;      Move them to a more general-purpose namespace.
(defn disable [id]
  (d/set-attr! (d/by-id id) "disabled" "disabled"))

(defn enable [id]
  (d/remove-attr! (d/by-id id) "disabled"))

(defmulti render (fn [{:keys [message]}] (:action message)))

(defmethod render :default [event])

(defmethod render :datastore-configuration/initialize [{:keys [new]}]
  (let [datastore-configuration (:datastore-configuration new)]
    (render-datastore-configuration-view datastore-configuration)
    (render-datastore-configuration-state datastore-configuration)))

(defmethod render :datastore-configuration/credentials-verified [{:keys [new]}]
  (render-datastore-configuration-state (:datastore-configuration new)))

(defmethod render :datastore-configuration/database-verified [{:keys [new]}]
  (render-datastore-configuration-state (:datastore-configuration new)))

(defmethod render :datastore-configuration/collections-verified [{:keys [new]}]
  (render-datastore-configuration-state (:datastore-configuration new)))

(defmethod render :datastore-configuration/initialization-failed [{:keys [new]}]
  (render-datastore-configuration-state (:datastore-configuration new)))

(defn render-datastore-configuration-view [datastore-configuration]
  (let [header (d/by-id "header")
        content (d/by-id "content")]
    (d/swap-content! header (:datastore-configuration-header snippets))
    (d/swap-content! content (:datastore-configuration-form snippets))
    (d/set-value! (d/by-id "api-key-input") (:api-key datastore-configuration))
    (event/listen (d/by-id "datastore-configuration-form-button")
                  "click"
                  #(dispatch/fire :action
                                  {:action :datastore-configuration/update
                                   :api-key (d/value (d/by-id "api-key-input"))}))))

(defmulti render-datastore-configuration-state
  (fn [datastore-configuration] (:state datastore-configuration)))

(defmethod render-datastore-configuration-state :obtain-credentials [config]
  (let [steps (css/sel "ul.progress-list > li")]
    (d/set-attr! steps "data-status" "pending")
    (enable-datastore-configuration-form)))

; TODO Render error message in (-> config :error :text)
(defmethod render-datastore-configuration-state :initialization-failed [config]
  (let [failed-step (-> config :error :occured-in-state)
        failed-step-id (str "step-" (name failed-step))]
    (render-datastore-configuration-progress failed-step-id :failure)))

(defmethod render-datastore-configuration-state :default [config]
  (let [state (:state config)
        current-step-id (str "step-" (name state))]
    (render-datastore-configuration-progress current-step-id :working)))

(defn render-datastore-configuration-progress
  "Update the view to reflect the status of the datastore initialization
  workflow, based on the given status of the given step:

  * Find steps that _precede_ the given step and mark them as having
    completed successfully.

  * Find steps that _follow_ the given step and mark them as pending.

  * Finds the given step and mark it as 'working' (i.e., in progress).

  * If the given status is :working (i.e., initialization is currently
    in progress, disable the form elements. Otherwise, enable the form
    elements (so that the user can edit the configuration and kick-off
    the initialization workflow."
  [current-step-id status]
  (let [current-step (d/by-id current-step-id)
        preceding-steps (xpath current-step "./preceding-sibling::*")
        following-steps (xpath current-step "./following-sibling::*")]
    (d/set-attr! preceding-steps "data-status" "success")
    (d/set-attr! following-steps "data-status" "pending")
    (if (= :working status)
      (do
        (d/set-attr! current-step "data-status" "working")
        (disable-datastore-configuration-form))
      (do
        (d/set-attr! current-step "data-status" "failure")
        (enable-datastore-configuration-form)))))

(defn disable-datastore-configuration-form []
  (disable "api-key-input")
  (disable "datastore-configuration-form-button"))

(defn enable-datastore-configuration-form []
  (enable "api-key-input")
  (enable "datastore-configuration-form-button"))

;;; Register reactors

(dispatch/react-to #{:model-change}
                   (fn [_ event] (render event)))

