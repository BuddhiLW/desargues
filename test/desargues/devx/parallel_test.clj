(ns desargues.devx.parallel-test
  "Unit tests for the parallel execution module.
   
   Tests cover:
   - Wave analysis and partitioning
   - Wave statistics calculation
   - Parallel time estimation
   - Edge cases (empty graphs, cycles, single segment)"
  (:require [clojure.test :refer [deftest testing is are]]
            [desargues.devx.segment :as seg]
            [desargues.devx.graph :as graph]
            [desargues.devx.parallel :as parallel]))

;; =============================================================================
;; Test Fixtures / Helpers
;; =============================================================================

(defn make-segment
  "Create a test segment with given id and dependencies."
  [id & {:keys [deps state] :or {deps #{} state :dirty}}]
  (-> (seg/create-segment
       :id id
       :construct-fn (fn [_] nil)
       :deps deps
       :metadata {})
      (assoc :state state)))

(defn make-graph
  "Create a graph from segment specs.
   Each spec is [id deps] or just id for independent segments."
  [& specs]
  (let [segments (map (fn [spec]
                        (if (vector? spec)
                          (make-segment (first spec) :deps (second spec))
                          (make-segment spec)))
                      specs)]
    (graph/add-segments (graph/create-graph) segments)))

(defn make-diamond-graph
  "Create a diamond-shaped DAG:
       a
      / \\
     b   c
      \\ /
       d"
  []
  (make-graph :a [:b #{:a}] [:c #{:a}] [:d #{:b :c}]))

(defn make-wide-graph
  "Create a wide graph with multiple independent segments:
   a  b  c  d  (all independent)
       |
       e       (depends on b)"
  []
  (make-graph :a :b :c :d [:e #{:b}]))

(defn make-linear-chain
  "Create a linear chain: a -> b -> c -> ..."
  [ids]
  (let [specs (reduce (fn [acc id]
                        (if (empty? acc)
                          [id] ; First element has no deps, just the id
                          (let [prev-id (if (vector? (last acc))
                                          (first (last acc))
                                          (last acc))]
                            (conj acc [id #{prev-id}]))))
                      []
                      ids)]
    (apply make-graph specs)))

;; =============================================================================
;; analyze-waves Tests
;; =============================================================================

(deftest test-analyze-waves-empty
  (testing "Empty graph returns empty waves"
    (let [g (graph/create-graph)
          waves (parallel/analyze-waves g [])]
      (is (= [] waves)))))

(deftest test-analyze-waves-single-segment
  (testing "Single segment creates single wave"
    (let [g (make-graph :a)
          waves (parallel/analyze-waves g [:a])]
      (is (= 1 (count waves)))
      (is (= #{:a} (first waves))))))

(deftest test-analyze-waves-independent-segments
  (testing "Independent segments are in the same wave"
    (let [g (make-graph :a :b :c :d)
          waves (parallel/analyze-waves g [:a :b :c :d])]
      (is (= 1 (count waves)))
      (is (= #{:a :b :c :d} (first waves))))))

(deftest test-analyze-waves-linear-chain
  (testing "Linear chain creates sequential waves"
    (let [g (make-linear-chain [:a :b :c :d])
          waves (parallel/analyze-waves g [:a :b :c :d])]
      (is (= 4 (count waves)))
      (is (= #{:a} (nth waves 0)))
      (is (= #{:b} (nth waves 1)))
      (is (= #{:c} (nth waves 2)))
      (is (= #{:d} (nth waves 3))))))

(deftest test-analyze-waves-diamond
  (testing "Diamond DAG creates correct waves"
    (let [g (make-diamond-graph)
          waves (parallel/analyze-waves g [:a :b :c :d])]
      ;; Wave 0: a (no deps)
      ;; Wave 1: b, c (both depend on a)
      ;; Wave 2: d (depends on b and c)
      (is (= 3 (count waves)))
      (is (= #{:a} (nth waves 0)))
      (is (= #{:b :c} (nth waves 1)))
      (is (= #{:d} (nth waves 2))))))

(deftest test-analyze-waves-partial-dirty
  (testing "Only dirty segments are included in waves"
    (let [g (make-diamond-graph)
          ;; Only b and d are dirty
          waves (parallel/analyze-waves g [:b :d])]
      ;; Wave 0: b (no dirty deps)
      ;; Wave 1: d (depends on b which is dirty)
      (is (= 2 (count waves)))
      (is (= #{:b} (nth waves 0)))
      (is (= #{:d} (nth waves 1))))))

(deftest test-analyze-waves-complex-graph
  (testing "Complex graph with multiple dependency levels"
    ;; Graph structure:
    ;;   a    b    c   (level 0 - independent)
    ;;   |    |    |
    ;;   d    e    f   (level 1 - each depends on one above)
    ;;    \  /|\  /
    ;;     g   h       (level 2 - g deps d,e; h deps e,f)
    ;;      \ /
    ;;       i         (level 3 - deps g,h)
    (let [g (make-graph
             :a :b :c
             [:d #{:a}] [:e #{:b}] [:f #{:c}]
             [:g #{:d :e}] [:h #{:e :f}]
             [:i #{:g :h}])
          waves (parallel/analyze-waves g [:a :b :c :d :e :f :g :h :i])]
      (is (= 4 (count waves)))
      (is (= #{:a :b :c} (nth waves 0)))
      (is (= #{:d :e :f} (nth waves 1)))
      (is (= #{:g :h} (nth waves 2)))
      (is (= #{:i} (nth waves 3))))))

;; =============================================================================
;; wave-stats Tests
;; =============================================================================

(deftest test-wave-stats-empty
  (testing "Empty waves return zero stats"
    (let [stats (parallel/wave-stats [])]
      (is (= 0 (:total-waves stats)))
      (is (= 0 (:total-segments stats)))
      (is (= 0 (:max-parallelism stats)))
      (is (= [] (:wave-sizes stats))))))

(deftest test-wave-stats-single-wave
  (testing "Single wave stats"
    (let [waves [#{:a :b :c}]
          stats (parallel/wave-stats waves)]
      (is (= 1 (:total-waves stats)))
      (is (= 3 (:total-segments stats)))
      (is (= 3 (:max-parallelism stats)))
      (is (= [3] (:wave-sizes stats))))))

(deftest test-wave-stats-multiple-waves
  (testing "Multiple waves with varying sizes"
    (let [waves [#{:a} #{:b :c :d} #{:e :f}]
          stats (parallel/wave-stats waves)]
      (is (= 3 (:total-waves stats)))
      (is (= 6 (:total-segments stats)))
      (is (= 3 (:max-parallelism stats)))
      (is (= [1 3 2] (:wave-sizes stats))))))

(deftest test-wave-stats-diamond
  (testing "Diamond graph wave stats"
    (let [g (make-diamond-graph)
          waves (parallel/analyze-waves g [:a :b :c :d])
          stats (parallel/wave-stats waves)]
      (is (= 3 (:total-waves stats)))
      (is (= 4 (:total-segments stats)))
      (is (= 2 (:max-parallelism stats)))
      (is (= [1 2 1] (:wave-sizes stats))))))

;; =============================================================================
;; estimate-parallel-time Tests
;; =============================================================================

(deftest test-estimate-parallel-time-empty
  (testing "Empty graph estimation"
    (let [g (graph/create-graph)
          estimate (parallel/estimate-parallel-time g 10.0 4)]
      (is (= 0.0 (:sequential-estimate-s estimate)))
      (is (= 0.0 (:parallel-estimate-s estimate)))
      (is (= 1.0 (:speedup-factor estimate))))))

(deftest test-estimate-parallel-time-independent
  (testing "Independent segments get full parallelism benefit"
    (let [g (make-graph :a :b :c :d)
          estimate (parallel/estimate-parallel-time g 10.0 4)]
      ;; Sequential: 4 segments * 10s = 40s
      (is (= 40.0 (:sequential-estimate-s estimate)))
      ;; Parallel with 4 workers: 1 wave * ceil(4/4) * 10s = 10s
      (is (= 10.0 (:parallel-estimate-s estimate)))
      ;; Speedup: 40/10 = 4x
      (is (= 4.0 (:speedup-factor estimate))))))

(deftest test-estimate-parallel-time-linear
  (testing "Linear chain gets no parallelism benefit"
    (let [g (make-linear-chain [:a :b :c :d])
          estimate (parallel/estimate-parallel-time g 10.0 4)]
      ;; Sequential: 4 segments * 10s = 40s
      (is (= 40.0 (:sequential-estimate-s estimate)))
      ;; Parallel: 4 waves * 1 segment each = 40s (no benefit)
      (is (= 40.0 (:parallel-estimate-s estimate)))
      ;; Speedup: 1x (no improvement)
      (is (= 1.0 (:speedup-factor estimate))))))

(deftest test-estimate-parallel-time-diamond
  (testing "Diamond graph gets partial parallelism benefit"
    (let [g (make-diamond-graph)
          estimate (parallel/estimate-parallel-time g 10.0 4)]
      ;; Sequential: 4 segments * 10s = 40s
      (is (= 40.0 (:sequential-estimate-s estimate)))
      ;; Parallel: 3 waves, max parallelism 2
      ;; Wave 0: 1 segment, Wave 1: 2 segments, Wave 2: 1 segment
      ;; With 4 workers: 3 * ceil(2/4) * 10 = 3 * 1 * 10 = 30s
      (is (= 30.0 (:parallel-estimate-s estimate))))))

(deftest test-estimate-parallel-time-worker-limit
  (testing "Worker limit affects parallelism"
    (let [g (make-graph :a :b :c :d :e :f :g :h) ; 8 independent segments
          estimate-4 (parallel/estimate-parallel-time g 10.0 4)
          estimate-2 (parallel/estimate-parallel-time g 10.0 2)]
      ;; Sequential: 8 * 10 = 80s
      (is (= 80.0 (:sequential-estimate-s estimate-4)))
      (is (= 80.0 (:sequential-estimate-s estimate-2)))
      ;; With 4 workers: ceil(8/4) * 10 = 20s
      (is (= 20.0 (:parallel-estimate-s estimate-4)))
      ;; With 2 workers: ceil(8/2) * 10 = 40s
      (is (= 40.0 (:parallel-estimate-s estimate-2))))))

;; =============================================================================
;; Integration Tests (wave analysis + stats combined)
;; =============================================================================

(deftest test-wide-graph-analysis
  (testing "Wide graph with mixed dependencies"
    (let [g (make-wide-graph)
          dirty-ids [:a :b :c :d :e]
          waves (parallel/analyze-waves g dirty-ids)
          stats (parallel/wave-stats waves)]
      ;; Wave 0: a, b, c, d (independent)
      ;; Wave 1: e (depends on b)
      (is (= 2 (:total-waves stats)))
      (is (= 5 (:total-segments stats)))
      (is (= 4 (:max-parallelism stats)))
      (is (= #{:a :b :c :d} (first waves)))
      (is (= #{:e} (second waves))))))

(deftest test-waves-preserve-all-segments
  (testing "All dirty segments appear exactly once in waves"
    (let [g (make-graph
             :a :b :c
             [:d #{:a}] [:e #{:b}] [:f #{:c}]
             [:g #{:d :e :f}])
          dirty-ids [:a :b :c :d :e :f :g]
          waves (parallel/analyze-waves g dirty-ids)
          all-in-waves (apply clojure.set/union waves)]
      (is (= (set dirty-ids) all-in-waves)))))

(deftest test-wave-order-respects-dependencies
  (testing "Dependencies always appear in earlier waves"
    (let [g (make-diamond-graph)
          waves (parallel/analyze-waves g [:a :b :c :d])
          wave-for-segment (into {}
                                 (for [[idx wave] (map-indexed vector waves)
                                       seg-id wave]
                                   [seg-id idx]))]
      ;; :a must be before :b and :c
      (is (< (wave-for-segment :a) (wave-for-segment :b)))
      (is (< (wave-for-segment :a) (wave-for-segment :c)))
      ;; :b and :c must be before :d
      (is (< (wave-for-segment :b) (wave-for-segment :d)))
      (is (< (wave-for-segment :c) (wave-for-segment :d))))))

;; =============================================================================
;; Edge Cases
;; =============================================================================

(deftest test-single-segment-no-deps
  (testing "Single segment with no dependencies"
    (let [g (make-graph :only)
          waves (parallel/analyze-waves g [:only])
          stats (parallel/wave-stats waves)]
      (is (= 1 (:total-waves stats)))
      (is (= 1 (:total-segments stats)))
      (is (= 1 (:max-parallelism stats))))))

(deftest test-many-independent-segments
  (testing "Large number of independent segments"
    (let [ids (map #(keyword (str "seg-" %)) (range 100))
          g (apply make-graph ids)
          waves (parallel/analyze-waves g ids)
          stats (parallel/wave-stats waves)]
      (is (= 1 (:total-waves stats)))
      (is (= 100 (:total-segments stats)))
      (is (= 100 (:max-parallelism stats))))))

(deftest test-subset-of-dirty-segments
  (testing "Analyze only a subset of segments as dirty"
    (let [g (make-diamond-graph)
          ;; Only :c and :d are dirty
          waves (parallel/analyze-waves g [:c :d])]
      ;; Wave 0: :c (no dirty deps - :a is not dirty)
      ;; Wave 1: :d (depends on :c which is dirty, but :b is not dirty)
      (is (= 2 (count waves)))
      (is (= #{:c} (first waves)))
      (is (= #{:d} (second waves))))))
