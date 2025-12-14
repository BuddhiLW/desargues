(ns hot-reload-test
  "Test namespace for verifying hot-reload functionality.
   
   This file is designed to be modified during hot-reload testing
   to verify that:
   1. File watcher detects changes
   2. Namespace is reloaded
   3. Affected segments are marked dirty
   4. Segment hashes are recomputed"
  (:require [desargues.devx.core :as devx]
            [desargues.devx.segment :as seg]
            [desargues.manim.core :as manim]))

;; =============================================================================
;; Test Configuration
;; =============================================================================

(def test-message "Hello from hot-reload test!")

(def animation-color "BLUE")

;; =============================================================================
;; Test Segments
;; =============================================================================

;; Segment 1: Simple intro
(devx/defsegment intro
  "Introduction segment - shows a title"
  [scene]
  (let [Text (manim/get-class "Text")
        Write (manim/get-class "Write")
        title (Text test-message)]
    (manim/play! scene (Write title))
    (manim/wait! scene 1)))

;; Segment 2: Depends on intro
(devx/defsegment main-circle
  "Main content - creates a colored circle"
  :deps #{:intro}
  [scene]
  (let [Circle (manim/get-class "Circle")
        Create (manim/get-class "Create")
        color-const (manim/get-constant animation-color)
        circle (Circle)]
    (manim/call-method circle "set_color" color-const)
    (manim/play! scene (Create circle))
    (manim/wait! scene 1)))

;; Segment 3: Depends on main-circle
(devx/defsegment outro
  "Outro - fades everything out"
  :deps #{:main-circle}
  [scene]
  (let [FadeOut (manim/get-class "FadeOut")
        mobjects (manim/call-method scene "get_mobjects")]
    (manim/play! scene (FadeOut mobjects))
    (manim/wait! scene 0.5)))

;; =============================================================================
;; Scene Graph
;; =============================================================================

(defn create-test-scene
  "Create the test scene graph with all segments."
  []
  (devx/scene [intro main-circle outro]
              :title "Hot Reload Test"
              :description "Scene for testing hot-reload functionality"))

;; =============================================================================
;; REPL Test Helpers
;; =============================================================================

(defn test-info
  "Print current test configuration."
  []
  (println "\n=== Hot Reload Test Info ===")
  (println "Message:" test-message)
  (println "Color:" animation-color)
  (println "\nSegments defined:")
  (println "  - intro (independent)")
  (println "  - main-circle (deps: intro)")
  (println "  - outro (deps: main-circle)"))
