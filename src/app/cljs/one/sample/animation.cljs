(ns ^{:doc "Defines animations which are used in the sample
  application."}
  one.sample.animation
  (:use [one.core :only (start)]
        [one.browser.animation :only (bind parallel serial play play-animation)]
        [domina :only (by-id set-html! set-styles! destroy-children! append! single-node)]
        [domina.xpath :only (xpath)])
  (:require [goog.dom.forms :as gforms]
            [goog.style :as style]))

(def form "//div[@id='form']")
(def cloud "//div[@id='greeting']")
(def label "//label[@id='name-input-label']/span")

(def ^:private
  form-in {:effect :fade :start 0 :end 1 :time 800})

(defn initialize-views
  "Accepts the form and greeting view HTML and adds them to the
  page. Animates the form sliding in from above. This function must be
  run before any other view functions. It may be called from any state
  to reset the UI."
  [form-html greeting-html]
  (let [content (xpath "//div[@id='content']")]
    (destroy-children! content)
    (set-html! content form-html)
    (append! content greeting-html)
    ;; Required for IE8 to work correctly
    (style/setOpacity (single-node (xpath label)) 1)
    (set-styles! (xpath cloud) {:opacity "0" :display "none" :margin-top "-500px"})
    (set-styles! (by-id "greet-button") {:opacity "0.2" :disabled true})
    (play form form-in {:after #(.focus (by-id "name-input") ())})))

(comment ;; Try it

  (initialize-views (:form one.sample.view/snippets)
                    (:greeting one.sample.view/snippets))
  
  )

(defn label-move-up
  "Move the passed input field label above the input field. Run when
  the field gets focus and is empty."
  [label]
  (play label [{:effect :color :end "#53607b" :time 200}
               {:effect :slide :up 40 :time 200}]))

(defn label-fade-out
  "Make the passed input field label invisible. Run when the input
  field loses focus and contains a valid input value."
  [label]
  (play label {:effect :fade :end 0 :time 200}))

(def move-down [{:effect :fade :end 1 :time 200}
                {:effect :color :end "#BBC4D7" :time 200}
                {:effect :slide :down 40 :time 200}])

(def fade-in {:effect :fade :end 1 :time 400})

(def fade-out {:effect :fade :end 0 :time 400})

(defn label-move-down
  "Make the passed input field label visible and move it down into the
  input field. Run when an input field loses focus and is empty."
  [label]
  (play label move-down))

(comment ;; Examples of label effects.
  
  (label-move-up label)
  (label-fade-out label)
  (label-move-down label)
  )

(defn show-greeting
  "Move the form out of view and the greeting into view. Run when the
  submit button is clicked and the form has valid input."
  []
  (let [e {:effect :fade :end 0 :time 500}]
    (play-animation #(parallel (bind form e)
                               (bind label e) ; Since the label won't fade in IE
                               (bind cloud
                                     {:effect :color :time 500} ; Dummy animation for delay purposes
                                     {:effect :fade-in-and-show :time 600}))
                    {:before #(gforms/setDisabled (by-id "name-input") true)
                     ;; We need this next one because IE8 won't hide the button
                     :after #(set-styles! (by-id "greet-button") {:display "none"})})))

(defn show-form
  "Move the greeting cloud out of view and show the form. Run when the
  back button is clicked from the greeting view."
  []
  (play-animation (serial (parallel (bind cloud {:effect :fade-out-and-hide :time 500})
                                    (bind form
                                          {:effect :color :time 300} ; Dummy animation for delay purposes
                                          form-in)
                                    (bind label fade-in move-down)))
                  {;; Because IE8 won't hide the button, we need to
                   ;; toggle it between displaying inline and none
                   :before #(set-styles! (by-id "greet-button") {:display "inline"})
                   :after #(do
                             (gforms/setDisabled (by-id "name-input") false)
                             (.focus (by-id "name-input") ()))}))

(comment ;; Switch between greeting and form views

  (label-move-up label)
  (show-greeting)
  (show-form)
  )

(defn disable-button
  "Accepts an element id for a button and disables it. Fades the
  button to 0.2 opacity."
  [id]
  (let [button (by-id id)]
    (gforms/setDisabled button true)
    (play button {:effect :fade :end 0.2 :time 400})))

(defn enable-button
  "Accepts an element id for a button and enables it. Fades the button
  to an opactiy of 1."
  [id]
  (let [button (by-id id)]
    (gforms/setDisabled button false)
    (play button fade-in)))

(comment ;; Examples of all effects

  (initialize-views (:form one.sample.view/snippets)
                    (:greeting one.sample.view/snippets))
  (label-move-up label)
  (label-fade-out label)
  (show-greeting)
  (show-form)

  (disable-button "greet-button")
  (enable-button "greet-button")
  )
