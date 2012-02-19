(ns one.reload
  "Contains protocols and functions which implement the default
  Clojure and ClojureScript code reloading as well as a Ring
  middleware for triggering reloading.

  Any reloading strategy can be used by providing your own
  implementations of ISources and IReloadable.

  The provided default implementations support reloading all files in
  a group when any of the files in that group change. Group
  granularity is configurable.

  Reloading/Compilation or shared Clojure and ClojureScript files is
  also supported."
  (:use [cljs.closure :only (build -compile Compilable)]
        [cljs.compiler :only (to-target-file)])
  (:require [clojure.java.io :as io]))

;; Reloading Protocols
;; ===================

(defprotocol ISources
  (files [this] "Returns a list of files."))

(defprotocol IReloadable
  (group-id [this request] "Returns a unique group-id for this reloadable.")
  (reload [this request] "Reloads this kind of reloadable."))

;; Provided ISource Implementations
;; ================================

(defn reload-clj
  "Reload a Clojure files which may be passed as a string or as a file
  object."
  [f]
  (cond (string? f) (load-file f)
        (= (type f) java.io.File) (load-file (.getAbsolutePath f))
        :else nil))

(defn reload-clj-seq
  "Reload a sequence of Clojure files."
  [files]
  (doseq [f files]
    (reload-clj f)))

(defn- by-extension
  "Create a predicate function which will identify files by their
  extension."
  [ext]
  (fn [file] (.endsWith (.getName file) ext)))

(defn by-file-name
  "Create a predicate function which will identify files by their
  name."
  [names]
  (fn [file] (contains? (set names) (.getName file))))

(defn files-in-dir
  "Return a sequence of all files in a directory. This sequence will
  not include directories."
  [dir]
  (filter #(.isFile %) (file-seq (io/file dir))))

(defn- filter-descendants
  "Filter all files under the provided directory using the provided
  predicate function."
  [dir pred]
  (filter pred (files-in-dir dir)))

(defn- filter-all-descendants
  "Filter all files under all provided directories using the provided
  predicate function."
  [dirs pred]
  (flatten (map #(filter-descendants % pred) dirs)))

(defn- all-descendants-ending-with
  "Filter all files under all provided directores which end with the
  provided extension."
  [dirs ext]
  (filter-all-descendants dirs (by-extension ext)))

;; Implement ISources for multiple directores. The pred function is
;; used to filter the returned files.

(defrecord Directories [dirs pred]
  ISources
  (files [this]
    (filter-all-descendants dirs pred)))

;; Implement ISources and Compilable for multiple ClojureScript source
;; directories. The directores may contain non-ClojureScript files,
;; only *.cljs files will be returned and compiled. Implementing
;; Compilable allows ClojureScript files from multiple directories to
;; be compiled as one unit.

(defrecord ClojureScriptDirs [dirs]
  ISources
  (files [this]
    (all-descendants-ending-with dirs ".cljs"))
  Compilable
  (-compile [_ opts]
    (flatten (map #(-compile % opts) dirs))))

(defn rename-to-js
  "Rename any Clojure-based file to a JavaScript file."
  [file-str]
  (clojure.string/replace file-str #".clj\w*$" ".js"))

(comment ;; try it

  (rename-to-js "file.clj")
  (rename-to-js "file.cljs")
  (rename-to-js "file.cljx")
  )

(defn- js-file-name
  "Given a root directory and a file name, return the relative path
  for the JavaScript file that this source file will be compiled to."
  [dir file]
  (let [d (io/file dir)
        target ""]
    (.substring (rename-to-js (.getPath (to-target-file d target file))) 1)))

;; Implement ISources and Compilable for filtered sources from a
;; single directory. This implementation expects to get Clojure files
;; which will both be reloaded in the Clojure environment and compiled
;; to JavaScript. This allows code to be shared between Clojure and
;; ClojureScript and for this to work with code reloading.

(defrecord Shared [dir pred]
  ISources
  (files [this]
    (filter-descendants dir pred))
  Compilable
  (-compile [this opts]
    (let [fs (files this)]
      (reload-clj-seq fs)
      (let [d (io/file dir)]
        (flatten (map #(-compile % (assoc opts :output-file (js-file-name d %)))
                      fs))))))

;; Provided IReloadable Implementations
;; ====================================

;; Implement Clojure code reloading.

(defrecord ClojureReloadable [sources]
  IReloadable
  (group-id [_ request] ::clj)
  (reload [_ _]
    (doseq [source sources]
      (reload-clj-seq (files source))))
  ISources
  (files [_]
    (flatten (map files sources))))

(defn- jstr
  "Use the :js location provided in opts to construct a path to a
  JavaScript file."
  [opts & paths]
  (apply str (:js opts) paths))

(defn cljs-compilation-options
  "Create ClojureScript build options based on the current
  environment. This will always add the :output-to key to the options
  map.

  The default file names for development, production and fresh
  environments are main.js, mainp.js and fresh.js. You may provide
  your own naming scheme by creating a function of the request and
  adding it to the options map under the :js-file-name-fn key."
  [{:keys [js-file-name-fn] :as opts} request]
  (let [build-opts (assoc opts :output-dir (jstr opts "/out"))
        file-name (when js-file-name-fn (js-file-name-fn request))]
    (cond (= (:uri request) "/production")
          (assoc build-opts :optimizations :advanced
                 :output-to (jstr opts "/" (or file-name "mainp.js")))
          (= (:uri request) "/fresh")
          (assoc build-opts
            :output-to (jstr opts "/" (or file-name "fresh.js")))
          :default
          (assoc build-opts
            :output-to (jstr opts "/" (or file-name "main.js"))))))

(comment ;; try it

  (cljs-compilation-options {:js "x/js"} {:uri "/development"})
  (cljs-compilation-options {:js "x/js"} {:uri "/production"})
  (cljs-compilation-options {:js "x/js"} {:uri "/fresh"})

  (defn js-file-name-fn [request]
    (case (:uri request)
      "/development" "dev.js"
      "/fresh" "fresh.js"
      "prod.js"))

  (cljs-compilation-options {:js "x/js" :js-file-name-fn js-file-name-fn}
                            {:uri "/development"})
  (cljs-compilation-options {:js "x/js" :js-file-name-fn js-file-name-fn}
                            {:uri "/production"})
  (cljs-compilation-options {:js "x/js" :js-file-name-fn js-file-name-fn}
                            {:uri "/fresh"})

  )

;; Implement ClojureScript reloading.

(defrecord ClojureScriptReloadable [opts sources top-level-pkgs]
  IReloadable
  (group-id [_ request] [::cljs (:uri request)])
  (reload [this request]
    (let [build-opts (cljs-compilation-options opts request)]
      (doseq [pkg top-level-pkgs]
        (doseq [file (files-in-dir (jstr opts "/out/" pkg))]
          (.setLastModified file 0)))
      (build this build-opts)))
  ISources
  (files [_]
    (flatten (map files sources)))
  Compilable
  (-compile [_ opts]
    (flatten (map #(-compile % opts) sources))))

;; Create an IReloadabel which can trigger the reload of other
;; reloadables.

(defrecord DirDependency [source reloadables]
  IReloadable
  (group-id [_ request] [::dir (:dirs source) (:uri request)])
  (reload [_ request]
    (doseq [reloadable reloadables]
      (reload reloadable request)))
  ISources
  (files [_] (files source)))

;; Sugar
;; =====

(defn clj-files-in [dirs & files]
  (let [clj (->Directories dirs (by-file-name (vec files)))]
    (->ClojureReloadable [clj])))

(defn shared
  ([dir]
     (shared dir (constantly true)))
  ([dir pred]
     (->Shared dir pred)))

(defn cljs
  ([opts dirs pkgs]
     (cljs opts dirs nil pkgs))
  ([opts dirs shared pkgs]
     (let [sources (->ClojureScriptDirs dirs)
           sources (if shared (conj [shared] sources) [sources])]
       (->ClojureScriptReloadable opts sources pkgs))))

(defn dir-trigger [dir reloadable]
  (let [dir-source (->Directories [dir] (constantly true))]
    (->DirDependency dir-source [reloadable])))

;; Examples
;; ========

(comment

  (def clj-dirs (->Directories ["src/app/cljs-macros" "src/app/clj" "src/lib/clj"]
                               (by-file-name ["host_page.clj"
                                              "templates.clj"
                                              "api.clj"
                                              "application.clj"
                                              "snippets.clj"])))
  (files clj-dirs)

  (def clj-r (->ClojureReloadable [clj-dirs]))
  (group-id clj-r {})
  (reload clj-r {})
  (files clj-r)

  (def shared-dir (->Shared "src/app/shared" (constantly true)))
  (files shared-dir)

  (def cljs-dirs (->ClojureScriptDirs ["src/app/cljs"]))
  (files cljs-dirs)

  (def cljs-r (->ClojureScriptReloadable {:js "public/javascripts"}
                                         [shared-dir cljs-dirs]
                                         ["one"]))
  (group-id cljs-r {:uri "/development"})
  (reload cljs-r {:uri "/development"})
  (files cljs-r)

  (def templates-dir (->Directories ["templates"] (constantly true)))
  (files templates-dir)

  (def templates-r (->DirDependency templates [cljs-r]))
  (group-id templates-r {:uri "/development"})
  (reload templates-r {:uri "/development"})
  (files templates-r)

  ;; With Sugar

  (def watch-clj (clj-files-in ["src/app/cljs-macros" "src/app/clj" "src/lib/clj"]
                               "host_page.clj"
                               "templates.clj"
                               "api.clj"
                               "application.clj"
                               "snippets.clj"))
  (def watch-cljs (cljs {:js "public/javascripts"}
                        ["src/app/cljs"]
                        (shared "src/app/shared")
                        ["one"]))
  (-compile watch-cljs {:output-dir "public/javascripts/out"
                        :output-to "public/javascripts/test.js"})
  (def watch-templates (dir-trigger "templates" watch-cljs))

  )

;; Wrap-reload Middleware
;; ======================

(defonce ^:private
  last-compile (atom {}))

(defn- any-modified
  "Given a group-id and a list of files, determine if any of the files
  have a more recent modification time than the last modification time
  for that group. If the modification time is more recent, return the
  the timestamp, otherwise, return nil."
  [group-id files]
  (let [newest (apply max
                      (map #(.lastModified %) files))]
    (when (> newest (get @last-compile group-id 0))
      newest)))

(defn wrap-reload
  "Ring middleware which will reload Clojure and ClojureScript files
  which have changed. Groups of resources are reloaded together. What
  defines a group and how that group is reloaded depends on how the
  ISource and IReloadable protocols are implemented.

  This function takes any number or relaodables as a sequence and will
  reload them in the order supported by that sequence.

  The passed reload-pred function will be used to determine if we
  should attempt to reload any files."
  [handler reload-pred reloadables]
  (fn [request]
    (when (reload-pred request)
      (doseq [reloadable reloadables]
        (let [group-id (group-id reloadable request)
              timestamp (any-modified group-id (files reloadable))]
          (when timestamp
            (swap! last-compile assoc group-id timestamp)
            (reload reloadable request)))))
    (handler request)))

(defn by-uri [& uris]
  (fn [request]
    (contains? (set uris) (:uri request))))

;; Examples
;; ========

(comment

  ;; This assumes that you have defined everything in the previous
  ;; example.

  (def handler (wrap-reload (constantly {})
                            (by-uri "/development" "/production" "/fresh")
                            [watch-clj watch-cljs watch-templates]))
  ;; after touching one the watched files this will do nothing
  (handler {:uri "/home"})
  ;; after touching one the watched files this will trigger a reload
  (handler {:uri "/development"})

  )
