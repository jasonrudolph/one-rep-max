(ns start.templates
  (:use net.cgrand.enlive-html)
  (:import java.io.File))

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

(defn apply-templates [handler]
  (fn [request]
    (let [{:keys [headers body] :as response} (handler request)]
      (if (and (= (type body) File)
               (.endsWith (.getName body) ".html"))
        (let [new-body (emit* (construct-html (html-snippet (slurp body))))]
          {:status 200
           :headers {"Content-Type" "text/html; charset=utf-8"}
           :body new-body})
        response))))
