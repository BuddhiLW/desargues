# Desargues (Varcalc) - Project Summary

## Overview

**Desargues** is a Clojure library that bridges **Emmy** (symbolic mathematics) with **Manim** (mathematical animation) via **libpython-clj**. The system computes derivatives symbolically, converts them to LaTeX, and renders beautiful mathematical animations.

The project provides two API layers:
1. **Original DDD/SOLID API** (`varcalc.api`) - Domain-driven design with protocols
2. **New DSL Layer** (`varcalc.dsl.*`) - High-level declarative DSL inspired by 3Blue1Brown videos

## Technology Stack

| Dependency | Version | Purpose |
|------------|---------|---------|
| Clojure | 1.12.3 | Core language |
| Emmy | 0.32.0 | Symbolic mathematics, automatic differentiation |
| libpython-clj | 2.026 | Clojure-Python interop for Manim |
| Manim Community | (Python) | Mathematical animation rendering |
| Conda | manim env | Python environment management |

## Project Structure

```
src/varcalc/
├── core.clj                    # Main entry point (-main)
├── api.clj                     # High-level facade API
│
├── domain/                     # Domain Layer (DDD)
│   ├── protocols.clj           # Protocol definitions (interfaces)
│   ├── math_expression.clj     # Domain entities (MathExpression, MathFunction)
│   └── services.clj            # Domain services
│
├── infrastructure/             # Infrastructure Layer
│   └── manim_adapter.clj       # Manim integration adapters
│
├── dsl/                        # High-level Animation DSL
│   ├── core.clj                # Core abstractions (Reactive, Temporal, Physics)
│   ├── math.clj                # Math expression DSL with Emmy
│   ├── renderer.clj            # DSL-to-Manim rendering bridge
│   └── examples.clj            # Example scene rewrites
│
├── videos/                     # Video Rendering System
│   └── render.clj              # Brachistochrone video renderer (pure Clojure)
│
├── manim/                      # Low-level Manim Bindings
│   ├── core.clj                # Python initialization, module access
│   ├── mobjects.clj            # All visual objects (~100 constructors)
│   ├── animations.clj          # All animation types (~70 types)
│   ├── scenes.clj              # Scene types, cameras, rendering
│   ├── constants.clj           # Colors, directions, math constants
│   ├── all.clj                 # Convenience re-exports
│   └── examples.clj            # Working demonstrations
│
├── manim_quickstart.clj        # Basic Manim setup
├── emmy_manim.clj              # Emmy ↔ Manim conversion utilities
├── emmy_manim_examples.clj     # Complete example workflows
├── brachistochrone.clj         # Physics example (brachistochrone curve)
└── brachistochrone_pure.clj    # Pure Clojure physics simulation

videos/                         # 3b1b reference videos (cloned repo)
```

## Key APIs and Usage

### 1. Original API (`varcalc.api`)

```clojure
(require '[varcalc.api :as v])

;; Initialize Python/Manim
(v/init!)

;; Create mathematical expressions
(def e (v/expr '(sin x)))
(def f (v/func [x] (e/sin x)))

;; Compute derivatives
(def df (v/derivative e))

;; Animate
(v/animate-derivative e)

;; Builder pattern for scenes
(-> (v/scene)
    (v/show e)
    (v/wait 1)
    (v/transform e df)
    (v/render!))
```

### 2. New DSL API (`varcalc.dsl.*`)

#### Reactive Values (replaces ValueTracker)
```clojure
(require '[varcalc.dsl.core :as dsl])

;; Create reactive values
(def x (dsl/reactive 0.5))
(def y (dsl/reactive 0.3))

;; Derived values auto-update (no manual updaters!)
(def sum (dsl/derive-reactive [x y] +))

(dsl/get-value sum)  ; => 0.8
(dsl/set-value! x 1.0)
(dsl/get-value sum)  ; => 1.3 (automatically updated)
```

#### Animation Combinators
```clojure
;; Sequential: >>
(dsl/>> (dsl/animation :create circle)
        (dsl/animation :fade-out circle))

;; Parallel: ||
(dsl/|| (dsl/animation :rotate circle :angle Math/PI)
        (dsl/animation :scale circle :factor 2))

;; Staggered: |>
(dsl/|> 0.1  ; 100ms lag between starts
        (dsl/animation :fade-in obj1)
        (dsl/animation :fade-in obj2)
        (dsl/animation :fade-in obj3))
```

#### Timeline DSL
```clojure
(-> (dsl/timeline)
    (dsl/at 0 (dsl/animation :create circle))
    (dsl/wait 1)
    (dsl/then (dsl/animation :transform circle square))
    (dsl/at 3 (dsl/animation :fade-out square)))
```

#### Physics Simulation
```clojure
;; Create particle
(def ball (dsl/particle {:position [0 5 0]
                         :velocity [1 0 0]
                         :mass 1.0}))

;; Apply forces
(dsl/apply-force! ball (dsl/spring-force [0 0 0] 10.0 :damping 0.5))
(dsl/apply-force! ball (dsl/gravity-force :g 9.8))

;; Step simulation
(dsl/step! ball 0.016)  ; ~60fps
```

#### Mathematical Expressions
```clojure
(require '[varcalc.dsl.math :as math])

;; Create expressions with auto-coloring
(def e (math/expr '(sin x) :colors {'x :blue}))

;; Symbolic operations
(def de (math/derivative e 'x))  ; => cos(x)
(math/simplify (math/expr '(+ x x)))  ; => (* 2 x)

;; Auto-generate colors for variables
(math/auto-color (math/expr '(+ x y z)))
; => Expression with {:x :blue :y :red :z :green}
```

### 3. Video Rendering (`varcalc.videos.render`)

```bash
# Full brachistochrone derivation (12 steps, high quality)
lein run -m varcalc.videos.render

# Just the intro scene
lein run -m varcalc.videos.render intro

# Specific derivation step (1-12)
lein run -m varcalc.videos.render step 7

# Low quality for fast iteration (~4x faster)
lein run -m varcalc.videos.render intro --low
lein run -m varcalc.videos.render --low

# All steps as separate files (parallelizable)
lein run -m varcalc.videos.render all-steps --low
```

**Quality/Performance:**
| Quality | Resolution | Intro Time | Full Video |
|---------|------------|------------|------------|
| High    | 1080p60    | ~90s       | ~10+ min   |
| Low     | 480p15     | ~20s       | ~2-3 min   |

**Parallel Rendering:**
```bash
# Run in separate terminals for parallel rendering
lein run -m varcalc.videos.render step 1 --low &
lein run -m varcalc.videos.render step 2 --low &
lein run -m varcalc.videos.render step 3 --low &
```

### 4. Low-level Manim Bindings (`varcalc.manim.*`)

```clojure
(require '[varcalc.manim.core :as manim])
(require '[varcalc.manim.mobjects :as mob])
(require '[varcalc.manim.animations :as anim])

;; Initialize
(manim/init!)

;; Create mobjects
(def c (mob/circle :radius 2 :color @mob/BLUE))
(def t (mob/tex "x^2 + y^2 = r^2"))

;; Create animations
(def fade (anim/fade-in c))
(def write (anim/write t))

;; Render scene
(manim/defscene MyScene []
  (manim/play! self fade)
  (manim/wait! self 1)
  (manim/play! self write))
```

## Architecture

### Layer Dependencies (Top → Down)

```
┌─────────────────────────────────────┐
│          API Layer                  │
│  varcalc.api, varcalc.dsl.*         │
│  (Facade pattern, DSL)              │
└─────────────────┬───────────────────┘
                  │ depends on
┌─────────────────▼───────────────────┐
│      Domain/Application Layer       │
│  varcalc.domain.*, dsl.core/math    │
│  (Protocols, Entities, Services)    │
└─────────────────┬───────────────────┘
                  │ depends on
┌─────────────────▼───────────────────┐
│       Infrastructure Layer          │
│  varcalc.manim.*, dsl.renderer      │
│  (Python interop, Manim adapters)   │
└─────────────────────────────────────┘
```

### Key Protocols

| Protocol | Purpose |
|----------|---------|
| `IMathematicalObject` | Base for all math objects |
| `IEvaluable` | Objects that can be evaluated |
| `IDifferentiable` | Objects supporting differentiation |
| `IRenderable` | Objects that can render to Manim |
| `IAnimatable` | Objects that can be animated |
| `Reactive` (DSL) | Reactive state containers |
| `Temporal` (DSL) | Time-varying entities |
| `Physical` (DSL) | Physics simulation objects |

## Critical Implementation Details

### Python Class Methods (IMPORTANT!)

When creating Python Scene subclasses from Clojure, you **MUST** wrap construct functions with `py-class/make-tuple-instance-fn` to properly receive the `self` argument:

```clojure
(require '[libpython-clj2.python :as py])
(require '[libpython-clj2.python.class :as py-class])

;; CORRECT: Wrap with make-tuple-instance-fn
(defn my-construct [self]
  (let [circle (mob/circle)]
    (py/call-attr self "play" (anim/create circle))))

(let [Scene (py/get-attr (manim/manim) "Scene")
      wrapped-construct (py-class/make-tuple-instance-fn my-construct)]
  (py/create-class "MyScene" [Scene] {"construct" wrapped-construct}))

;; WRONG: Without wrapping, self is not passed!
;; (py/create-class "MyScene" [Scene] {"construct" my-construct})
;; => Error: Wrong number of args (0) passed to construct
```

This fix is already applied in:
- `varcalc.manim-quickstart/create-scene-class`
- `varcalc.manim.core/create-scene-class`
- `varcalc.videos.render` (all render functions)

## Implementation Patterns

### 1. Reactive Programming (DSL)
```clojure
;; Instead of manual updaters (Python style):
;; tracker.add_updater(lambda m: m.set_value(get_max()))

;; Use derived reactive values:
(def max-val (dsl/derive-reactive [x1 x2] max))
;; Automatically updates when x1 or x2 change
```

### 2. Data as Code
```clojure
;; Animations are data structures, not imperative commands
{:type :sequence
 :children [{:type :fade-in :target :circle :duration 1}
            {:type :wait :duration 0.5}
            {:type :transform :from :circle :to :square}]}
```

### 3. Protocol-based Extensibility
```clojure
;; Add new shape types without modifying existing code
(defmethod render-shape :my-custom-shape
  [{:keys [opts]}]
  ;; implementation
  )
```

### 4. Lazy Constants
```clojure
;; Colors are lazy to avoid Python init at load time
(def RED (delay (manim/get-constant "RED")))
;; Use with @RED
```

## Python Environment Setup

```bash
# Create conda environment
conda create -n manim python=3.12
conda activate manim

# Install Manim
pip install manim

# Verify
manim --version
```

**Important**: Update paths in `src/varcalc/manim_quickstart.clj:init!` if your conda paths differ:
```clojure
:python-executable "/home/lages/anaconda3/envs/manim/bin/python"
:library-path "/home/lages/anaconda3/envs/manim/lib/libpython3.12.so"
:site-packages "/home/lages/anaconda3/envs/manim/lib/python3.12/site-packages"
```

## Development Workflow

### Running the Main Demo
```bash
lein run
```

### REPL Development
```bash
lein repl

;; In REPL:
(require '[varcalc.api :as v])
(v/init!)
(v/animate-derivative (v/expr '(sin x)))
```

### Running Tests
```bash
lein test
```

### Checking Compilation
```bash
lein check
```

## Extension Points

### Adding New Mobject Types
1. Add constructor in `src/varcalc/manim/mobjects.clj`
2. Add render method in `src/varcalc/dsl/renderer.clj`

### Adding New Animation Types
1. Add constructor in `src/varcalc/manim/animations.clj`
2. Add render method in `src/varcalc/dsl/renderer.clj`

### Adding New Physics Forces
```clojure
(defn my-force [params]
  (fn [particle]
    (let [pos (dsl/get-position particle)]
      ;; Return [fx fy fz] force vector
      [0 0 0])))
```

### Adding New Math Operations
1. Add to `src/varcalc/dsl/math.clj`
2. Implement using Emmy's symbolic capabilities

## Reference Material

The `videos/` directory contains the cloned 3b1b videos repository with Python examples that inspired the DSL design:
- `videos/_2024/puzzles/max_rand.py` - ValueTracker patterns
- `videos/_2023/optics_puzzles/driven_harmonic_oscillator.py` - Physics simulations
- `videos/_2025/laplace/main_equations.py` - Equation transformations

## Output

Rendered videos are saved to `media/videos/` directory:
- **High quality**: `media/videos/1080p60/` - 1080p at 60fps
- **Medium quality**: `media/videos/720p30/` - 720p at 30fps  
- **Low quality**: `media/videos/480p15/` - 480p at 15fps (fast dev iteration)

Use `--low` flag for fast iteration during development.

## Known Issues

- Emmy reflection warnings during compilation (harmless)
- SLF4J logger binding warning (harmless)
- Python must be initialized before any Manim operations
