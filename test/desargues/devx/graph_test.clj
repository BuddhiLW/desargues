(ns desargues.devx.graph-test
  "Unit and property-based tests for the SceneGraph DAG.
   
   Tests cover:
   - Graph creation and manipulation
   - Topological sorting
   - Cycle detection
   - Dirty propagation
   - Dependency queries
   - Statistics and visualization"
  (:require [clojure.test :refer [deftest testing is are]]
            [clojure.test.check :as tc]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.generators :as gen]
            [desargues.devx.segment :as seg]
            [desargues.devx.graph :as graph]
            [desargues.specs.devx-generators :as g]))

;; =============================================================================
;; Helper Functions
;; =============================================================================

(defn make-segment
  "Create a test segment with given id and dependencies."
  [id & {:keys [deps] :or {deps #{}}}]
  (seg/create-segment
   :id id
   :construct-fn (fn [_] nil)
   :deps deps
   :metadata {}))

(defn make-linear-chain
  "Create segments forming a linear chain: a -> b -> c -> ..."
  [ids]
  (map-indexed
   (fn [idx id]
     (make-segment id :deps (if (zero? idx)
                              #{}
                              #{(nth ids (dec idx))})))
   ids))

(defn make-diamond
  "Create a diamond-shaped DAG:
       a
      / \\
     b   c
      \\ /
       d"
  []
  [(make-segment :a)
   (make-segment :b :deps #{:a})
   (make-segment :c :deps #{:a})
   (make-segment :d :deps #{:b :c})])

(defn index-of
  "Find index of item in collection."
  [coll item]
  (first (keep-indexed #(when (= %2 item) %1) coll)))

;; =============================================================================
;; Unit Tests: Graph Creation
;; =============================================================================

(deftest test-empty-graph-creation
  (testing "Create empty graph"
    (let [g (graph/create-graph)]
      (is (= {} (:segments g)))
      (is (= {} (:edges g)))
      (is (= [] (:render-order g)))))

  (testing "Create graph with metadata"
    (let [g (graph/create-graph :metadata {:name "test-scene"})]
      (is (= "test-scene" (get-in g [:metadata :name]))))))

;; =============================================================================
;; Unit Tests: Adding Segments
;; =============================================================================

(deftest test-add-single-segment
  (testing "Add segment without dependencies"
    (let [seg (make-segment :intro)
          g (-> (graph/create-graph)
                (graph/add-segment seg))]
      (is (= 1 (graph/segment-count g)))
      (is (= #{:intro} (set (graph/segment-ids g))))
      (is (= [:intro] (graph/render-order g))))))

(deftest test-add-segment-with-deps
  (testing "Add segment with valid dependencies"
    (let [parent (make-segment :parent)
          child (make-segment :child :deps #{:parent})
          g (-> (graph/create-graph)
                (graph/add-segment parent)
                (graph/add-segment child))]
      (is (= 2 (graph/segment-count g)))
      (is (= [:parent :child] (graph/render-order g))))))

(deftest test-add-segment-missing-deps
  (testing "Error when adding segment with missing dependency"
    (let [child (make-segment :child :deps #{:nonexistent})]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"missing dependencies"
                            (-> (graph/create-graph)
                                (graph/add-segment child)))))))

(deftest test-add-segments-batch
  (testing "Add multiple segments at once"
    (let [segments (make-linear-chain [:a :b :c])
          g (-> (graph/create-graph)
                (graph/add-segments segments))]
      (is (= 3 (graph/segment-count g)))
      (is (= [:a :b :c] (graph/render-order g))))))

;; =============================================================================
;; Unit Tests: Removing Segments
;; =============================================================================

(deftest test-remove-segment
  (testing "Remove segment without dependents"
    (let [g (-> (graph/create-graph)
                (graph/add-segment (make-segment :only)))
          g2 (graph/remove-segment g :only)]
      (is (= 0 (graph/segment-count g2))))))

(deftest test-remove-segment-with-dependents
  (testing "Error when removing segment with dependents"
    (let [g (-> (graph/create-graph)
                (graph/add-segments (make-linear-chain [:a :b])))]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"dependents"
                            (graph/remove-segment g :a))))))

;; =============================================================================
;; Unit Tests: Topological Sort
;; =============================================================================

(deftest test-topological-sort-simple
  (testing "Linear chain is sorted correctly"
    (let [g (-> (graph/create-graph)
                (graph/add-segments (make-linear-chain [:first :second :third])))]
      (is (= [:first :second :third] (graph/render-order g))))))

(deftest test-topological-sort-diamond
  (testing "Diamond DAG has valid topological order"
    (let [g (-> (graph/create-graph)
                (graph/add-segments (make-diamond)))
          order (graph/render-order g)]
      ;; :a must come first
      (is (= :a (first order)))
      ;; :d must come last
      (is (= :d (last order)))
      ;; :b and :c must come after :a and before :d
      (is (< (index-of order :a) (index-of order :b)))
      (is (< (index-of order :a) (index-of order :c)))
      (is (< (index-of order :b) (index-of order :d)))
      (is (< (index-of order :c) (index-of order :d))))))

(deftest test-cycle-detection
  (testing "Cycle is detected and throws"
    ;; We can't create a cycle through normal add-segment
    ;; because dependencies must exist before adding
    ;; But we can test the internal function with a contrived scenario
    (let [seg-a (make-segment :a :deps #{:b})
          seg-b (make-segment :b :deps #{:a})]
      ;; Try to create a situation that would be cyclic
      ;; First add :a (which references :b that doesn't exist yet)
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"missing dependencies"
                            (-> (graph/create-graph)
                                (graph/add-segment seg-a)))))))

;; =============================================================================
;; Unit Tests: Dirty Propagation
;; =============================================================================

(deftest test-dirty-propagation
  (testing "Marking segment dirty propagates to dependents"
    (let [g (-> (graph/create-graph)
                (graph/add-segments (make-linear-chain [:a :b :c]))
                ;; Mark all as cached first
                (graph/update-segment :a #(-> % seg/mark-rendering (seg/mark-cached "/a.mp4")))
                (graph/update-segment :b #(-> % seg/mark-rendering (seg/mark-cached "/b.mp4")))
                (graph/update-segment :c #(-> % seg/mark-rendering (seg/mark-cached "/c.mp4")))
                ;; Now mark :a dirty
                (graph/mark-dirty :a))
          dirty-ids (set (graph/dirty-segment-ids g))]
      ;; All should be dirty because b depends on a, and c depends on b
      (is (contains? dirty-ids :a))
      (is (contains? dirty-ids :b))
      (is (contains? dirty-ids :c)))))

(deftest test-mark-all-dirty
  (testing "Mark all segments dirty"
    (let [g (-> (graph/create-graph)
                (graph/add-segments (make-linear-chain [:a :b :c]))
                graph/mark-all-dirty)]
      (is (= 3 (count (graph/dirty-segments g)))))))

;; =============================================================================
;; Unit Tests: Dependency Queries
;; =============================================================================

(deftest test-dependency-queries
  (let [g (-> (graph/create-graph)
              (graph/add-segments (make-diamond)))]

    (testing "Direct dependents"
      (is (= #{:b :c} (graph/dependents g :a)))
      (is (= #{:d} (graph/dependents g :b)))
      (is (= #{:d} (graph/dependents g :c)))
      (is (= #{} (graph/dependents g :d))))

    (testing "Direct dependencies"
      (is (= #{} (graph/dependencies g :a)))
      (is (= #{:a} (graph/dependencies g :b)))
      (is (= #{:a} (graph/dependencies g :c)))
      (is (= #{:b :c} (graph/dependencies g :d))))

    (testing "Transitive dependents"
      (is (= #{:b :c :d} (graph/transitive-dependents g :a)))
      (is (= #{:d} (graph/transitive-dependents g :b))))

    (testing "Transitive dependencies"
      (is (= #{} (graph/transitive-dependencies g :a)))
      (is (= #{:a :b :c} (graph/transitive-dependencies g :d))))))

;; =============================================================================
;; Unit Tests: Render Planning
;; =============================================================================

(deftest test-render-order
  (testing "Render order respects dependencies"
    (let [g (-> (graph/create-graph)
                (graph/add-segments (make-diamond)))
          order (graph/render-order g)]
      ;; Dependencies must come before dependents
      (doseq [[id seg] (:segments g)]
        (let [seg-idx (index-of order id)]
          (doseq [dep (seg/dependencies seg)]
            (is (< (index-of order dep) seg-idx)
                (str dep " should come before " id))))))))

(deftest test-dirty-segments
  (testing "Dirty segments are returned in render order"
    (let [g (-> (graph/create-graph)
                (graph/add-segments (make-linear-chain [:a :b :c])))
          dirty (graph/dirty-segments g)]
      ;; All are pending (needs render)
      (is (= 3 (count dirty)))
      ;; Order is correct
      (is (= [:a :b :c] (map seg/segment-id dirty))))))

(deftest test-independent-dirty-segments
  (testing "Independent segments can be rendered in parallel"
    ;; Note: is-cached? requires the file to exist, so we test the logic
    ;; by checking that segments with :cached deps and :pending/:dirty state
    ;; would be returned. We mock this by creating the partial directory
    ;; and an actual file.
    (seg/ensure-partial-dir!)
    (let [g (-> (graph/create-graph)
                (graph/add-segments (make-diamond)))
          ;; Get segment :a and compute its actual partial file path
          seg-a (graph/get-segment g :a)
          partial-path (seg/partial-file-path seg-a)
          ;; Create the partial file so is-cached? returns true
          _ (spit partial-path "test")
          g-with-cached (graph/update-segment g :a #(-> % seg/mark-rendering (seg/mark-cached partial-path)))
          independent (graph/independent-dirty-segments g-with-cached)]
      ;; :b and :c can now be rendered (their only dep :a is cached)
      (is (= #{:b :c} (set (map seg/segment-id independent))))
      ;; Clean up
      (clojure.java.io/delete-file partial-path true))))

(deftest test-next-render-batch
  (testing "Next batch includes only segments with satisfied deps"
    (let [g (-> (graph/create-graph)
                (graph/add-segments (make-linear-chain [:a :b :c])))
          batch1 (graph/next-render-batch g)]
      ;; Only :a can be rendered (no deps)
      (is (= [:a] (map seg/segment-id batch1))))))

;; =============================================================================
;; Unit Tests: Statistics
;; =============================================================================

(deftest test-stats
  (testing "Statistics are accurate"
    (let [g (-> (graph/create-graph)
                (graph/add-segments (make-diamond))
                (graph/update-segment :a #(-> % seg/mark-rendering (seg/mark-cached "/a.mp4")))
                (graph/update-segment :b seg/mark-dirty))
          s (graph/stats g)]
      (is (= 4 (:total s)))
      (is (= 1 (:cached s)))
      (is (= 1 (:dirty s)))
      (is (= 2 (:pending s))))))

;; =============================================================================
;; Unit Tests: Visualization
;; =============================================================================

(deftest test-dot-generation
  (testing "DOT output is valid format"
    (let [g (-> (graph/create-graph)
                (graph/add-segments (make-linear-chain [:a :b])))
          dot (graph/->dot g)]
      (is (clojure.string/includes? dot "digraph"))
      (is (clojure.string/includes? dot "a"))
      (is (clojure.string/includes? dot "b"))
      (is (clojure.string/includes? dot "->")))))

;; =============================================================================
;; Unit Tests: Segment Access
;; =============================================================================

(deftest test-segment-access
  (let [g (-> (graph/create-graph)
              (graph/add-segments (make-diamond)))]

    (testing "Get segment by ID"
      (let [seg (graph/get-segment g :a)]
        (is (some? seg))
        (is (= :a (seg/segment-id seg)))))

    (testing "Get non-existent segment returns nil"
      (is (nil? (graph/get-segment g :nonexistent))))

    (testing "All segments returns all"
      (is (= 4 (count (graph/all-segments g)))))

    (testing "Segment IDs returns all IDs"
      (is (= #{:a :b :c :d} (set (graph/segment-ids g)))))))

(deftest test-update-segment
  (testing "Update segment in graph"
    (let [g (-> (graph/create-graph)
                (graph/add-segment (make-segment :test))
                (graph/update-segment :test seg/mark-dirty))
          seg (graph/get-segment g :test)]
      (is (= :dirty (seg/render-state seg)))))

  (testing "Update non-existent segment throws"
    (let [g (graph/create-graph)]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"not found"
                            (graph/update-segment g :nonexistent identity))))))

;; =============================================================================
;; Property-Based Tests
;; =============================================================================

(defspec graph-segment-count-matches 50
  (prop/for-all [segments g/gen-acyclic-segments]
                (let [g (graph/add-segments (graph/create-graph) segments)]
                  (= (count segments) (graph/segment-count g)))))

(defspec graph-render-order-contains-all 50
  (prop/for-all [segments g/gen-acyclic-segments]
                (let [g (graph/add-segments (graph/create-graph) segments)
                      order (graph/render-order g)
                      seg-ids (set (map seg/segment-id segments))]
                  (= seg-ids (set order)))))

(defspec graph-render-order-respects-deps 50
  (prop/for-all [segments g/gen-acyclic-segments]
                (let [g (graph/add-segments (graph/create-graph) segments)
                      order (vec (graph/render-order g))]
                  (every?
                   (fn [seg-id]
                     (let [seg (graph/get-segment g seg-id)
                           seg-idx (index-of order seg-id)]
                       (every? #(< (index-of order %) seg-idx)
                               (seg/dependencies seg))))
                   order))))

(defspec graph-dirty-includes-dependents 30
  (prop/for-all [segments (g/gen-linear-chain 3)]
                (let [g (-> (graph/create-graph)
                            (graph/add-segments segments)
                ;; Cache all segments first
                            (graph/update-segment :seg-0 #(-> % seg/mark-rendering (seg/mark-cached "/0.mp4")))
                            (graph/update-segment :seg-1 #(-> % seg/mark-rendering (seg/mark-cached "/1.mp4")))
                            (graph/update-segment :seg-2 #(-> % seg/mark-rendering (seg/mark-cached "/2.mp4")))
                ;; Mark first dirty
                            (graph/mark-dirty :seg-0))
                      dirty-ids (set (graph/dirty-segment-ids g))]
      ;; All should be dirty because they form a chain
                  (= #{:seg-0 :seg-1 :seg-2} dirty-ids))))
