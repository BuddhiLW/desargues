(ns desargues.devx.backend-test
  "Tests for the render backend strategy pattern."
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [desargues.devx.backend :as backend]))

;; =============================================================================
;; Test Fixtures
;; =============================================================================

(defn reset-backend [f]
  (let [prev (backend/current-backend-impl)]
    (f)
    (when prev
      (backend/set-backend! (backend/backend-name prev)))))

(use-fixtures :each reset-backend)

;; =============================================================================
;; Protocol Tests
;; =============================================================================

(deftest test-mock-backend-satisfies-protocol
  (testing "MockBackend satisfies IRenderBackend"
    (let [mock (backend/mock-backend)]
      (is (satisfies? backend/IRenderBackend mock)))))

(deftest test-mock-backend-name
  (testing "MockBackend returns correct name"
    (let [mock (backend/mock-backend)]
      (is (= :mock (backend/backend-name mock))))))

;; =============================================================================
;; Registry Tests
;; =============================================================================

(deftest test-list-backends
  (testing "Mock backend is registered by default"
    (is (contains? (set (backend/list-backends)) :mock))))

(deftest test-get-backend-by-keyword
  (testing "Can get backend by keyword"
    (let [mock (backend/get-backend :mock)]
      (is (satisfies? backend/IRenderBackend mock)))))

(deftest test-get-backend-direct
  (testing "Can pass backend directly"
    (let [mock (backend/mock-backend)]
      (is (= mock (backend/get-backend mock))))))

(deftest test-get-unknown-backend-throws
  (testing "Getting unknown backend throws"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Unknown backend"
                          (backend/get-backend :nonexistent)))))

(deftest test-set-backend
  (testing "Can set current backend"
    (backend/set-backend! :mock)
    (is (= :mock (backend/backend-name (backend/current-backend-impl))))))

;; =============================================================================
;; Mock Backend Rendering Tests
;; =============================================================================

(deftest test-mock-render-segment
  (testing "Mock backend renders segment"
    (let [mock (backend/mock-backend)
          segment {:id :test-seg :construct-fn identity :state :dirty}
          result (backend/render-segment mock segment {:quality :low})]
      (is (= :cached (:state result)))
      (is (string? (:partial-file result)))
      (is (= :mock (get-in result [:metadata :backend]))))))

(deftest test-mock-render-with-custom-output
  (testing "Mock backend respects custom output path"
    (let [mock (backend/mock-backend)
          segment {:id :test-seg :construct-fn identity :state :dirty}
          result (backend/render-segment mock segment {:output-file "custom/path.mp4"})]
      (is (= "custom/path.mp4" (:partial-file result))))))

(deftest test-mock-preview-segment
  (testing "Mock backend preview works"
    (let [mock (backend/mock-backend)
          segment {:id :test-seg :construct-fn identity :state :dirty}
          result (backend/preview-segment mock segment)]
      (is (= :cached (:state result))))))

(deftest test-mock-combine-videos
  (testing "Mock backend combine returns output path"
    (let [mock (backend/mock-backend)
          result (backend/combine-videos mock ["a.mp4" "b.mp4"] "out.mp4")]
      (is (= "out.mp4" result)))))

(deftest test-mock-render-delay
  (testing "Mock backend respects render delay"
    (let [mock (backend/mock-backend :render-delay-ms 50)
          segment {:id :test-seg :construct-fn identity :state :dirty}
          start (System/currentTimeMillis)
          _ (backend/render-segment mock segment {})
          elapsed (- (System/currentTimeMillis) start)]
      (is (>= elapsed 50)))))

;; =============================================================================
;; Context Macro Tests
;; =============================================================================

(deftest test-with-backend-macro
  (testing "with-backend temporarily changes backend"
    (backend/set-backend! :mock)
    (let [custom-mock (backend/mock-backend :render-delay-ms 10)]
      (backend/register-backend! :custom-mock custom-mock)

      ;; Inside with-backend, current backend changes
      (backend/with-backend :custom-mock
        (is (= :mock (backend/backend-name (backend/current-backend-impl)))))

      ;; After with-backend, original is restored
      (is (= :mock (backend/backend-name (backend/current-backend-impl)))))))

;; =============================================================================
;; Custom Backend Registration Tests
;; =============================================================================

(defrecord TestBackend []
  backend/IRenderBackend
  (backend-name [_] :test)
  (init-backend! [_] nil)
  (render-segment [_ segment _] (assoc segment :state :cached))
  (preview-segment [_ segment] (assoc segment :state :cached))
  (combine-videos [_ _ path] path))

(deftest test-register-custom-backend
  (testing "Can register custom backend"
    (let [custom (->TestBackend)]
      (backend/register-backend! :test custom)
      (is (contains? (set (backend/list-backends)) :test))
      (is (= :test (backend/backend-name (backend/get-backend :test)))))))
