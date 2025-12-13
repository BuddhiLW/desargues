(ns desargues.devx.hot-reload-test
  "Tests for Phase 2 hot-reload infrastructure."
  (:require [clojure.test :refer [deftest is testing]]
            [desargues.devx.watcher :as watcher]
            [desargues.devx.ns-tracker :as tracker]
            [desargues.devx.segment :as seg]
            [desargues.devx.graph :as graph]))

;; =============================================================================
;; Watcher Tests
;; =============================================================================

(deftest watcher-state-test
  (testing "Watcher starts in stopped state"
    (is (false? (watcher/watching?)))))

(deftest watcher-stats-test
  (testing "Watcher stats returns expected structure"
    (let [stats (watcher/watcher-stats)]
      (is (contains? stats :running?))
      (is (contains? stats :paths))
      (is (contains? stats :changes-detected)))))

;; =============================================================================
;; Namespace Tracker Tests
;; =============================================================================

(deftest segment-source-ns-test
  (testing "Extracts source-ns from segment metadata"
    (let [seg (seg/create-segment
               :id :test-seg
               :construct-fn (fn [_] nil)
               :metadata {:source-ns 'my.test.ns})]
      (is (= 'my.test.ns (tracker/segment-source-ns seg)))))

  (testing "Returns nil when no source-ns"
    (let [seg (seg/create-segment
               :id :no-ns-seg
               :construct-fn (fn [_] nil))]
      (is (nil? (tracker/segment-source-ns seg))))))

(deftest segments-for-namespace-test
  (testing "Finds segments by namespace"
    (let [seg1 (seg/create-segment
                :id :seg1
                :construct-fn (fn [_] nil)
                :metadata {:source-ns 'my.ns})
          seg2 (seg/create-segment
                :id :seg2
                :construct-fn (fn [_] nil)
                :metadata {:source-ns 'other.ns})
          seg3 (seg/create-segment
                :id :seg3
                :construct-fn (fn [_] nil)
                :metadata {:source-ns 'my.ns})
          g (-> (graph/create-graph)
                (graph/add-segments [seg1 seg2 seg3]))]
      (is (= #{:seg1 :seg3} (set (tracker/segments-for-namespace g 'my.ns))))
      (is (= #{:seg2} (set (tracker/segments-for-namespace g 'other.ns))))
      (is (empty? (tracker/segments-for-namespace g 'unknown.ns))))))

(deftest affected-segments-test
  (testing "Finds affected segments including dependents"
    (let [seg1 (seg/create-segment
                :id :base
                :construct-fn (fn [_] nil)
                :metadata {:source-ns 'base.ns})
          seg2 (seg/create-segment
                :id :dependent
                :construct-fn (fn [_] nil)
                :deps #{:base}
                :metadata {:source-ns 'other.ns})
          g (-> (graph/create-graph)
                (graph/add-segments [seg1 seg2]))]
      ;; When base.ns changes, both :base and :dependent are affected
      (is (= #{:base :dependent} (tracker/affected-segments g 'base.ns)))
      ;; When other.ns changes, only :dependent is affected
      (is (= #{:dependent} (tracker/affected-segments g 'other.ns))))))

(deftest build-ns-index-test
  (testing "Builds namespace index correctly"
    (let [seg1 (seg/create-segment
                :id :tracked1
                :construct-fn (fn [_] nil)
                :metadata {:source-ns 'my.ns})
          seg2 (seg/create-segment
                :id :untracked
                :construct-fn (fn [_] nil))
          g (-> (graph/create-graph)
                (graph/add-segments [seg1 seg2]))
          index (tracker/build-ns-index g)]
      (is (contains? (:by-ns index) 'my.ns))
      (is (= #{:tracked1} (get-in index [:by-ns 'my.ns])))
      (is (= #{:untracked} (:untracked index))))))

(deftest report-tracking-test
  (testing "Generates tracking report"
    (let [seg1 (seg/create-segment
                :id :tracked
                :construct-fn (fn [_] nil)
                :metadata {:source-ns 'my.ns})
          g (-> (graph/create-graph)
                (graph/add-segment seg1))
          report (tracker/report-tracking g)]
      (is (= 1 (:total-segments report)))
      (is (= 1 (:tracked-segments report)))
      (is (= 0 (:untracked-segments report)))
      (is (= 1.0 (:tracking-coverage report))))))

;; =============================================================================
;; Watcher Lifecycle Tests
;; =============================================================================

(deftest watcher-start-stop-test
  (testing "Watcher can start and stop"
    ;; Ensure stopped first
    (when (watcher/watching?)
      (watcher/stop-watcher!))

    (is (false? (watcher/watching?)))

    ;; Start watcher
    (let [events (atom [])
          result (watcher/start-watcher!
                  {:paths ["src"]
                   :on-change (fn [e] (swap! events conj e))})]
      (is (some? result))
      (is (true? (watcher/watching?)))

      ;; Check stats
      (let [stats (watcher/watcher-stats)]
        (is (true? (:running? stats)))
        (is (= ["src"] (:paths stats)))
        (is (= 0 (:changes-detected stats))))

      ;; Stop watcher
      (watcher/stop-watcher!)
      (is (false? (watcher/watching?))))))

(deftest watcher-requires-callback-test
  (testing "Watcher throws without on-change callback"
    (is (thrown? clojure.lang.ExceptionInfo
                 (watcher/start-watcher! {:paths ["src"]})))))

(deftest watcher-prevents-double-start-test
  (testing "Watcher returns nil if already running"
    (when (watcher/watching?)
      (watcher/stop-watcher!))

    ;; Start first watcher
    (watcher/start-watcher!
     {:paths ["src"]
      :on-change (fn [_] nil)})

    (is (true? (watcher/watching?)))

    ;; Try to start second watcher - should return nil
    (let [result (watcher/start-watcher!
                  {:paths ["test"]
                   :on-change (fn [_] nil)})]
      (is (nil? result))
      ;; Paths should still be original
      (is (= ["src"] (:paths (watcher/watcher-stats)))))

    ;; Cleanup
    (watcher/stop-watcher!)))

;; =============================================================================
;; Reload Module Tests
;; =============================================================================

(deftest reload-hash-computation-test
  (testing "Hash changes when construct-fn changes"
    (let [seg1 (seg/create-segment
                :id :test
                :construct-fn (fn [_] :version-1)
                :metadata {:source-ns 'test.ns})
          seg2 (seg/create-segment
                :id :test
                :construct-fn (fn [_] :version-2)
                :metadata {:source-ns 'test.ns})
          g1 (-> (graph/create-graph) (graph/add-segment seg1))
          g2 (-> (graph/create-graph) (graph/add-segment seg2))
          hash1 (:hash (graph/get-segment g1 :test))
          hash2 (:hash (graph/get-segment g2 :test))]
      ;; Different construct-fn should produce different hash
      (is (not= hash1 hash2)))))

(deftest multi-namespace-tracking-test
  (testing "Complex dependency graph with multiple namespaces"
    (let [;; Create a complex graph:
          ;; ns-a: :a1, :a2
          ;; ns-b: :b1 (deps: :a1)
          ;; ns-c: :c1 (deps: :b1, :a2)
          a1 (seg/create-segment :id :a1 :construct-fn (fn [_] nil)
                                 :metadata {:source-ns 'ns-a})
          a2 (seg/create-segment :id :a2 :construct-fn (fn [_] nil)
                                 :metadata {:source-ns 'ns-a})
          b1 (seg/create-segment :id :b1 :construct-fn (fn [_] nil)
                                 :deps #{:a1}
                                 :metadata {:source-ns 'ns-b})
          c1 (seg/create-segment :id :c1 :construct-fn (fn [_] nil)
                                 :deps #{:b1 :a2}
                                 :metadata {:source-ns 'ns-c})
          g (-> (graph/create-graph)
                (graph/add-segments [a1 a2 b1 c1]))]

      ;; When ns-a changes: a1, a2 directly + b1 (via a1) + c1 (via b1 and a2)
      (is (= #{:a1 :a2 :b1 :c1} (tracker/affected-segments g 'ns-a)))

      ;; When ns-b changes: b1 directly + c1 (via b1)
      (is (= #{:b1 :c1} (tracker/affected-segments g 'ns-b)))

      ;; When ns-c changes: only c1 (no dependents)
      (is (= #{:c1} (tracker/affected-segments g 'ns-c))))))
