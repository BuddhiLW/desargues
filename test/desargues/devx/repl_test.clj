(ns desargues.devx.repl-test
  "Tests for REPL workflow helpers.
   
   Tests cover:
   - State management (graph atom)
   - Status and inspection functions
   - Rendering operations
   - Change management
   - Short aliases
   
   These tests are designed to be low-maintenance by depending on
   the high-level REPL API which is unlikely to change."
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [desargues.devx.segment :as seg]
            [desargues.devx.graph :as graph]
            [desargues.devx.repl :as repl]))

;; =============================================================================
;; Test Fixtures
;; =============================================================================

(defn clean-graph-fixture
  "Ensure graph is cleared before and after each test."
  [f]
  (repl/clear-graph!)
  (f)
  (repl/clear-graph!))

(use-fixtures :each clean-graph-fixture)

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

(defn make-simple-graph
  "Create a simple test graph with segments a -> b -> c."
  []
  (let [segments [(make-segment :a)
                  (make-segment :b :deps #{:a})
                  (make-segment :c :deps #{:b})]]
    (graph/add-segments (graph/create-graph) segments)))

;; =============================================================================
;; Unit Tests: State Management
;; =============================================================================

(deftest test-state-management
  (testing "Initial state is nil"
    (is (nil? (repl/get-graph))))

  (testing "Set graph stores the graph"
    (let [g (make-simple-graph)]
      (repl/set-graph! g)
      (is (= g (repl/get-graph)))))

  (testing "Clear graph removes the graph"
    (repl/set-graph! (make-simple-graph))
    (repl/clear-graph!)
    (is (nil? (repl/get-graph))))

  (testing "Set graph returns the graph for chaining"
    (let [g (make-simple-graph)
          result (repl/set-graph! g)]
      (is (= g result)))))

;; =============================================================================
;; Unit Tests: Status and Inspection
;; =============================================================================

(deftest test-status-functions
  (testing "status with no graph prints message"
    ;; Just verify it doesn't throw
    (is (nil? (repl/status))))

  (testing "stats returns nil when no graph"
    (is (nil? (repl/stats))))

  (testing "segments returns nil when no graph"
    (is (nil? (repl/segments))))

  (testing "dirty-segments returns nil when no graph"
    (is (nil? (repl/dirty-segments))))

  ;; Now set up a graph for the remaining tests
  (repl/set-graph! (make-simple-graph))

  (testing "status with graph prints status"
    ;; Just verify it doesn't throw
    (is (nil? (repl/status))))

  (testing "stats returns statistics map when graph loaded"
    (let [s (repl/stats)]
      (is (map? s))
      (is (= 3 (:total s)))))

  (testing "segments returns segment IDs when graph loaded"
    (is (= #{:a :b :c} (set (repl/segments)))))

  (testing "dirty-segments returns dirty IDs when graph loaded"
    ;; All segments start as pending (needing render)
    (is (= #{:a :b :c} (set (repl/dirty-segments))))))

;; =============================================================================
;; Unit Tests: Change Management
;; =============================================================================

(deftest test-change-management
  (testing "dirty! returns nil when no graph"
    (is (nil? (repl/dirty! :a))))

  (testing "dirty! marks segment and dependents as dirty"
    (let [g (-> (make-simple-graph)
                ;; Cache all segments first
                (graph/update-segment :a #(-> % seg/mark-rendering (seg/mark-cached "/a.mp4")))
                (graph/update-segment :b #(-> % seg/mark-rendering (seg/mark-cached "/b.mp4")))
                (graph/update-segment :c #(-> % seg/mark-rendering (seg/mark-cached "/c.mp4"))))]
      (repl/set-graph! g)
      (repl/dirty! :a)
      ;; All should be dirty because they form a chain
      (is (= #{:a :b :c} (set (repl/dirty-segments))))))

  (testing "dirty-all! marks all segments dirty"
    (let [g (-> (make-simple-graph)
                (graph/update-segment :a #(-> % seg/mark-rendering (seg/mark-cached "/a.mp4")))
                (graph/update-segment :b #(-> % seg/mark-rendering (seg/mark-cached "/b.mp4")))
                (graph/update-segment :c #(-> % seg/mark-rendering (seg/mark-cached "/c.mp4"))))]
      (repl/set-graph! g)
      (repl/dirty-all!)
      (is (= #{:a :b :c} (set (repl/dirty-segments)))))))

;; =============================================================================
;; Unit Tests: Short Aliases
;; =============================================================================

(deftest test-short-aliases
  (testing "Aliases are defined and point to correct functions"
    (is (= repl/render! repl/r!))
    (is (= repl/preview! repl/p!))
    (is (= repl/dirty! repl/d!))
    (is (= repl/status repl/s!))
    (is (= repl/combine! repl/c!))))

;; =============================================================================
;; Unit Tests: Namespace Smoke Test
;; =============================================================================

(deftest test-repl-namespace-loads
  (testing "REPL namespace loads without errors"
    ;; State management
    (is (fn? repl/set-graph!))
    (is (fn? repl/get-graph))
    (is (fn? repl/clear-graph!))
    ;; Status
    (is (fn? repl/status))
    (is (fn? repl/stats))
    (is (fn? repl/segments))
    (is (fn? repl/dirty-segments))
    ;; Rendering
    (is (fn? repl/render!))
    (is (fn? repl/render-all!))
    (is (fn? repl/render-segment!))
    ;; Preview
    (is (fn? repl/preview!))
    ;; Changes
    (is (fn? repl/dirty!))
    (is (fn? repl/dirty-all!))
    ;; Export
    (is (fn? repl/combine!))
    (is (fn? repl/export!))
    ;; Help
    (is (fn? repl/help))))

(deftest test-help-function
  (testing "Help prints without error"
    ;; Just verify it runs without throwing
    (is (nil? (repl/help)))))
