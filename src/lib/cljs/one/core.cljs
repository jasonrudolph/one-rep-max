(ns ^{:doc "Core ClojureScript One library."}
  one.core)

(defprotocol Startable
  (start [this]))

(defprotocol Disposable
  (dispose [this]))
