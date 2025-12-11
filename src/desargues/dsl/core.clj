(ns desargues.dsl.core
  "High-level DSL for mathematical animations.

   This namespace provides a declarative, composable approach to building
   mathematical animations. It leverages Clojure's strengths:
   - Data as code: Scenes are data structures
   - Functional composition: Animations compose like functions
   - Macros: DSL syntax that compiles to efficient code
   - Protocols: Extensible abstractions

   ## Architecture (DDD/SOLID)

   Domain Layer:
   - `Animatable` protocol - anything that can be animated
   - `Reactive` protocol - reactive state containers
   - `Composable` protocol - things that can be combined

   Application Layer:
   - Scene builders
   - Animation combinators
   - Physics simulators

   Infrastructure Layer:
   - Manim interop (separate namespace)

   ## Example Usage:

   ```clojure
   (defscene my-scene []
     (let [circle (shape :circle {:color :blue :radius 1})
           square (shape :square {:side 2})]
       (-> (timeline)
           (at 0 (create circle))
           (at 1 (transform circle square))
           (at 2 (fade-out square))
           (run!))))
   ```"
  (:require [clojure.spec.alpha :as s]))

;; =============================================================================
;; Protocols - The DNA of our system
;; =============================================================================

(defprotocol Animatable
  "Protocol for objects that can be animated."
  (animate [this animation-type opts]
    "Apply an animation to this object.")
  (get-state [this]
    "Get current state as a map.")
  (set-state! [this state]
    "Set state from a map."))

(defprotocol Reactive
  "Protocol for reactive state containers.
   Like ValueTracker but functional."
  (get-value [this]
    "Get current value.")
  (set-value! [this v]
    "Set value, triggering watchers.")
  (add-watcher! [this key fn]
    "Add a watcher function (fn [old new] ...).")
  (remove-watcher! [this key]
    "Remove a watcher by key."))

(defprotocol Composable
  "Protocol for composable entities."
  (compose [this other]
    "Compose two entities.")
  (decompose [this]
    "Decompose into constituent parts."))

(defprotocol Temporal
  "Protocol for time-varying entities."
  (at-time [this t]
    "Get state at time t.")
  (duration [this]
    "Get total duration."))

(defprotocol Renderable
  "Protocol for things that can be rendered."
  (render! [this context]
    "Render to a context (scene, canvas, etc).")
  (to-manim [this]
    "Convert to Manim mobject."))

;; =============================================================================
;; Reactive State - Clojure's answer to ValueTracker
;; =============================================================================

(defrecord ReactiveValue [state watchers]
  Reactive
  (get-value [_] @state)
  (set-value! [_ v]
    (let [old @state]
      (reset! state v)
      (doseq [[_ f] @watchers]
        (f old v))))
  (add-watcher! [_ key f]
    (swap! watchers assoc key f))
  (remove-watcher! [_ key]
    (swap! watchers dissoc key)))

(defn reactive
  "Create a reactive value container.

   ```clojure
   (def x (reactive 0))
   (add-watcher! x :log (fn [old new] (println \"Changed:\" old \"->\" new)))
   (set-value! x 5)  ; prints \"Changed: 0 -> 5\"
   ```"
  [initial-value]
  (->ReactiveValue (atom initial-value) (atom {})))

(defn derive-reactive
  "Create a derived reactive value that updates when sources change.

   ```clojure
   (def a (reactive 1))
   (def b (reactive 2))
   (def sum (derive-reactive [a b] +))
   (get-value sum)  ; => 3
   (set-value! a 10)
   (get-value sum)  ; => 12
   ```"
  [sources f]
  (let [derived (reactive (apply f (map get-value sources)))]
    (doseq [[i source] (map-indexed vector sources)]
      (add-watcher! source (gensym "derive")
                    (fn [_ _]
                      (set-value! derived (apply f (map get-value sources))))))
    derived))

;; =============================================================================
;; Animation Combinators - Functional composition of animations
;; =============================================================================

(defrecord Animation [type target opts duration-ms]
  Temporal
  (at-time [this t]
    (let [progress (/ t (/ duration-ms 1000.0))]
      {:type type
       :target target
       :progress (min 1.0 (max 0.0 progress))
       :opts opts}))
  (duration [_] (/ duration-ms 1000.0)))

(defn animation
  "Create an animation specification."
  [type target & {:keys [duration] :or {duration 1000} :as opts}]
  (->Animation type target (dissoc opts :duration) duration))

;; Animation combinators
(defn sequential
  "Combine animations to run one after another.

   ```clojure
   (sequential
     (animation :create circle)
     (animation :transform circle square)
     (animation :fade-out square))
   ```"
  [& animations]
  (let [total-duration (reduce + (map duration animations))]
    (reify
      Temporal
      (at-time [_ t]
        (loop [anims animations
               elapsed 0]
          (when-let [anim (first anims)]
            (let [d (duration anim)]
              (if (< t (+ elapsed d))
                (at-time anim (- t elapsed))
                (recur (rest anims) (+ elapsed d)))))))
      (duration [_] total-duration)

      Composable
      (compose [this other] (sequential this other))
      (decompose [_] animations))))

(defn parallel
  "Combine animations to run simultaneously.

   ```clojure
   (parallel
     (animation :rotate circle :angle TAU)
     (animation :scale circle :factor 2))
   ```"
  [& animations]
  (let [max-duration (apply max (map duration animations))]
    (reify
      Temporal
      (at-time [_ t]
        (mapv #(at-time % t) animations))
      (duration [_] max-duration)

      Composable
      (compose [this other] (parallel this other))
      (decompose [_] animations))))

(defn staggered
  "Combine animations with staggered start times.

   ```clojure
   (staggered 0.1  ; 100ms between starts
     (animation :fade-in obj1)
     (animation :fade-in obj2)
     (animation :fade-in obj3))
   ```"
  [lag-ratio & animations]
  (let [lag-seconds (* lag-ratio (duration (first animations)))
        offsets (map #(* % lag-seconds) (range (count animations)))
        total-duration (+ (last offsets) (duration (last animations)))]
    (reify
      Temporal
      (at-time [_ t]
        (mapv (fn [anim offset]
                (when (>= t offset)
                  (at-time anim (- t offset))))
              animations offsets))
      (duration [_] total-duration)

      Composable
      (compose [this other] (sequential this other))
      (decompose [_] animations))))

;; Convenience aliases using threading-friendly names
(def >> sequential)
(def || parallel)
(def |> staggered)

;; =============================================================================
;; Scene DSL - Declarative scene construction
;; =============================================================================

(defrecord Scene [name objects timeline camera])

(defn scene
  "Create a new scene."
  [name & {:keys [camera] :or {camera :default}}]
  (->Scene name (atom {}) (atom []) (atom camera)))

(defn add-object!
  "Add an object to the scene."
  [scene id object]
  (swap! (:objects scene) assoc id object)
  scene)

(defn get-object
  "Get an object from the scene by id."
  [scene id]
  (get @(:objects scene) id))

(defn schedule!
  "Schedule an animation at a specific time."
  [scene time animation]
  (swap! (:timeline scene) conj {:time time :animation animation})
  scene)

;; =============================================================================
;; Timeline DSL - Fluent API for building timelines
;; =============================================================================

(defrecord Timeline [events cursor])

(defn timeline
  "Create a new timeline starting at time 0."
  []
  (->Timeline (atom []) (atom 0)))

(defn at
  "Add an event at a specific time."
  [tl time event]
  (swap! (:events tl) conj {:time time :event event})
  (reset! (:cursor tl) (+ time (if (satisfies? Temporal event)
                                 (duration event)
                                 1)))
  tl)

(defn then
  "Add an event after the current cursor position."
  [tl event]
  (at tl @(:cursor tl) event))

(defn wait
  "Add a wait period."
  [tl seconds]
  (swap! (:cursor tl) + seconds)
  tl)

(defn get-events
  "Get all events sorted by time."
  [tl]
  (sort-by :time @(:events tl)))

;; =============================================================================
;; Updater System - Declarative bindings between objects
;; =============================================================================

(defn bind!
  "Bind a property of one object to track another.

   ```clojure
   (bind! label :position dot :center)
   ; label's position will follow dot's center
   ```"
  [target target-prop source source-prop & {:keys [transform] :or {transform identity}}]
  (add-watcher! source (gensym "bind")
                (fn [_ _]
                  (let [state (get-state source)
                        new-val (transform (get state source-prop))]
                    (set-state! target {target-prop new-val})))))

(defn always
  "Create an updater that continuously updates based on a function.

   ```clojure
   (always label
     (fn [self]
       (set-state! self {:position (get-state dot :center)})))
   ```"
  [target update-fn]
  {:type :updater
   :target target
   :fn update-fn})

;; =============================================================================
;; Mathematical Objects - High-level constructors
;; =============================================================================

(defn shape
  "Create a shape specification.

   ```clojure
   (shape :circle {:radius 1 :color :blue})
   (shape :square {:side 2 :fill-opacity 0.5})
   (shape :line {:start [0 0] :end [1 1]})
   ```"
  [type opts]
  {:type :shape
   :shape-type type
   :opts opts})

(defn tex
  "Create a LaTeX text specification with automatic color mapping.

   ```clojure
   (tex \"x^2 + y^2 = r^2\"
        :colors {\"x\" :blue \"y\" :red \"r\" :green})
   ```"
  [content & {:keys [colors font-size] :or {font-size 48}}]
  {:type :tex
   :content content
   :colors (or colors {})
   :font-size font-size})

(defn axes
  "Create axes specification.

   ```clojure
   (axes {:x [-5 5] :y [-3 3]}
         :labels true
         :grid true)
   ```"
  [ranges & {:keys [labels grid tips] :or {labels false grid false tips true}}]
  {:type :axes
   :ranges ranges
   :labels labels
   :grid grid
   :tips tips})

(defn graph
  "Create a function graph specification.

   ```clojure
   (graph sin {:x-range [0 (* 2 Math/PI)]
               :color :blue
               :stroke-width 3})
   ```"
  [f opts]
  {:type :graph
   :function f
   :opts opts})

(defn parametric
  "Create a parametric curve specification.

   ```clojure
   (parametric (fn [t] [(Math/cos t) (Math/sin t)])
               {:t-range [0 (* 2 Math/PI)]
                :color :yellow})
   ```"
  [f opts]
  {:type :parametric
   :function f
   :opts opts})

(defn vector-field
  "Create a vector field specification.

   ```clojure
   (vector-field (fn [[x y]] [(- y) x])
                 {:x-range [-3 3]
                  :y-range [-3 3]
                  :color-fn (fn [v] (magnitude v))})
   ```"
  [f opts]
  {:type :vector-field
   :function f
   :opts opts})

;; =============================================================================
;; Physics Simulation - Declarative physics
;; =============================================================================

(defprotocol Physical
  "Protocol for objects with physics."
  (get-position [this])
  (get-velocity [this])
  (apply-force! [this force])
  (step! [this dt]))

(defrecord Particle [position velocity mass forces]
  Physical
  (get-position [_] @position)
  (get-velocity [_] @velocity)
  (apply-force! [_ force]
    (swap! forces conj force))
  (step! [this dt]
    (let [total-force (reduce (fn [acc f]
                                (mapv + acc (if (fn? f) (f this) f)))
                              [0 0 0]
                              @forces)
          acceleration (mapv #(/ % mass) total-force)
          new-velocity (mapv + @velocity (mapv #(* % dt) acceleration))
          new-position (mapv + @position (mapv #(* % dt) new-velocity))]
      (reset! velocity new-velocity)
      (reset! position new-position)
      (reset! forces [])
      this)))

(defn particle
  "Create a particle for physics simulation.

   ```clojure
   (def ball (particle {:position [0 5 0]
                        :velocity [1 0 0]
                        :mass 1.0}))
   (apply-force! ball [0 -9.8 0])  ; gravity
   (step! ball 0.016)  ; ~60fps
   ```"
  [{:keys [position velocity mass]
    :or {position [0 0 0] velocity [0 0 0] mass 1.0}}]
  (->Particle (atom (vec position))
              (atom (vec velocity))
              mass
              (atom [])))

(defn spring-force
  "Create a spring force function.

   ```clojure
   (apply-force! particle (spring-force anchor k))
   ```"
  [anchor k & {:keys [rest-length damping] :or {rest-length 0 damping 0}}]
  (fn [obj]
    (let [pos (get-position obj)
          vel (get-velocity obj)
          displacement (mapv - anchor pos)
          distance (Math/sqrt (reduce + (map #(* % %) displacement)))
          direction (if (zero? distance)
                      [0 0 0]
                      (mapv #(/ % distance) displacement))
          spring-magnitude (* k (- distance rest-length))
          damping-force (mapv #(* (- damping) %) vel)]
      (mapv + (mapv #(* spring-magnitude %) direction) damping-force))))

(defn gravity-force
  "Create a gravity force."
  [& {:keys [g direction] :or {g 9.8 direction [0 -1 0]}}]
  (fn [obj]
    (let [mass (:mass obj)]
      (mapv #(* g mass %) direction))))

;; =============================================================================
;; Camera DSL - Declarative camera control
;; =============================================================================

(defn camera
  "Create a camera specification.

   ```clojure
   (camera {:position [0 0 10]
            :look-at [0 0 0]
            :fov 60})
   ```"
  [opts]
  {:type :camera
   :opts opts})

(defn camera-move
  "Create a camera movement animation.

   ```clojure
   (camera-move {:to [5 3 8]
                 :duration 2
                 :ease :smooth})
   ```"
  [opts]
  {:type :camera-move
   :opts opts})

(defn orbit
  "Create an orbital camera movement.

   ```clojure
   (orbit {:center [0 0 0]
           :radius 10
           :angle (* 2 Math/PI)
           :duration 5})
   ```"
  [opts]
  {:type :orbit
   :opts opts})

;; =============================================================================
;; Specs for validation
;; =============================================================================

(s/def ::color (s/or :keyword keyword? :rgb (s/coll-of number? :count 3)))
(s/def ::position (s/coll-of number? :min-count 2 :max-count 3))
(s/def ::duration pos?)

(s/def ::shape-opts
  (s/keys :opt-un [::color ::position ::fill-opacity ::stroke-width]))

(s/def ::animation-type #{:create :fade-in :fade-out :transform :rotate :scale :move})
