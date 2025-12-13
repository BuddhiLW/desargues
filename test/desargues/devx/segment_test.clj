(ns desargues.devx.segment-test
  "Unit and property-based tests for the Segment abstraction.
   
   Tests cover:
   - Segment creation and validation
   - State machine transitions
   - Content hashing
   - Protocol implementations
   - Query functions"
  (:require [clojure.test :refer [deftest testing is are]]
            [clojure.test.check :as tc]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.generators :as gen]
            [desargues.devx.segment :as seg]
            [desargues.specs.devx-generators :as g]))

;; =============================================================================
;; Helper Functions
;; =============================================================================

(defn make-test-segment
  "Create a simple test segment with optional overrides."
  [& {:keys [id construct-fn deps metadata]
      :or {id :test-seg
           construct-fn (fn [_] nil)
           deps #{}
           metadata {}}}]
  (seg/create-segment
   :id id
   :construct-fn construct-fn
   :deps deps
   :metadata metadata))

(defn different-construct-fn
  "Create a construct function that produces different output."
  [n]
  (fn [_] {:value n}))

;; =============================================================================
;; Unit Tests: Segment Creation
;; =============================================================================

(deftest test-segment-creation
  (testing "Create segment with required fields"
    (let [seg (make-test-segment :id :my-segment)]
      (is (= :my-segment (:id seg)))
      (is (= :pending (:state seg)))
      (is (= #{} (:deps seg)))
      (is (string? (:hash seg)))
      (is (fn? (:construct-fn seg)))))

  (testing "Create segment with dependencies"
    (let [seg (make-test-segment :id :child :deps #{:parent})]
      (is (= #{:parent} (:deps seg)))))

  (testing "Create segment with metadata"
    (let [seg (make-test-segment :id :meta-seg
                                 :metadata {:duration 5.0
                                            :description "Test"})]
      (is (= 5.0 (get-in seg [:metadata :duration])))
      (is (= "Test" (get-in seg [:metadata :description]))))))

(deftest test-segment-creation-validation
  (testing "Segment ID must be a keyword"
    (is (thrown? AssertionError
                 (seg/create-segment :id "not-a-keyword"
                                     :construct-fn (fn [_] nil)))))

  (testing "Dependencies must be a set"
    (is (thrown? AssertionError
                 (seg/create-segment :id :test
                                     :construct-fn (fn [_] nil)
                                     :deps [:not :a :set]))))

  (testing "Dependencies must be keywords"
    (is (thrown? AssertionError
                 (seg/create-segment :id :test
                                     :construct-fn (fn [_] nil)
                                     :deps #{"not-a-keyword"})))))

;; =============================================================================
;; Unit Tests: State Transitions
;; =============================================================================

(deftest test-state-transitions
  (testing "pending -> rendering"
    (let [seg (make-test-segment)
          seg2 (seg/mark-rendering seg)]
      (is (= :rendering (:state seg2)))))

  (testing "rendering -> cached"
    (let [seg (-> (make-test-segment)
                  seg/mark-rendering
                  (seg/mark-cached "/path/to/file.mp4"))]
      (is (= :cached (:state seg)))
      (is (= "/path/to/file.mp4" (:partial-file seg)))))

  (testing "any -> dirty"
    (let [seg (make-test-segment)]
      (is (= :dirty (:state (seg/mark-dirty seg))))))

  (testing "dirty -> rendering"
    (let [seg (-> (make-test-segment)
                  seg/mark-dirty
                  seg/mark-rendering)]
      (is (= :rendering (:state seg)))))

  (testing "error -> rendering (retry)"
    (let [seg (-> (make-test-segment)
                  seg/mark-rendering
                  (seg/mark-error {:message "Failed"})
                  seg/mark-rendering)]
      (is (= :rendering (:state seg)))))

  (testing "any -> pending (reset)"
    (let [seg (-> (make-test-segment)
                  seg/mark-rendering
                  (seg/mark-cached "/path.mp4")
                  seg/mark-pending)]
      (is (= :pending (:state seg)))
      (is (nil? (:partial-file seg))))))

(deftest test-invalid-state-transitions
  (testing "Cannot transition from cached to rendering directly"
    (let [seg (-> (make-test-segment)
                  seg/mark-rendering
                  (seg/mark-cached "/path.mp4"))]
      (is (thrown? AssertionError (seg/mark-rendering seg)))))

  (testing "Cannot mark cached without being in rendering state"
    (let [seg (make-test-segment)]
      (is (thrown? AssertionError (seg/mark-cached seg "/path.mp4"))))))

;; =============================================================================
;; Unit Tests: Hashing
;; =============================================================================

(deftest test-hash-computation
  (testing "Hash is computed on creation"
    (let [seg (make-test-segment)]
      (is (string? (:hash seg)))
      (is (> (count (:hash seg)) 10))))

  (testing "Same construct-fn produces same hash"
    (let [f (fn [_] :same)
          seg1 (seg/create-segment :id :s1 :construct-fn f :deps #{})
          seg2 (seg/create-segment :id :s1 :construct-fn f :deps #{})]
      (is (= (:hash seg1) (:hash seg2)))))

  (testing "Different construct-fn produces different hash"
    (let [seg1 (make-test-segment :id :s1 :construct-fn (different-construct-fn 1))
          seg2 (make-test-segment :id :s1 :construct-fn (different-construct-fn 2))]
      (is (not= (:hash seg1) (:hash seg2)))))

  (testing "with-hash updates hash based on dependency hashes"
    (let [seg (make-test-segment :deps #{:dep1})
          seg-with-hash (seg/with-hash seg {:dep1 "abc123"})]
      (is (not= (:hash seg) (:hash seg-with-hash)))))

  (testing "Different dependency hashes produce different segment hashes"
    (let [seg (make-test-segment :deps #{:dep1})
          seg-hash-a (seg/with-hash seg {:dep1 "hash-a"})
          seg-hash-b (seg/with-hash seg {:dep1 "hash-b"})]
      (is (not= (:hash seg-hash-a) (:hash seg-hash-b))))))

;; =============================================================================
;; Unit Tests: Protocol Implementations
;; =============================================================================

(deftest test-protocol-implementations
  (testing "ISegment protocol"
    (let [seg (make-test-segment :id :proto-test :deps #{:dep1 :dep2})]
      (is (= :proto-test (seg/segment-id seg)))
      (is (string? (seg/segment-hash seg)))
      (is (= #{:dep1 :dep2} (seg/dependencies seg)))
      (is (= :pending (seg/render-state seg)))))

  (testing "construct! executes construct-fn"
    (let [result (atom nil)
          seg (make-test-segment
               :construct-fn (fn [scene]
                               (reset! result scene)
                               :done))]
      (seg/construct! seg :test-scene)
      (is (= :test-scene @result)))))

;; =============================================================================
;; Unit Tests: Partial File Management
;; =============================================================================

(deftest test-partial-file-path
  (testing "Path includes segment ID"
    (let [seg (make-test-segment :id :my-segment)
          path (seg/partial-file-path seg)]
      (is (clojure.string/includes? path "my-segment"))))

  (testing "Path includes hash prefix"
    (let [seg (make-test-segment)
          path (seg/partial-file-path seg)
          hash-prefix (subs (:hash seg) 0 8)]
      (is (clojure.string/includes? path hash-prefix))))

  (testing "Path ends with .mp4"
    (let [seg (make-test-segment)
          path (seg/partial-file-path seg)]
      (is (clojure.string/ends-with? path ".mp4")))))

;; =============================================================================
;; Unit Tests: Query Functions
;; =============================================================================

(deftest test-query-functions
  (testing "needs-render? for pending segment"
    (let [seg (make-test-segment)]
      (is (seg/needs-render? seg))))

  (testing "needs-render? for dirty segment"
    (let [seg (seg/mark-dirty (make-test-segment))]
      (is (seg/needs-render? seg))))

  (testing "needs-render? false for cached segment"
    (let [seg (-> (make-test-segment)
                  seg/mark-rendering
                  (seg/mark-cached "/path.mp4"))]
      (is (not (seg/needs-render? seg)))))

  (testing "is-independent? for segment without deps"
    (let [seg (make-test-segment :deps #{})]
      (is (seg/is-independent? seg))))

  (testing "is-independent? false for segment with deps"
    (let [seg (make-test-segment :deps #{:other})]
      (is (not (seg/is-independent? seg)))))

  (testing "valid-state? for all valid states"
    (doseq [state [:pending :rendering :cached :dirty :error]]
      (is (seg/valid-state? state))))

  (testing "valid-state? false for invalid state"
    (is (not (seg/valid-state? :invalid)))
    (is (not (seg/valid-state? nil)))))

;; =============================================================================
;; Property-Based Tests
;; =============================================================================

(defspec segment-hash-deterministic 50
  (prop/for-all [seg g/gen-independent-segment]
                (let [hash1 (:hash seg)
          ;; Recreate with same params
                      seg2 (seg/create-segment
                            :id (:id seg)
                            :construct-fn (:construct-fn seg)
                            :deps (:deps seg)
                            :metadata (:metadata seg))]
                  (= hash1 (:hash seg2)))))

(defspec segment-state-always-valid 100
  (prop/for-all [seg g/gen-independent-segment
                 state g/gen-render-state]
                (let [seg-with-state (assoc seg :state state)]
                  (seg/valid-state? (:state seg-with-state)))))

(defspec segment-id-preserved-through-transitions 50
  (prop/for-all [seg g/gen-independent-segment]
                (let [original-id (:id seg)
                      after-dirty (seg/mark-dirty seg)
                      after-rendering (seg/mark-rendering after-dirty)
                      after-cached (seg/mark-cached after-rendering "/test.mp4")
                      after-pending (seg/mark-pending after-cached)]
                  (and (= original-id (:id after-dirty))
                       (= original-id (:id after-rendering))
                       (= original-id (:id after-cached))
                       (= original-id (:id after-pending))))))

(defspec segment-dependencies-preserved 50
  (prop/for-all [segments g/gen-acyclic-segments]
                (every? (fn [seg]
                          (set? (seg/dependencies seg)))
                        segments)))
