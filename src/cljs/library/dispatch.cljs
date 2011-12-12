(ns ^{:doc "Event dispatching."}
  library.dispatch)

(def ^{:doc "A source of unique ids for listener functions."}
  counter (atom 0))

(def ^{:doc "Maps listener functions to the number of times they have
  been fired."}
  run-counts (atom {}))

(def ^{:doc "Stores the current listeners."}
  listeners (atom {:map {} :fns []}))

(defn add-run-counter
  ""
  [dispatch id c]
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





(comment

  (do
    (let [recorded-reactions (atom [])
          reaction (react-to #{:do-something} #(swap! recorded-reactions conj [%1 %2]))]
      ;; Did we get a reaction back?
      (assert reaction)
      (fire :do-something)
      ;; Did the reactor catch the event?
      (assert (= [[:do-something nil]] @recorded-reactions))
      (fire :do-something)
      ;; Did the reactor catch the event a second time?
      (assert (= [[:do-something nil] [:do-something nil]] @recorded-reactions))
      (fire :something-else)
      ;; Did we ignore events we're not reacting to?
      (assert (= [[:do-something nil]] @recorded-reactions))
      (reset! recorded-reactions [])
      (fire :do-something 17)
      ;; Does event data arrive intact?
      (assert (= [[:do-something 17]] @recorded-reactions))
      (reset! recorded-reactions [])
      (delete-reaction reaction)
      (fire :do-something 17)
      ;; Does deleting a reaction cause us to stop reacting?
      (assert (= [] @recorded-reactions)))
    (let [recorded-reactions (atom #{})
          reaction-once (react-to 1 #{:do-something}
                                  #(swap! recorded-reactions conj [1 %1 %2]))
          reaction-twice (react-to 2 #{:do-something}
                                   #(swap! recorded-reactions conj [2 %1 %2]))]
      (fire :do-something 1)
      ;; Did both reactions react?
      (assert (= #{[1 :do-something 1] [2 :do-something 1]} @recorded-reactions))
      (fire :do-something 2)
      ;; Did only the second reaction react?
      (assert (= #{[1 :do-something 1] [2 :do-something 1] [2 :do-something 2]}
                 @recorded-reactions))
      (fire :do-something 3)
      ;; Did nothing change?
      (assert (= #{[1 :do-something 1] [2 :do-something 1] [2 :do-something 2]}
                 @recorded-reactions)))
    true
    )  
  
;;  (is (= (fire :)))
  
;;  (fire :do-something {:a :b}) ;; event-id, event-data
;; ;;  (fire [:something-else 2])   ;; event-id

;;   (react-to #{:do-something} #(...reactor...)) ;; event-pred, reactor function
  ;;=> #<Reaction:0xabcdef01>

  ;; (react-to 1 #{...} #)

  ;; (delete-reaction #<Reaction:0xabcdef01>) ;; reaction
  ;; 2
  )
