(ns start.templates
  (:use net.cgrand.enlive-html))

(defn render [t] (apply str (emit* t)))

(declare construct-html)

(defn html-body [name]
  (:content (first (select (html-resource name) [:body]))))

(defn include-html [h]
  (let [includes (select h [:_include])]
    (loop [h h
           includes (seq includes)]
      (if includes
        (let [file (-> (first includes) :attrs :file)
              include (construct-html (html-body file))]
          (recur (transform h [[:_include (attr= :file file)]] (substitute include))
                 (next includes)))
        h))))

(defn- maps [c] (filter map? c))

(defn- replace-html [h c]
  (let [id (-> c :attrs :id)
        tag (:tag c)
        selector (keyword (str (name tag) "#" id))]
    (transform h [selector] (substitute c))))

(defn wrap-html [h]
  (let [within (seq (select h [:_within]))]
    (if within
      (let [file (-> (first within) :attrs :file)
            outer (construct-html (html-resource file))
            content (maps (:content (first within)))]
        (loop [outer outer
               content (seq content)]
          (if content
            (recur (replace-html outer (first content)) (next content))
            outer)))
      h)))

(defn construct-html [h]
  (wrap-html (include-html h)))

(defn load-html [file]
  (render (construct-html (html-resource file))))

(comment

  (println (render (construct-html (html-resource "application.html"))))
  (println (render (construct-html (html-resource "test.html"))))
  (println (render (construct-html (html-resource "form.html"))))
  )
