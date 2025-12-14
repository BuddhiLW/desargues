(ns verify-hot-reload
  "Interactive verification script for the hot-reload system.
   
   Run this in a REPL to test the full hot-reload cycle:
   
   1. (verify-hot-reload/run-all-tests!)
   
   Or run individual tests:
   
   2. (verify-hot-reload/test-init!)
   3. (verify-hot-reload/test-scene-graph!)
   4. (verify-hot-reload/test-watcher!)
   5. (verify-hot-reload/test-reload-cycle!)"
  (:require [desargues.devx.core :as devx]
            [desargues.devx.repl :as repl]
            [desargues.devx.segment :as seg]
            [desargues.devx.graph :as graph]
            [desargues.devx.watcher :as watcher]
            [desargues.devx.reload :as reload]
            [desargues.devx.ns-tracker :as tracker]
            [desargues.manim.core :as manim]
            [clojure.java.io :as io]))

;; =============================================================================
;; Test State
;; =============================================================================

(defonce test-results (atom {}))

(defn reset-results! []
  (reset! test-results {}))

(defn record-result! [test-name result]
  (swap! test-results assoc test-name result)
  result)

;; =============================================================================
;; Test 1: Initialization
;; =============================================================================

(defn test-init!
  "Test that Python/Manim initializes correctly."
  []
  (println "\n" (str (char 9552)) " TEST 1: Initialization")
  (println (apply str (repeat 50 (char 9472))))
  (try
    (devx/init!)
    (if (manim/initialized?*)
      (do
        (println "✓ Python/Manim initialized successfully")
        (record-result! :init {:success true}))
      (do
        (println "✗ Manim reports not initialized")
        (record-result! :init {:success false :error "Not initialized"})))
    (catch Exception e
      (println "✗ Initialization failed:" (.getMessage e))
      (record-result! :init {:success false :error (.getMessage e)}))))

;; =============================================================================
;; Test 2: Scene Graph Construction
;; =============================================================================

(defn test-scene-graph!
  "Test that scene graph is constructed correctly with source-ns metadata."
  []
  (println "\n" (str (char 9552)) " TEST 2: Scene Graph Construction")
  (println (apply str (repeat 50 (char 9472))))
  (try
    ;; Load the test namespace
    (require 'hot-reload-test :reload)
    (let [create-fn (resolve 'hot-reload-test/create-test-scene)
          scene-graph (create-fn)
          segments (graph/all-segments scene-graph)
          seg-ids (graph/segment-ids scene-graph)]

      (println "Scene graph created with" (count segments) "segments")
      (println "Segment IDs:" (vec seg-ids))

      ;; Check segments have source-ns metadata
      (println "\nChecking source-ns metadata:")
      (doseq [seg segments]
        (let [source-ns (tracker/segment-source-ns seg)]
          (println "  -" (seg/segment-id seg) "-> source-ns:" source-ns)))

      ;; Verify dependency structure
      (println "\nDependency structure:")
      (doseq [seg segments]
        (println "  -" (seg/segment-id seg) "deps:" (:deps seg)))

      ;; Verify render order
      (let [order (graph/render-order scene-graph)]
        (println "\nRender order:" (vec order)))

      ;; Store for later tests
      (repl/set-graph! scene-graph)

      (if (and (= 3 (count segments))
               (every? #(tracker/segment-source-ns %) segments))
        (do
          (println "\n✓ Scene graph constructed correctly")
          (record-result! :scene-graph {:success true :segments (vec seg-ids)}))
        (do
          (println "\n✗ Scene graph issues detected")
          (record-result! :scene-graph {:success false}))))
    (catch Exception e
      (println "✗ Scene graph construction failed:" (.getMessage e))
      (.printStackTrace e)
      (record-result! :scene-graph {:success false :error (.getMessage e)}))))

;; =============================================================================
;; Test 3: File Watcher
;; =============================================================================

(defn test-watcher!
  "Test that file watcher starts and can detect changes."
  []
  (println "\n" (str (char 9552)) " TEST 3: File Watcher")
  (println (apply str (repeat 50 (char 9472))))

  ;; Stop any existing watcher
  (watcher/stop-watcher!)

  (let [changes (atom [])
        test-callback (fn [event]
                        (println "  [detected]" (:namespace event) "-" (:file event))
                        (swap! changes conj event))]
    (try
      ;; Start watcher with test callback
      (println "Starting watcher on 'dev' directory...")
      (watcher/start-watcher! {:paths ["dev"]
                               :on-change test-callback
                               :debounce-ms 50})

      (if (watcher/watching?)
        (do
          (println "✓ Watcher started successfully")

          ;; Touch the test file to trigger a change
          (println "\nTouching hot_reload_test.clj to trigger change detection...")
          (Thread/sleep 200)

          (let [test-file (io/file "dev/hot_reload_test.clj")
                content (slurp test-file)]
            ;; Add a comment and save
            (spit test-file (str content "\n;; Touch: " (System/currentTimeMillis)))
            (Thread/sleep 500) ;; Wait for watcher to detect

            ;; Restore original content
            (spit test-file content))

          (let [detected (count @changes)]
            (println "\nChanges detected:" detected)
            (if (pos? detected)
              (do
                (println "✓ File watcher detected changes")
                (record-result! :watcher {:success true :changes-detected detected}))
              (do
                (println "✗ No changes detected (watcher may need more time)")
                (record-result! :watcher {:success false :error "No changes detected"})))))
        (do
          (println "✗ Watcher failed to start")
          (record-result! :watcher {:success false :error "Failed to start"})))

      (finally
        (watcher/stop-watcher!)
        (println "Watcher stopped.")))))

;; =============================================================================
;; Test 4: Reload Cycle (without rendering)
;; =============================================================================

(defn test-reload-cycle!
  "Test the full reload cycle: file change -> reload -> dirty marking."
  []
  (println "\n" (str (char 9552)) " TEST 4: Reload Cycle")
  (println (apply str (repeat 50 (char 9472))))

  (try
    ;; Ensure we have a scene graph
    (when-not (repl/get-graph)
      (require 'hot-reload-test :reload)
      (let [create-fn (resolve 'hot-reload-test/create-test-scene)]
        (repl/set-graph! (create-fn))))

    (let [graph-before (repl/get-graph)
          dirty-before (graph/dirty-segment-ids graph-before)]

      (println "Initial dirty segments:" (vec dirty-before))

      ;; Initialize clj-reload
      (println "\nInitializing clj-reload...")
      (reload/init-reload! :source-paths ["src" "dev"])

      ;; Get affected segments for the test namespace
      (let [affected (tracker/affected-segments graph-before 'hot-reload-test)]
        (println "Segments affected by 'hot-reload-test':" (vec affected)))

      ;; Simulate a namespace change by modifying a def
      (println "\nModifying test namespace...")
      (let [test-file (io/file "dev/hot_reload_test.clj")
            original-content (slurp test-file)
            ;; Change the test-message
            modified-content (clojure.string/replace
                              original-content
                              #"\"Hello from hot-reload test!\""
                              "\"Modified message for reload test!\"")]

        (try
          ;; Write modified content
          (spit test-file modified-content)
          (Thread/sleep 100)

          ;; Manually trigger reload
          (println "Triggering manual reload...")
          (let [reload-result (reload/safe-reload-namespace! 'hot-reload-test)]
            (println "Reload result:" (select-keys reload-result [:success? :reloaded]))

            ;; Recompute hashes and check for changes
            (let [graph (repl/get-graph)
                  affected-ids (tracker/affected-segments graph 'hot-reload-test)
                  {:keys [results graph]} (reload/recompute-affected-hashes graph affected-ids)
                  changed (filter :changed? results)]

              (println "\nHash recomputation results:")
              (doseq [r results]
                (println "  -" (:segment-id r)
                         (if (:changed? r) "[CHANGED]" "[unchanged]")))

              ;; Mark changed segments dirty
              (let [new-graph (reload/mark-changed-segments-dirty graph results)]
                (repl/set-graph! new-graph)
                (let [dirty-after (graph/dirty-segment-ids new-graph)]
                  (println "\nDirty segments after reload:" (vec dirty-after))

                  (if (seq changed)
                    (do
                      (println "\n✓ Reload cycle detected changes correctly")
                      (record-result! :reload {:success true
                                               :changed (mapv :segment-id changed)
                                               :dirty (vec dirty-after)}))
                    (do
                      (println "\n✗ No hash changes detected")
                      (record-result! :reload {:success false
                                               :error "No hash changes"})))))))

          (finally
            ;; Restore original content
            (spit test-file original-content)
            (Thread/sleep 100)
            ;; Reload to restore original state
            (reload/safe-reload-namespace! 'hot-reload-test)))))

    (catch Exception e
      (println "✗ Reload cycle failed:" (.getMessage e))
      (.printStackTrace e)
      (record-result! :reload {:success false :error (.getMessage e)}))))

;; =============================================================================
;; Test 5: Full Hot-Reload Integration
;; =============================================================================

(defn test-hot-reload-integration!
  "Test the full hot-reload system with watcher integration."
  []
  (println "\n" (str (char 9552)) " TEST 5: Hot-Reload Integration")
  (println (apply str (repeat 50 (char 9472))))

  (try
    ;; Ensure we have a scene graph
    (when-not (repl/get-graph)
      (require 'hot-reload-test :reload)
      (let [create-fn (resolve 'hot-reload-test/create-test-scene)]
        (repl/set-graph! (create-fn))))

    (println "Starting hot-reload watcher...")
    (println "(This uses the full repl/watch! integration)")

    ;; Use the REPL workflow
    (repl/watch! :paths ["dev"] :auto-render? false)

    (if (repl/watching?)
      (do
        (println "✓ Hot-reload watcher started")
        (println "\nCurrent status:")
        (repl/quick-status)

        ;; Show namespace tracking
        (println "\nNamespace tracking:")
        (repl/tracking-report)

        (println "\n✓ Hot-reload integration working")
        (record-result! :integration {:success true}))
      (do
        (println "✗ Hot-reload watcher failed to start")
        (record-result! :integration {:success false})))

    (catch Exception e
      (println "✗ Integration test failed:" (.getMessage e))
      (.printStackTrace e)
      (record-result! :integration {:success false :error (.getMessage e)}))

    (finally
      (repl/unwatch!))))

;; =============================================================================
;; Run All Tests
;; =============================================================================

(defn run-all-tests!
  "Run all verification tests in sequence."
  []
  (reset-results!)

  (println "\n")
  (println (apply str (repeat 60 (char 9552))))
  (println "     HOT-RELOAD VERIFICATION SUITE")
  (println (apply str (repeat 60 (char 9552))))

  (test-init!)
  (test-scene-graph!)
  (test-watcher!)
  (test-reload-cycle!)
  (test-hot-reload-integration!)

  ;; Summary
  (println "\n")
  (println (apply str (repeat 60 (char 9552))))
  (println "     SUMMARY")
  (println (apply str (repeat 60 (char 9552))))

  (let [results @test-results
        passed (count (filter #(:success (val %)) results))
        total (count results)]

    (doseq [[test-name result] results]
      (println (if (:success result) "✓" "✗")
               (name test-name)
               (when-not (:success result)
                 (str "- " (:error result "failed")))))

    (println)
    (println (format "Results: %d/%d tests passed" passed total))

    (if (= passed total)
      (println "\n🎉 All tests passed! Hot-reload system is working.")
      (println "\n⚠️  Some tests failed. Check output above for details."))

    {:passed passed :total total :results results}))

;; =============================================================================
;; Interactive Demo
;; =============================================================================

(defn demo!
  "Interactive demo of hot-reload.
   
   Instructions:
   1. Run (demo!) to start the watcher
   2. Edit dev/hot_reload_test.clj in another editor
   3. Watch the console for reload messages
   4. Run (repl/status) to see dirty segments
   5. Run (stop-demo!) to stop"
  []
  (println "\n=== Hot-Reload Demo ===")
  (println "1. Initializing...")
  (devx/init!)

  (println "2. Loading test scene...")
  (require 'hot-reload-test :reload)
  (let [create-fn (resolve 'hot-reload-test/create-test-scene)]
    (repl/set-graph! (create-fn)))

  (println "3. Starting watcher with auto-render DISABLED...")
  (repl/watch! :paths ["dev"] :auto-render? false)

  (println "\n" (apply str (repeat 50 (char 9472))))
  (println "Demo running! Try these:")
  (println "  - Edit dev/hot_reload_test.clj (change test-message)")
  (println "  - Run (repl/status) to see dirty segments")
  (println "  - Run (repl/quick-status) for one-line summary")
  (println "  - Run (stop-demo!) when done")
  (println (apply str (repeat 50 (char 9472)))))

(defn stop-demo!
  "Stop the demo watcher."
  []
  (repl/unwatch!)
  (println "Demo stopped."))
