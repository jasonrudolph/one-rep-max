(ns library.test
  "Support for evaluating ClojureScript code from Clojure tests."
  (:refer-clojure :exclude [load-file])
  (:use [cljs.compiler :only (namespaces)]
        [cljs.repl :only (evaluate-form load-file load-namespace)]))

(def ^:dynamic *eval-env*)

(defn evaluate-cljs
  "Evaluate a ClojureScript form within the given evaluation
   environment. The form will also be evaluated in the passed
   namespace which defaults to 'cljs.user."
  ([eval-env form]
     (evaluate-cljs eval-env 'cljs.user form))
  ([eval-env ns form]
     (let [env {:context :statement :locals {}}]
       (cond
        (and (seq? form) ('#{load-file clojure.core/load-file} (first form)))
        (load-file eval-env (second form))
        
        (and (seq? form) ('#{load-namespace} (first form)))
        (load-namespace eval-env (second form))
        
        :else
        (let [ret (evaluate-form env
                                 (assoc env :ns (@namespaces ns))
                                 "<testing>"
                                 form
                                 (fn [x] `(cljs.core.pr-str ~x)))]
          (try (read-string ret)
               (catch Exception e
                 (if (string? ret)
                   ret
                   nil))))))))

(defn ensure-ns-loaded
  "Ensure that that browser has completely loaded namespace ns. We
   need this because in some situations, we wind up trying to run code
   that depends on a namespace that isn't available yet, due to
   asynchrony in the browser. Returns true if the namespace loads
   within the specified timeout (roughly 60 seconds by default), and
   throws Exception otherwise."
  ([eval-env ns] (ensure-ns-loaded eval-env ns 60000))
  ([eval-env ns remaining]
     (if (pos? remaining)
       (if (evaluate-cljs eval-env (list 'boolean ns))
         true
         (do (Thread/sleep 10)
             (recur eval-env ns (- remaining 10))))
       (throw (Exception. (str "Namespace " ns " did not load before the timeout expired."))))))

(defmacro cljs-eval [ns & forms]
  `(do
     (ensure-ns-loaded *eval-env* (quote ~ns))
     ~@(map (fn [x] `(evaluate-cljs *eval-env* (quote ~ns) (quote ~x))) forms)))

(defn pause
  ([]
     (pause 500))
  ([t]
     (Thread/sleep t)))
