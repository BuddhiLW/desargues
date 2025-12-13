(ns desargues.devx.events-test
  "Tests for the observer pattern event system."
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [desargues.devx.events :as events]))

;; =============================================================================
;; Fixture - Clean up observers after each test
;; =============================================================================

(defn cleanup-observers [f]
  (events/clear-observers!)
  (f)
  (events/clear-observers!))

(use-fixtures :each cleanup-observers)

;; =============================================================================
;; Observer Registration Tests
;; =============================================================================

(deftest test-register-observer
  (testing "Can register an observer"
    (events/register-observer! :test-obs (fn [_]))
    (is (contains? (set (events/list-observers)) :test-obs))))

(deftest test-unregister-observer
  (testing "Can unregister an observer"
    (events/register-observer! :temp-obs (fn [_]))
    (is (contains? (set (events/list-observers)) :temp-obs))
    (events/unregister-observer! :temp-obs)
    (is (not (contains? (set (events/list-observers)) :temp-obs)))))

(deftest test-clear-observers
  (testing "Can clear all observers"
    (events/register-observer! :obs1 (fn [_]))
    (events/register-observer! :obs2 (fn [_]))
    (is (= 2 (count (events/list-observers))))
    (events/clear-observers!)
    (is (empty? (events/list-observers)))))

;; =============================================================================
;; Event Emission Tests
;; =============================================================================

(deftest test-emit-delivers-to-observer
  (testing "emit! delivers events to registered observers"
    (let [received (atom nil)]
      (events/register-observer! :collector (fn [e] (reset! received e)))
      (events/emit! {:type :test-event :data 42})
      (is (= :test-event (:type @received)))
      (is (= 42 (:data @received))))))

(deftest test-emit-adds-timestamp
  (testing "emit! adds timestamp if not present"
    (let [received (atom nil)]
      (events/register-observer! :collector (fn [e] (reset! received e)))
      (events/emit! {:type :test-event})
      (is (number? (:timestamp @received))))))

(deftest test-emit-preserves-timestamp
  (testing "emit! preserves existing timestamp"
    (let [received (atom nil)
          custom-ts 12345]
      (events/register-observer! :collector (fn [e] (reset! received e)))
      (events/emit! {:type :test-event :timestamp custom-ts})
      (is (= custom-ts (:timestamp @received))))))

(deftest test-emit-delivers-to-multiple-observers
  (testing "emit! delivers to all registered observers"
    (let [count1 (atom 0)
          count2 (atom 0)]
      (events/register-observer! :obs1 (fn [_] (swap! count1 inc)))
      (events/register-observer! :obs2 (fn [_] (swap! count2 inc)))
      (events/emit! {:type :test-event})
      (is (= 1 @count1))
      (is (= 1 @count2)))))

;; =============================================================================
;; Convenience Emission Functions Tests
;; =============================================================================

(deftest test-emit-state-change
  (testing "emit-state-change! creates correct event"
    (let [received (atom nil)]
      (events/register-observer! :collector (fn [e] (reset! received e)))
      (events/emit-state-change! :intro :dirty :rendering)
      (is (= :state-change (:type @received)))
      (is (= :intro (:segment-id @received)))
      (is (= :dirty (:old-state @received)))
      (is (= :rendering (:new-state @received))))))

(deftest test-emit-render-complete
  (testing "emit-render-complete! creates correct event"
    (let [received (atom nil)]
      (events/register-observer! :collector (fn [e] (reset! received e)))
      (events/emit-render-complete! :intro :duration 2.5 :output-file "test.mp4")
      (is (= :render-complete (:type @received)))
      (is (= :intro (:segment-id @received)))
      (is (= 2.5 (:duration @received)))
      (is (= "test.mp4" (:output-file @received))))))

(deftest test-emit-render-error
  (testing "emit-render-error! creates correct event"
    (let [received (atom nil)]
      (events/register-observer! :collector (fn [e] (reset! received e)))
      (events/emit-render-error! :intro "Something went wrong")
      (is (= :render-error (:type @received)))
      (is (= :intro (:segment-id @received)))
      (is (= "Something went wrong" (:error @received))))))

;; =============================================================================
;; Filter Tests
;; =============================================================================

(deftest test-observer-with-filter
  (testing "Observer filter restricts which events are received"
    (let [received (atom [])]
      (events/register-observer! :filtered
                                 (fn [e] (swap! received conj e))
                                 :filter (fn [e] (= :render-complete (:type e))))
      (events/emit! {:type :state-change :segment-id :a})
      (events/emit! {:type :render-complete :segment-id :b})
      (events/emit! {:type :render-error :segment-id :c})
      (is (= 1 (count @received)))
      (is (= :render-complete (:type (first @received)))))))

(deftest test-state-change-filter
  (testing "state-change-filter matches specific transitions"
    (let [received (atom [])
          filter-fn (events/state-change-filter :to :cached)]
      (events/register-observer! :filtered
                                 (fn [e] (swap! received conj e))
                                 :filter filter-fn)
      (events/emit-state-change! :a :dirty :rendering)
      (events/emit-state-change! :b :rendering :cached)
      (events/emit-state-change! :c :dirty :error)
      (is (= 1 (count @received)))
      (is (= :b (:segment-id (first @received)))))))

(deftest test-segment-filter
  (testing "segment-filter matches specific segment IDs"
    (let [received (atom [])
          filter-fn (events/segment-filter #{:intro :outro})]
      (events/register-observer! :filtered
                                 (fn [e] (swap! received conj e))
                                 :filter filter-fn)
      (events/emit! {:type :render-start :segment-id :intro})
      (events/emit! {:type :render-start :segment-id :middle})
      (events/emit! {:type :render-start :segment-id :outro})
      (is (= 2 (count @received)))
      (is (= #{:intro :outro} (set (map :segment-id @received)))))))

;; =============================================================================
;; Built-in Observer Tests
;; =============================================================================

(deftest test-metrics-observer
  (testing "metrics-observer collects render statistics"
    (let [metrics (events/metrics-observer)]
      (events/register-observer! :metrics metrics)

      ;; Simulate some renders
      (events/emit-render-complete! :a :duration 1.0)
      (events/emit-render-complete! :b :duration 2.0)
      (events/emit-render-complete! :a :duration 1.5)
      (events/emit-render-error! :c "Failed")

      (let [stats (metrics :stats)]
        (is (= 3 (:render-count stats)))
        (is (= 1 (:error-count stats)))
        (is (= 4.5 (:total-duration stats)))
        (is (= 2 (get-in stats [:by-segment :a :count])))))))

(deftest test-metrics-observer-reset
  (testing "metrics-observer can be reset"
    (let [metrics (events/metrics-observer)]
      (events/register-observer! :metrics metrics)
      (events/emit-render-complete! :a :duration 1.0)
      (is (= 1 (:render-count (metrics :stats))))
      (metrics :reset)
      (is (= 0 (:render-count (metrics :stats)))))))

;; =============================================================================
;; Error Handling Tests
;; =============================================================================

(deftest test-observer-error-doesnt-stop-other-observers
  (testing "An error in one observer doesn't prevent others from receiving events"
    (let [received (atom 0)]
      (events/register-observer! :bad-observer (fn [_] (throw (Exception. "Boom!"))))
      (events/register-observer! :good-observer (fn [_] (swap! received inc)))
      (events/emit! {:type :test-event})
      (is (= 1 @received)))))
