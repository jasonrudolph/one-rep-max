(ns one.test
  "Support for evaluating ClojureScript code from Clojure tests."
  (:refer-clojure :exclude [load-file])
  (:require [cljs.repl.browser :as browser])
  (:use [cljs.compiler :only (namespaces)]
        [cljs.repl :only (evaluate-form load-file load-namespace)]
        [clojure.java.browse :only (browse-url)]
        [cljs.repl :only (-setup -tear-down)]
        [one.dev-server :only (run-server)]))

(def ^:dynamic *eval-env*)

(defn evaluate-cljs
  "Evaluate a ClojureScript form within the given evaluation
  environment. The form will also be evaluated in the passed namespace
  which defaults to `'cljs.user`."
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
        (let [ret (evaluate-form eval-env
                                 (assoc env :ns (@namespaces ns))
                                 "<testing>"
                                 form
                                 (fn [x] `(cljs.core.pr-str ~x)))]
          (try (read-string ret)
               (catch Exception e
                 (if (string? ret)
                   ret
                   nil))))))))

(defn cljs-wait-for*
  "Using evaluation environment `eval-env` evaluate form in namespace
  `ns` in the browser until `pred` applied to the result returns `true` or
  the timeout expires. If `pred` returns logical true, returns the
  result of `pred`. Throws `Exception` if the timeout (in milliseconds)
  has expired."
  [eval-env pred ns form remaining]
  (if (pos? remaining)
    (if-let [result (pred (evaluate-cljs eval-env ns form))]
      result
      (do (Thread/sleep 10)
          (recur eval-env pred ns form (- remaining 10))))
    (throw (Exception.
            (str "Form "
                 form
                 " did not satisfy predicate before the timeout expired.")))))

(defmacro cljs-wait-for
  "Expands to a call to `cljs-wait-for*` using `*eval-env*` as the
  evaluation environment and a timeout of roughly one minute."
  [pred ns form]
  `(cljs-wait-for* *eval-env* ~pred (quote ~ns) (quote ~form) 60000))

(defn ensure-ns-loaded
  "Ensure that that browser has completely loaded namespace ns. We
   need this because in some situations, we wind up trying to run code
   that depends on a namespace that isn't available yet, due to
   asynchrony in the browser. Returns true if the namespace loads
   within the specified timeout (roughly 60 seconds by default), and
   throws `Exception` otherwise."
  ([eval-env ns] (ensure-ns-loaded eval-env ns 60000))
  ([eval-env ns remaining]
     (if (pos? remaining)
       (if (evaluate-cljs eval-env (list 'boolean ns))
         true
         (do (Thread/sleep 10)
             (recur eval-env ns (- remaining 10))))
       (throw (Exception. (str "Namespace " ns " did not load before the timeout expired."))))))

(defmacro cljs-eval
  "Evaluate forms in namespace `ns` in the evaluation environment
  `*eval-env*`."
  [ns & forms]
  `(do
     (ensure-ns-loaded *eval-env* (quote ~ns))
     ~@(map (fn [x] `(evaluate-cljs *eval-env* (quote ~ns) (quote ~x))) forms)))

(defn within-browser-env
  "Evaluate f with `one.test/*eval-env*` bound to a browser evaluation
  environment. Opens a browser window and navigates to url which
  defaults to 'http://localhost:8080/development'."
  ([f] (within-browser-env "http://localhost:8080/development" nil f))
  ([url init f]
     (let [server (run-server)
           eval-env (browser/repl-env)]
       (-setup eval-env)
       (browse-url url)
       (binding [*eval-env* eval-env]
         (when init (init))
         (f))
       (-tear-down eval-env)
       (.stop server))))

(defmacro js-ns
  "Set up the ClojureScript testing namespace. This will arrange for
  all tests to be run in the provided namespace within the provided
  evaluation environment. This must appear only once per Clojure
  namespace and before any calls to js-defn or js.

  This macro will define the vars, js-test-namespace and js-functions,
  in the calling namespace."
  [ns env-fn url]
  `(do (def ~'js-test-namespace (quote ~ns))
       (def ~'js-functions (atom []))
       (~'use-fixtures :once (partial ~env-fn ~url
                                    (fn [] (do (cljs-eval cljs.user (~'load-namespace (quote ~ns)))
                                              (doseq [f# @~'js-functions]
                                                (f#))))))))

(defn- test-namespace
  "Get the symbol for the current testing namespace."
  []
  (let [test-ns-var (symbol (str *ns*) "js-test-namespace")]
    (var-get (find-var test-ns-var))))

(defmacro js-defn
  "Define a ClojureScript function in the test namespace in the
  current JavaScript evaluation environment.

  All ClojureScript functions will be loaded before tests are run."
  [name & body]
  (let [[doc-string args & body] (if (string? (first body))
                                   body
                                   (conj body ""))]
    `(swap! ~'js-functions conj
          (fn [] (cljs-eval ~(test-namespace) (defn ~name ~args ~@body))))))

(defmacro js
  "Accepts a form and evaluates it in the current testing namespace
  and evaluation environment."
  [form]
  `(cljs-eval ~(test-namespace) ~form))

(defn browser-eval-env
  "Create and set up a browser evaluation environment. Open a browser
  to connect to this client."
  [& options]
  (let [eval-env (apply browser/repl-env options)]
    (-setup eval-env)
    eval-env))

(def ^:dynamic *eval-ns* 'cljs.user)

(defn bep-setup
  "Create the environment and start a socket listener for the
  BEP (Browser-Eval-Print).

  Valid options are :port"
  [& options]
  (alter-var-root #'*eval-env* (constantly (apply browser-eval-env options))))

(defn bep-teardown
  "Shutdown socket listener for the BEP (Browser-Eval-Print)."
  []
  (alter-var-root #'*eval-env* -tear-down))

(defn bep-in-ns
  "Switch the BEP (Browser-Eval-Print) environment to the namespace
  with the given name (a symbol)."
  [ns-name]
  (alter-var-root #'*eval-ns* (constantly ns-name)))

(defmacro bep
  "Evaluate forms in the browser."
  [& forms]
  `(evaluate-cljs *eval-env* *eval-ns* '(do ~@forms)))
