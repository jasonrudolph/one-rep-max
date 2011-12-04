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

(defmacro cljs-eval [ns & forms]
  `(do ~@(map (fn [x] `(evaluate-cljs *eval-env* (quote ~ns) (quote ~x))) forms)))

(defn pause
  ([]
     (pause 500))
  ([t]
     (Thread/sleep t)))
