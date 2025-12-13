(ns desargues.devx.specs-test
  "Tests for the specification pattern."
  (:require [clojure.test :refer [deftest testing is]]
            [desargues.devx.specs :as s]))

;; =============================================================================
;; Test Data
;; =============================================================================

(def test-graph
  {:segments {:intro {:id :intro :state :dirty :deps #{}
                      :metadata {:source-ns 'my.app :duration 5}}
              :main {:id :main :state :cached :deps #{:intro}
                     :metadata {:source-ns 'my.app}}
              :outro {:id :outro :state :pending :deps #{:main}
                      :metadata {:source-ns 'other.ns}}
              :error-seg {:id :error-seg :state :error :deps #{}}}})

;; =============================================================================
;; State Specification Tests
;; =============================================================================

(deftest test-dirty-spec
  (testing "dirty? matches dirty segments"
    (let [ids (set (s/find-segment-ids test-graph (s/dirty?)))]
      (is (contains? ids :intro))
      (is (not (contains? ids :main))))))

(deftest test-cached-spec
  (testing "cached? matches cached segments"
    (let [ids (set (s/find-segment-ids test-graph (s/cached?)))]
      (is (contains? ids :main))
      (is (not (contains? ids :intro))))))

(deftest test-pending-spec
  (testing "pending? matches pending segments"
    (let [ids (set (s/find-segment-ids test-graph (s/pending?)))]
      (is (contains? ids :outro)))))

(deftest test-error-spec
  (testing "error? matches error segments"
    (let [ids (set (s/find-segment-ids test-graph (s/error?)))]
      (is (contains? ids :error-seg)))))

(deftest test-state-spec
  (testing "state? matches specific state"
    (let [ids (set (s/find-segment-ids test-graph (s/state? :cached)))]
      (is (= #{:main} ids)))))

;; =============================================================================
;; Dependency Specification Tests
;; =============================================================================

(deftest test-depends-on-spec
  (testing "depends-on? matches segments with dependency"
    (let [ids (set (s/find-segment-ids test-graph (s/depends-on? :intro)))]
      (is (contains? ids :main))
      (is (not (contains? ids :intro))))))

(deftest test-independent-spec
  (testing "independent? matches segments without dependencies"
    (let [ids (set (s/find-segment-ids test-graph (s/independent?)))]
      (is (contains? ids :intro))
      (is (contains? ids :error-seg))
      (is (not (contains? ids :main))))))

(deftest test-has-dependents-spec
  (testing "has-dependents? matches segments with dependencies"
    (let [ids (set (s/find-segment-ids test-graph (s/has-dependents?)))]
      (is (contains? ids :main))
      (is (contains? ids :outro))
      (is (not (contains? ids :intro))))))

;; =============================================================================
;; Metadata Specification Tests
;; =============================================================================

(deftest test-has-metadata-spec
  (testing "has-metadata? matches segments with metadata key"
    (let [ids (set (s/find-segment-ids test-graph (s/has-metadata? :duration)))]
      (is (= #{:intro} ids)))))

(deftest test-metadata-equals-spec
  (testing "metadata-equals? matches specific value"
    (let [ids (set (s/find-segment-ids test-graph (s/metadata-equals? :source-ns 'my.app)))]
      (is (= #{:intro :main} ids)))))

;; =============================================================================
;; ID Pattern Specification Tests
;; =============================================================================

(deftest test-id-matches-spec
  (testing "id-matches? matches regex pattern"
    (let [ids (set (s/find-segment-ids test-graph (s/id-matches? #".*tro")))]
      (is (contains? ids :intro))
      (is (contains? ids :outro))
      (is (not (contains? ids :main))))))

;; =============================================================================
;; Custom Predicate Tests
;; =============================================================================

(deftest test-where-spec
  (testing "where matches custom predicate"
    (let [spec (s/where #(= 5 (get-in % [:metadata :duration])))
          ids (set (s/find-segment-ids test-graph spec))]
      (is (= #{:intro} ids)))))

;; =============================================================================
;; Composite Specification Tests
;; =============================================================================

(deftest test-and-spec
  (testing "and-spec requires all conditions"
    (let [spec (s/and-spec (s/dirty?) (s/independent?))
          ids (set (s/find-segment-ids test-graph spec))]
      (is (= #{:intro} ids)))))

(deftest test-or-spec
  (testing "or-spec matches any condition"
    (let [spec (s/or-spec (s/dirty?) (s/pending?))
          ids (set (s/find-segment-ids test-graph spec))]
      (is (= #{:intro :outro} ids)))))

(deftest test-not-spec
  (testing "not-spec negates condition"
    (let [spec (s/not-spec (s/cached?))
          ids (set (s/find-segment-ids test-graph spec))]
      (is (not (contains? ids :main)))
      (is (contains? ids :intro)))))

(deftest test-complex-composite
  (testing "Complex nested specifications"
    (let [spec (s/and-spec
                (s/or-spec (s/dirty?) (s/pending?))
                (s/not-spec (s/error?)))
          ids (set (s/find-segment-ids test-graph spec))]
      (is (= #{:intro :outro} ids)))))

;; =============================================================================
;; Query Function Tests
;; =============================================================================

(deftest test-count-segments
  (testing "count-segments returns correct count"
    (is (= 1 (s/count-segments test-graph (s/dirty?))))
    (is (= 2 (s/count-segments test-graph (s/independent?))))))

(deftest test-any-match
  (testing "any-match? returns boolean"
    (is (s/any-match? test-graph (s/dirty?)))
    (is (not (s/any-match? test-graph (s/state? :rendering))))))

(deftest test-all-match
  (testing "all-match? checks all segments"
    (is (not (s/all-match? test-graph (s/dirty?))))
    (is (s/all-match? test-graph (s/where #(keyword? (:id %)))))))

(deftest test-partition-by-spec
  (testing "partition-by-spec splits segments"
    (let [[matching not-matching] (s/partition-by-spec test-graph (s/dirty?))]
      (is (= 1 (count matching)))
      (is (= 3 (count not-matching))))))

;; =============================================================================
;; Convenience Specification Tests
;; =============================================================================

(deftest test-needs-render
  (testing "needs-render? matches dirty or pending"
    (let [ids (set (s/find-segment-ids test-graph (s/needs-render?)))]
      (is (= #{:intro :outro} ids)))))

(deftest test-from-namespace
  (testing "from-namespace? matches source namespace"
    (let [ids (set (s/find-segment-ids test-graph (s/from-namespace? 'my.app)))]
      (is (= #{:intro :main} ids)))))

;; =============================================================================
;; Spec Description Tests
;; =============================================================================

(deftest test-spec-descriptions
  (testing "Specifications have descriptions"
    (is (= "dirty" (s/spec-description (s/dirty?))))
    (is (= "cached" (s/spec-description (s/cached?))))
    (is (string? (s/spec-description (s/and-spec (s/dirty?) (s/cached?)))))))
