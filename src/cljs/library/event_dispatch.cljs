(ns library.event-dispatch)

(def counter (atom 0))
(def run-counts (atom {}))
(def listeners (atom {:map {} :fns []}))

(defn add-run-counter [dispatch id c]
  (swap! run-counts assoc id [dispatch c]))

(defn- decrement-run-count
  [id]
  (swap! run-counts
         (fn [old]
           (if-let [[dispatch c] (get old id)]
             (assoc old id [dispatch (dec c)])
             old))))

(defn- add-keyword-listener
  [id f]
  (fn [old]
    (let [m (:map old)
          l (get m id [])]
      (assoc-in old [:map id] (conj l f)))))

(defn- add-pred-listener
  [p f]
  (fn [old]
    (let [v (:fns old)]
      (assoc old :fns (conj v [p f])))))

(defn respond-to
  ([id f]
     (respond-to 0 id f)) ; 0 means respond until removed
  ([c id f]
     (let [fn-id (swap! counter inc)
           f {:id fn-id :fn f}]
       (when (> c 0) (add-run-counter id fn-id c))
       (swap! listeners
              (if (keyword? id)
                (add-keyword-listener id f)
                (add-pred-listener id f)))
       fn-id)))

(defn- remove-keyword-listener
  [state dispatch id]
  (let [m (:map state)
        l (get m dispatch)
        l (filter #(not= (:id %) id) l)]
    (if (empty? l)
      (assoc state :map (dissoc m dispatch))
      (assoc-in state [:map dispatch] (vec l)))))

(defn- remove-pred-listener
  [state id]
  (let [v (:fns state)]
    (assoc state :fns (vec (filter (fn [[_ f]] (not= (:id f) id)) v)))))

(defn remove-listener [dispatch id]
  (swap! listeners
         (fn [old]
           (if (keyword? dispatch)
             (remove-keyword-listener old dispatch id)
             (remove-pred-listener old id))))
  (swap! run-counts dissoc id))

(defn get-listeners [id]
  (concat (map second (filter (fn [[p f]] (p id)) (:fns @listeners)))
          (id (:map @listeners))))

(defn collect-garbage [listeners]
  (doseq [{id :id} listeners]
    (if-let [[dispatch c] (get @run-counts id)]
      (let [counts (decrement-run-count id)]
        (when (<= (second (get counts id)) 0)
          (remove-listener dispatch id))))))

(defn fire
  ([id]
     (fire id nil))
  ([id data]
     (when-let [fns (get-listeners id)]
       (doseq [f (map :fn fns)] (f id data))
       (collect-garbage fns))))
