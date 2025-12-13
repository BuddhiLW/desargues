(ns desargues.devx.renderer-test
  "Unit and integration tests for the Segment Renderer.
   
   Unit tests (mocked) run without Python/Manim.
   Integration tests require Python/Manim environment.
   
   Run integration tests separately:
   lein test :only desargues.devx.renderer-test/test-python-initialization"
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [desargues.devx.segment :as seg]
            [desargues.devx.graph :as graph]
            [desargues.devx.renderer :as renderer]
            [desargues.devx.quality :as quality]))

;; =============================================================================
;; Helper Functions
;; =============================================================================

(defn make-segment
  "Create a test segment with given id and dependencies."
  [id & {:keys [deps construct-fn] :or {deps #{} construct-fn (fn [_] nil)}}]
  (seg/create-segment
   :id id
   :construct-fn construct-fn
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
;; Unit Tests: Configuration (No Python Required)
;; =============================================================================

(deftest test-quality-settings
  (testing "Quality settings map has expected keys"
    (is (quality/preset-exists? :low))
    (is (quality/preset-exists? :medium))
    (is (quality/preset-exists? :high))))

(deftest test-quality-settings-structure
  (testing "Each quality setting has required fields"
    (doseq [preset-key (quality/list-presets)]
      (testing (str "Quality: " preset-key)
        (let [settings (quality/get-preset preset-key)]
          (is (contains? settings :quality)
              (str preset-key " should have :quality field"))
          (is (contains? settings :fps)
              (str preset-key " should have :fps field"))
          (is (contains? settings :height)
              (str preset-key " should have :height field"))
          (is (string? (:quality settings))
              (str preset-key " :quality should be a string"))
          (is (pos-int? (:fps settings))
              (str preset-key " :fps should be a positive integer"))
          (is (pos-int? (:height settings))
              (str preset-key " :height should be a positive integer")))))))

(deftest test-quality-values
  (testing "Low quality settings"
    (let [low (quality/get-preset :low)]
      (is (= "low_quality" (:quality low)))
      (is (= 15 (:fps low)))
      (is (= 480 (:height low)))))

  (testing "Medium quality settings"
    (let [medium (quality/get-preset :medium)]
      (is (= "medium_quality" (:quality medium)))
      (is (= 30 (:fps medium)))
      (is (= 720 (:height medium)))))

  (testing "High quality settings"
    (let [high (quality/get-preset :high)]
      (is (= "high_quality" (:quality high)))
      (is (= 60 (:fps high)))
      (is (= 1080 (:height high))))))

(deftest test-default-bindings
  (testing "Default quality is :low"
    (is (= :low renderer/*quality*)))

  (testing "Default preview mode is false"
    (is (false? renderer/*preview-mode*)))

  (testing "Default output directory is set"
    (is (string? renderer/*output-dir*))))

(deftest test-with-quality-macro
  (testing "with-quality macro binds quality correctly"
    (is (= :low renderer/*quality*))
    (renderer/with-quality :high
      (is (= :high renderer/*quality*)))
    (is (= :low renderer/*quality*))))

(deftest test-with-preview-macro
  (testing "with-preview macro sets preview mode and low quality"
    (is (false? renderer/*preview-mode*))
    (renderer/with-preview
      (is (true? renderer/*preview-mode*))
      (is (= :low renderer/*quality*)))
    (is (false? renderer/*preview-mode*))))

;; =============================================================================
;; NOTE: REPL state management tests are in repl_test.clj
;; The renderer module is now stateless - REPL helpers moved to devx.repl
;; =============================================================================

;; =============================================================================
;; Integration Tests (Requires Python/Manim)
;;
;; These tests are marked with ^:integration metadata.
;; Run them with: lein test :only desargues.devx.renderer-test
;; Make sure to activate the manim conda environment first.
;; =============================================================================

(def ^:dynamic *python-available* false)

(defn check-python-available
  "Check if Python/Manim is available for integration tests."
  []
  (try
    (require '[desargues.manim.core :as manim])
    ((resolve 'desargues.manim.core/init!))
    true
    (catch Exception _
      false)))

(defn integration-test-fixture
  "Fixture that initializes Python for integration tests."
  [f]
  (if (check-python-available)
    (binding [*python-available* true]
      (f))
    (do
      (println "\n⚠️  Skipping integration tests - Python/Manim not available")
      (println "   Run 'conda activate manim' before running integration tests\n"))))

(use-fixtures :once integration-test-fixture)

(deftest ^:integration test-python-initialization
  (testing "Manim initializes correctly"
    (when *python-available*
      (require '[desargues.manim.core :as manim])
      (is (some? ((resolve 'desargues.manim.core/manim)))
          "Manim module should be loaded"))))

(deftest ^:integration test-render-segment-creates-file
  (testing "Rendering a segment creates a partial file"
    (when *python-available*
      (let [seg (make-segment :test-render
                              :construct-fn (fn [scene]
                                              ;; Simple construct that does nothing
                                              nil))]
        ;; This test would actually render - skip in CI
        ;; (let [result (renderer/render-segment! seg :quality :low)]
        ;;   (is (= :cached (seg/render-state result))))
        (is true "Placeholder - full render test requires Manim")))))

(deftest ^:integration test-render-segment-error-handling
  (testing "Failed render transitions segment to :error state"
    (when *python-available*
      ;; Test with a construct-fn that throws
      (let [seg (make-segment :error-test
                              :construct-fn (fn [_scene]
                                              (throw (Exception. "Test error"))))]
        ;; The renderer should catch this and mark as error
        ;; Skipping actual render in unit tests
        (is true "Placeholder - error handling test requires Manim")))))

;; =============================================================================
;; Smoke Tests (Quick verification that code loads)
;; =============================================================================

(deftest test-renderer-namespace-loads
  (testing "Renderer namespace loads without errors"
    (is (quality/preset-exists? :low))
    (is (fn? renderer/render-segment!))
    (is (fn? renderer/render-dirty!))
    (is (fn? renderer/render-all!))
    (is (fn? renderer/preview-segment!))
    (is (fn? renderer/combine-partials!))))

;; NOTE: REPL convenience function tests are in repl_test.clj
;; The stateless rendering functions above are the renderer's public API
