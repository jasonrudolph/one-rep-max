(ns ^{:doc "Provides support for basic animations. Allows effects to be
  represented as Clojure data."}
  one.browser.animation
  (:use [one.color :only (color bg-color rgb)]
        [one.core :only (start dispose)])
  (:require [goog.style :as style]
            [goog.string :as gstring]
            [goog.fx.AnimationQueue :as queue]
            [goog.fx.easing :as easing]
            [goog.fx.dom :as anim]
            [clojure.browser.event :as event]
            [clojure.browser.dom :as dom]
            [domina :as d]))

(defn- get-element
  "Accepts a keyword, string or element and returns an
  element. Strings are assumed to be xpath format."
  [e]
  (cond (keyword? e) (d/by-id (name e))
        (string? e) (first (d/nodes (d/xpath e)))
        :else e))

(defprotocol IPosition
  (position [this] "Return the position of the passed object as a 2D array, `[X Y]`."))

(extend-protocol IPosition

  cljs.core.Vector
  (position [this] this)

  js/HTMLElement
  (position [this]
    (let [p (js->clj (style/getPosition this) :keywordize-keys true)]
      [(:x p) (:y p)])))

(defprotocol IScroll
  (scroll [this] "Return the scroll position of an element as `[X Y]`."))

(extend-protocol IScroll

  js/Number
  (scroll [this] [0 this])

  cljs.core.Vector
  (scroll [this] this)

  js/HTMLElement
  (scroll [this]
    [(.scrollLeft this) (.scrollTop this)]))

(defprotocol ISize
  (size [this] "Return the size of an element as `[W H]`.")
  (width [this] "Return the width of an element.")
  (height [this] "Return the height of an element."))

(extend-protocol ISize

  js/Number
  (size [this] [this this])
  (width [this] this)
  (height [this] this)

  cljs.core.Vector
  (size [this] this)
  (width [this] (first this))
  (height [this] (second this))

  js/HTMLElement
  (size [this]
    (let [s (js->clj (style/getSize this)
                     :keywordize-keys true)]
      [(:width s) (:height s)]))
  (width [this]
    (width (size this)))
  (height [this]
    (height (size this))))

(defprotocol IOpacity
  (opacity [this] "Return the elements current opacity."))

(extend-protocol IOpacity

  js/String
  (opacity [this]
    (js/parseFloat this))

  js/Number
  (opacity [this] this)

  js/HTMLElement
  (opacity [this]
    (opacity (style/getComputedStyle this "opacity"))))

(extend-type goog.fx.AnimationQueue
  
  one.core/Startable
  (start [this] (.play this ()))
  
  one.core/Disposable
  (dispose [this] (.dispose this ())))

(extend-type goog.fx.dom.PredefinedEffect
  
  one.core/Startable
  (start [this] (.play this ()))
  
  one.core/Disposable
  (dispose [this] (.dispose this ()))
  
  event/EventType
  (event-types [this]
    (into {}
          (map
           (fn [[k v]]
             [(keyword (. k (toLowerCase)))
              v])
           (merge (js->clj goog.fx.Animation.EventType))))))

(defmulti acceleration
  "Get the acceleration function associated with a given
  keyword. Implementing this as a multimethod allows developers to add new
  functions and still represent effects as data."
  identity :default :ease-out)

(defmethod acceleration :ease-out [name]
  easing/easeOut)

(defmethod acceleration :ease-in [name]
  easing/easeIn)

(defmethod acceleration :in-and-out [name]
  easing/inAndOut)

(defn- accel
  "Given a map which represents an effect. Return the acceleration
  function or `nil`."
  [m]
  (when-let [a (:accel m)]
    (if (fn? a)
      a
      (acceleration a))))

(defmulti effect
  "Accepts a map and returns an effect. The returned effect may be run
  or composed with other effects.

  Available effects include: `:color`, `:fade`, `:fade-in`, `:fade-out`,
  `:fade-in-and-show`, `:fade-out-and-hide`, `:slide`, `:swipe`, `:bg-color`,
  `:resize`, `:resize-width` and `:resize-height`."
  (fn [e {effect :effect}] effect))

(defmethod effect :color [element m]
  (let [start (or (:start m) element)
        end (or (:end m) element)]
    (goog.fx.dom.ColorTransform. element
                                 (apply array (rgb (color start)))
                                 (apply array (rgb (color end)))
                                 (or (:time m) 2000)
                                 (accel m))))

(comment ;; Color effect examples

  (def label (get-element "//label[@id='name-input-label']/span"))
  (def label-color (color label))

  (def red [255 0 0])
  (def green [0 255 0])

  (start (effect label {:effect :color :end red}))
  (start (effect label {:effect :color :end green}))
  (start (effect label {:effect :color :end label-color}))
  )

(defmethod effect :fade [element m]
  (goog.fx.dom.Fade. element
                     (opacity (or (:start m) element))
                     (opacity (:end m))
                     (or (:time m) 1000)
                     (accel m)))

(defmethod effect :fade-in [element m]
  (goog.fx.dom.FadeIn. element (or (:time m) 1000) (accel m)))

(defmethod effect :fade-out [element m]
  (goog.fx.dom.FadeOut. element (or (:time m) 1000) (accel m)))

(defmethod effect :fade-in-and-show [element m]
  (goog.fx.dom.FadeInAndShow. element (or (:time m) 1000) (accel m)))

(defmethod effect :fade-out-and-hide [element m]
  (goog.fx.dom.FadeOutAndHide. element (or (:time m) 1000) (accel m)))

(comment ;; Fade effect examples

  (def label (get-element "//label[@id='name-input-label']/span"))
  (def title (get-element "//div[@id='input-field']/h1"))
  (def title-opacity (opacity title))
  (def label-opacity (opacity label))

  (start (effect label {:effect :fade :end 0.2}))
  (start (effect title {:effect :fade :end label}))
  (start (effect label {:effect :fade :end label-opacity}))
  (start (effect title {:effect :fade :end title-opacity}))

  (start (effect label {:effect :fade-out}))
  (start (effect label {:effect :fade-in}))

  (start (effect label {:effect :fade-out-and-hide}))
  (start (effect label {:effect :fade-in-and-show}))
  )

(defmethod effect :bg-color [element m]
  (let [start (or (:start m) element)
        end (or (:end m) element)]
    (goog.fx.dom.BgColorTransform. element
                                   (apply array (rgb (bg-color start)))
                                   (apply array (rgb (bg-color end)))
                                   (or (:time m) 2000)
                                   (accel m))))

(comment ;; Background color effect examples

  (def input (get-element :name-input))

  (def red [255 0 0])
  (def green [0 255 0])

  (def input-bg-color (bg-color input))
  (def input-color (color input))

  (start (effect input {:effect :bg-color :end red}))
  (start (effect input {:effect :bg-color :end green}))
  (start (effect input {:effect :bg-color :end input-bg-color}))
  )

(defn- calculate-slide-end
  "Calculate the end of a slide based on the start value and the
  passed `:left`, `:right`, `:up` and `:down` values."
  [[x y] m]
  (vector (+ (- x (:left m 0)) (:right m 0))
          (+ (- y (:up m 0)) (:down m 0))))

(defmethod effect :slide [element m]
  (let [start (position (or (:start m) element))
        end (or (:end m) (calculate-slide-end start m))]
    (goog.fx.dom.Slide. element
                        (apply array start)
                        (apply array end)
                        (or (:time m) 1000)
                        (accel m))))


(comment ;; Slide effect examples
  
  (def label (get-element "//label[@id='name-input-label']/span"))
  
  (start (effect label {:effect :slide :up 40 :time 100}))
  (start (effect label {:effect :slide :down 40 :time 100}))

  ;; Easing examples
  (start (effect label {:effect :slide :up 200 :accel :ease-out}))
  (start (effect label {:effect :slide :down 200 :accel :ease-in}))
  )

(defmethod effect :resize-height [element m]
  (goog.fx.dom.ResizeHeight. element
                             (height (or (:start m) element))
                             (height (or (:end m) element))
                             (or (:time m) 1000)
                             (accel m)))

(defmethod effect :resize-width [element m]
  (goog.fx.dom.ResizeWidth. element
                            (width (or (:start m) element))
                            (width (or (:end m) element))
                            (or (:time m) 1000)
                            (accel m)))

(defmethod effect :resize [element m]
  (let [start (size (or (:start m) element))
        end (size (or (:end m) element))]
    (goog.fx.dom.Resize. element
                         (apply array start)
                         (apply array end)
                         (or (:time m) 1000)
                         (accel m))))

(comment ;; Resize examples

  (def button (get-element :greet-button))
  (def button-size (size button))
  (def button-height (height button))
  (def button-width (width button))

  (start (effect button {:effect :resize :end [200 200]}))
  (start (effect button {:effect :resize :end button-size}))

  (start (effect button {:effect :resize-height :end 200}))
  (start (effect button {:effect :resize-height :end button-height}))

  (start (effect button {:effect :resize-width :end 200}))
  (start (effect button {:effect :resize-width :end button-width}))
  )

(defmethod effect :scroll [element m]
  (let [start (or (:start m) element)
        end (:end m)]
    (goog.fx.dom.Scroll. element
                         (apply array (scroll start))
                         (apply array (scroll end))
                         (or (:time m) 1000)
                         (accel m))))

(comment ;; Scroll examples

  (def doc (get-element "//body"))

  ;; Make the window small before trying this.
  (start (effect doc {:effect :scroll :end [500 500]}))
  (start (effect doc {:effect :scroll :end [0 0]}))

  (start (effect doc {:effect :scroll :end 300}))
  (start (effect doc {:effect :scroll :end 0}))
  )

(defmethod effect :swipe [element m]
  (let [start (or (:start m) [0 0])
        end (or (:end m) element)]
    (goog.fx.dom.Swipe. element
                        (apply array (size start))
                        (apply array (size end))
                        (or (:time m) 1000)
                        (accel m))))

(comment ;; Swipe examples

  (def button (get-element :greet-button))
  (def button-size (size button))

  (style/setStyle button "position" "absolute")
  
  (start (effect button {:effect :swipe :start [100 0] :time 300}))
  (start (effect button {:effect :swipe :start [0 45] :time 300}))
  (start (effect button {:effect :swipe :time 300}))
  
  (style/setStyle button "position" "")
  )

(defn parallel
  "Cause the passed animations to run in parallel."
  [& effects]
  (let [parallel (goog.fx.AnimationParallelQueue.)]
    (doseq [effect effects] (.add parallel effect))
    parallel))

(defn serial
  "Cause the passed animations to be run in order."
  [& effects]
  (let [serial (goog.fx.AnimationSerialQueue.)]
    (doseq [effect effects]
      (.add serial effect))
    serial))

(defn bind
  "Bind effects to an element returning an animation. Accepts an HTML
  element and any number of effects. Effects can be Maps or a Vector
  of Maps. Each effect is run in order. Each effect within a Vector is
  run in parallel."
  [element & effects]
  (let [element (get-element element)
        serial (goog.fx.AnimationSerialQueue.)]
    (doseq [sequential-effect effects]
      (if (vector? sequential-effect)
        (let [parallel (goog.fx.AnimationParallelQueue.)]
          (doseq [parallel-effect sequential-effect]
            (.add parallel (effect element parallel-effect)))
          (.add serial parallel))
        (.add serial (effect element sequential-effect))))
    serial))

(comment ;; Bind examples

  (def label-color (color (get-element "//label[@id='name-input-label']/span")))
  
  (def label-up (bind "//label[@id='name-input-label']/span"
                      {:effect :color :end "#53607b" :time 200}
                      {:effect :slide :up 40 :time 200}))
  (start label-up)
  (def label-down (bind "//label[@id='name-input-label']/span"
                        [{:effect :color :end label-color :time 200}
                         {:effect :slide :down 40 :time 200}]))
  (start label-down)

  ;; Serial and parallel animations on different elements
  
  (def button (get-element :greet-button))
  (def button-size (size button))

  (def big-button (bind :greet-button {:effect :resize :end [200 200] :time 200}))
  (start big-button)

  (def small-button (bind :greet-button {:effect :resize :end button-size :time 200}))
  (start small-button)

  (start (serial label-up big-button))
  (start (serial small-button label-down))
  (start (parallel label-up big-button))
  (start (parallel small-button label-down))
  )

(comment ;; Events
  
  ;; You may listen for "begin" and "finish" events
  (def label-up (bind "//label[@id='name-input-label']/span"
                      {:effect :color :end "#53607b" :time 200}
                      {:effect :slide :up 40 :time 200}))
  (event/listen-once label-up
                     "finish"
                     #(js/alert "Anaimation finished."))
  (start label-up)
  )
