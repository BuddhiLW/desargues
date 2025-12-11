(ns desargues.devx.renderer
  "Segment Renderer - connects segment graph to Manim rendering.
   
   This namespace provides:
   - Single segment rendering
   - Batch rendering with parallelism
   - Partial file combination
   - Preview modes (last-frame, low-quality)
   
   ## Key Functions
   
   - render-segment!: Render a single segment
   - render-dirty!: Render all dirty segments
   - render-all!: Force full re-render
   - preview!: Quick preview of segment
   - combine!: Combine partial files into final video"
  (:require [desargues.devx.segment :as seg]
            [desargues.devx.graph :as graph]
            [desargues.manim.core :as manim]
            [libpython-clj2.python :as py]
            [libpython-clj2.python.class :as py-class]
            [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]))

;; =============================================================================
;; Configuration
;; =============================================================================

(def ^:dynamic *quality* :low)
(def ^:dynamic *preview-mode* false)
(def ^:dynamic *output-dir* "media/videos")

(def quality-settings
  {:low {:quality "low_quality" :fps 15 :height 480}
   :medium {:quality "medium_quality" :fps 30 :height 720}
   :high {:quality "high_quality" :fps 60 :height 1080}})

;; =============================================================================
;; Manim Scene Creation
;; =============================================================================

(defn- create-segment-scene
  "Create a Manim scene class for a segment."
  [segment]
  (let [construct-fn (:construct-fn segment)
        m (manim/manim)
        Scene (py/get-attr m "Scene")
        wrapped-construct (py-class/make-tuple-instance-fn
                           (fn [self] (construct-fn self)))]
    (py/create-class
     (str "Segment_" (name (seg/segment-id segment)))
     [Scene]
     {"construct" wrapped-construct})))

;; =============================================================================
;; Single Segment Rendering  
;; =============================================================================

(defn render-segment!
  "Render a single segment, returning updated segment with state.
   
   Options:
   - :quality - :low, :medium, :high
   - :preview - if true, only render last frame
   - :output-file - override output path"
  [segment & {:keys [quality preview output-file]
              :or {quality *quality* preview *preview-mode*}}]
  (manim/init!)
  (seg/ensure-partial-dir!)

  (let [seg-id (seg/segment-id segment)
        output (or output-file (seg/partial-file-path segment))
        config (py/get-attr (manim/manim) "config")
        q-settings (get quality-settings quality)]

    (println (format "Rendering segment: %s (%s quality)"
                     (name seg-id) (name quality)))

    ;; Configure Manim
    (py/set-attr! config "quality" (:quality q-settings))
    (py/set-attr! config "output_file" output)

    (when preview
      (py/set-attr! config "save_last_frame" true)
      (py/set-attr! config "write_to_movie" false))

    (try
      ;; Create and render scene
      (let [scene-class (create-segment-scene segment)
            scene (scene-class)
            start-time (System/currentTimeMillis)]

        (py/call-attr scene "render")

        (let [elapsed (/ (- (System/currentTimeMillis) start-time) 1000.0)]
          (println (format "  Completed in %.2fs" elapsed))

          ;; Return updated segment
          (-> segment
              (seg/mark-cached output)
              (update :metadata assoc
                      :render-time elapsed
                      :rendered-at (java.util.Date.)))))

      (catch Exception e
        (println (format "  ERROR: %s" (.getMessage e)))
        (seg/mark-error segment {:exception (str e)
                                 :message (.getMessage e)})))))

(defn preview-segment!
  "Quick preview - render only the last frame of a segment.
   Opens the resulting image."
  [segment]
  (let [result (render-segment! segment :preview true :quality :low)]
    (when (= :cached (seg/render-state result))
      ;; Try to open the image
      (let [img-path (clojure.string/replace (:partial-file result)
                                             #"\.mp4$" ".png")]
        (when (.exists (io/file img-path))
          (println (format "Preview saved to: %s" img-path))
          ;; Platform-specific open
          (try
            (sh "xdg-open" img-path)
            (catch Exception _
              (println "Could not auto-open image"))))))
    result))

;; =============================================================================
;; Graph Rendering
;; =============================================================================

(defn render-dirty!
  "Render all dirty segments in the graph, in dependency order.
   Returns updated graph."
  [graph & {:keys [quality parallel?]
            :or {quality *quality* parallel? false}}]
  (let [dirty (graph/dirty-segments graph)]
    (if (empty? dirty)
      (do (println "No dirty segments to render.")
          graph)
      (do
        (println (format "\nRendering %d dirty segment(s)..." (count dirty)))
        (reduce
         (fn [g segment]
           (let [result (render-segment! segment :quality quality)]
             (graph/update-segment g (seg/segment-id segment)
                                   (constantly result))))
         graph
         dirty)))))

(defn render-all!
  "Force re-render of all segments."
  [graph & opts]
  (let [dirty-graph (graph/mark-all-dirty graph)]
    (apply render-dirty! dirty-graph opts)))

(defn render-segment-by-id!
  "Render a specific segment by ID."
  [graph seg-id & opts]
  (if-let [segment (graph/get-segment graph seg-id)]
    (let [result (apply render-segment! segment opts)]
      (graph/update-segment graph seg-id (constantly result)))
    (throw (ex-info "Segment not found" {:segment seg-id}))))

;; =============================================================================
;; Parallel Rendering
;; =============================================================================

(defn render-parallel!
  "Render dirty segments in parallel where possible.
   Processes segments in waves based on dependency levels."
  [graph & {:keys [quality max-workers]
            :or {quality *quality* max-workers 4}}]
  (loop [g graph
         iteration 0]
    (let [batch (graph/next-render-batch g)]
      (if (empty? batch)
        (do
          (println (format "\nParallel render complete (%d iterations)" iteration))
          g)
        (do
          (println (format "\n=== Batch %d: %d segment(s) ==="
                           iteration (count batch)))
          (let [;; Mark batch as rendering
                g-rendering (reduce
                             (fn [gr seg]
                               (graph/update-segment gr (seg/segment-id seg)
                                                     seg/mark-rendering))
                             g batch)
                ;; Render in parallel (limited by max-workers)
                results (doall
                         (pmap
                          (fn [seg]
                            [(seg/segment-id seg)
                             (render-segment! seg :quality quality)])
                          (take max-workers batch)))
                ;; Update graph with results
                g-updated (reduce
                           (fn [gr [seg-id result]]
                             (graph/update-segment gr seg-id (constantly result)))
                           g-rendering
                           results)]
            (recur g-updated (inc iteration))))))))

;; =============================================================================
;; Partial File Combination
;; =============================================================================

(defn combine-partials!
  "Combine partial movie files into final output.
   Uses ffmpeg concatenation."
  [graph output-path]
  (let [cached (graph/cached-segments graph)
        ordered-files (->> (graph/render-order graph)
                           (map #(graph/get-segment graph %))
                           (filter #(= :cached (seg/render-state %)))
                           (map :partial-file)
                           (filter identity))]
    (if (empty? ordered-files)
      (println "No cached segments to combine.")
      (let [;; Create concat file for ffmpeg
            concat-file (str seg/*partial-dir* "/concat.txt")
            concat-content (clojure.string/join "\n"
                                                (map #(str "file '" (.getAbsolutePath (io/file %)) "'")
                                                     ordered-files))]
        (spit concat-file concat-content)

        (println (format "Combining %d segments into %s"
                         (count ordered-files) output-path))

        (let [result (sh "ffmpeg" "-y" "-f" "concat" "-safe" "0"
                         "-i" concat-file
                         "-c" "copy" output-path)]
          (if (zero? (:exit result))
            (do
              (println "Successfully created:" output-path)
              output-path)
            (do
              (println "ffmpeg error:" (:err result))
              nil)))))))

;; =============================================================================
;; REPL Convenience Functions
;; =============================================================================

(defonce ^:private graph-atom (atom nil))

(defn set-graph!
  "Set the current working graph for REPL convenience functions."
  [graph]
  (reset! graph-atom graph))

(defn get-graph
  "Get the current working graph."
  []
  @graph-atom)

(defn status
  "Print status of current graph."
  []
  (if-let [g @graph-atom]
    (graph/print-status g)
    (println "No graph loaded. Use (set-graph! g) first.")))

(defn render!
  "Render dirty segments in current graph."
  [& {:keys [quality parallel?] :as opts}]
  (if-let [g @graph-atom]
    (let [result (apply render-dirty! g (flatten (seq opts)))]
      (reset! graph-atom result)
      (status))
    (println "No graph loaded. Use (set-graph! g) first.")))

(defn preview!
  "Preview a segment by ID."
  [seg-id]
  (if-let [g @graph-atom]
    (if-let [seg (graph/get-segment g seg-id)]
      (let [result (preview-segment! seg)]
        (swap! graph-atom graph/update-segment seg-id (constantly result)))
      (println "Segment not found:" seg-id))
    (println "No graph loaded. Use (set-graph! g) first.")))

(defn dirty!
  "Mark a segment (and dependents) as dirty."
  [seg-id]
  (if-let [g @graph-atom]
    (do
      (swap! graph-atom graph/mark-dirty! seg-id)
      (status))
    (println "No graph loaded.")))

(defn combine!
  "Combine cached segments into final video."
  [output-path]
  (if-let [g @graph-atom]
    (combine-partials! g output-path)
    (println "No graph loaded.")))

;; =============================================================================
;; Quality Helpers
;; =============================================================================

(defmacro with-quality
  "Execute body with specified quality setting."
  [quality & body]
  `(binding [*quality* ~quality]
     ~@body))

(defmacro with-preview
  "Execute body in preview mode."
  [& body]
  `(binding [*preview-mode* true
             *quality* :low]
     ~@body))
