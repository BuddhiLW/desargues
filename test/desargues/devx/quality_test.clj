(ns desargues.devx.quality-test
  "Tests for the extensible quality settings system."
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [desargues.devx.quality :as quality]))

;; =============================================================================
;; Fixture - Clean up custom presets after tests
;; =============================================================================

(defn cleanup-custom-presets [f]
  (f)
  ;; Remove any test presets
  (quality/unregister-preset! :test-preset)
  (quality/unregister-preset! :another-test))

(use-fixtures :each cleanup-custom-presets)

;; =============================================================================
;; Built-in Presets Tests
;; =============================================================================

(deftest test-builtin-presets-exist
  (testing "Built-in presets are available"
    (is (quality/preset-exists? :low))
    (is (quality/preset-exists? :medium))
    (is (quality/preset-exists? :high))))

(deftest test-builtin-preset-values
  (testing ":low preset has correct values"
    (let [settings (quality/get-preset :low)]
      (is (= "low_quality" (:quality settings)))
      (is (= 15 (:fps settings)))
      (is (= 480 (:height settings)))))

  (testing ":high preset has correct values"
    (let [settings (quality/get-preset :high)]
      (is (= "high_quality" (:quality settings)))
      (is (= 60 (:fps settings)))
      (is (= 1080 (:height settings))))))

;; =============================================================================
;; Custom Preset Registration Tests (OCP)
;; =============================================================================

(deftest test-register-custom-preset
  (testing "Can register a custom quality preset"
    (quality/register-preset! :test-preset
                              {:quality "test_quality"
                               :fps 24
                               :height 360
                               :description "Test preset"})
    (is (quality/preset-exists? :test-preset))
    (is (= 24 (:fps (quality/get-preset :test-preset))))))

(deftest test-unregister-preset
  (testing "Can unregister a custom preset"
    (quality/register-preset! :another-test
                              {:quality "another"
                               :fps 30
                               :height 720})
    (is (quality/preset-exists? :another-test))
    (quality/unregister-preset! :another-test)
    (is (not (quality/preset-exists? :another-test)))))

(deftest test-list-presets
  (testing "list-presets returns all registered presets"
    (let [presets (set (quality/list-presets))]
      (is (contains? presets :low))
      (is (contains? presets :medium))
      (is (contains? presets :high)))))

;; =============================================================================
;; Quality Resolution Tests
;; =============================================================================

(deftest test-resolve-keyword-preset
  (testing "resolve-quality works with keyword presets"
    (let [settings (quality/resolve-quality :low)]
      (is (= "low_quality" (:quality settings)))
      (is (= 15 (:fps settings))))))

(deftest test-resolve-direct-map
  (testing "resolve-quality works with direct settings map"
    (let [direct-settings {:quality "custom_quality"
                           :fps 45
                           :height 900}
          resolved (quality/resolve-quality direct-settings)]
      (is (= "custom_quality" (:quality resolved)))
      (is (= 45 (:fps resolved)))
      (is (= 900 (:height resolved))))))

(deftest test-resolve-unknown-preset-throws
  (testing "resolve-quality throws for unknown preset"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Unknown quality preset"
                          (quality/resolve-quality :nonexistent)))))

;; =============================================================================
;; Convenience Function Tests
;; =============================================================================

(deftest test-convenience-functions
  (testing "quality-string returns Manim quality string"
    (is (= "low_quality" (quality/quality-string :low)))
    (is (= "high_quality" (quality/quality-string :high))))

  (testing "fps returns frames per second"
    (is (= 15 (quality/fps :low)))
    (is (= 60 (quality/fps :high))))

  (testing "height returns resolution height"
    (is (= 480 (quality/height :low)))
    (is (= 1080 (quality/height :high)))))

(deftest test-describe-preset
  (testing "describe-preset returns human-readable description"
    (is (string? (quality/describe-preset :low)))
    (is (nil? (quality/describe-preset :nonexistent)))))
