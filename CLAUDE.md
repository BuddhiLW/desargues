# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Varcalc** is a Clojure library that bridges Emmy (symbolic mathematics) with Manim Community Edition (mathematical animation) via libpython-clj. The system computes derivatives symbolically, converts them to LaTeX, and renders beautiful mathematical animations.

**Technology Stack:**
- Clojure + Leiningen build system
- Emmy for symbolic mathematics
- Manim Community (Python) for mathematical animations
- libpython-clj for Clojure-Python interop
- Conda environment for Python dependencies

## Essential Commands

### Build & Run
```bash
# Run main animation demo (sin(x) derivative)
lein run

# Start REPL for interactive development
lein repl

# Run tests
lein test

# Install dependencies
lein deps
```

### Python Environment
```bash
# Activate conda environment (required before running)
conda activate manim

# Verify Manim installation
manim --version
```

### Generated Output
Videos are saved to `media/videos/1080p60/` after rendering. Quality can be changed by editing `:quality` parameter in `src/varcalc/manim_quickstart.clj` (`"low_quality"`, `"medium_quality"`, or `"high_quality"`).

## Architecture: Layered DDD/SOLID Design

This codebase follows **Domain-Driven Design** with **SOLID principles**. Understanding the layer boundaries is critical:

### Layer Structure (Dependency Direction: Top → Down)

```
API Layer (Facade)
    ↓ depends on
Application/Domain Layer (Core Logic)
    ↓ depends on
Infrastructure Layer (External Systems)
```

### Layer Responsibilities

**1. API Layer** (`src/varcalc/api.clj`)
- Facade pattern providing clean, simple interface
- Factory methods: `expr`, `func`, `point`
- Fluent builders: `scene`, `show`, `transform`, `wait`, `render!`
- High-level workflows: `animate-expression`, `animate-derivative`
- **Never contains business logic**

**2. Domain Layer** (`src/varcalc/domain/`)
- `protocols.clj`: Protocol definitions (interfaces)
  - `IMathematicalObject`, `IEvaluable`, `IDifferentiable`
  - `IRenderable`, `IAnimatable`, `IScene`
- `math_expression.clj`: Domain entities and value objects
  - Entities: `MathExpression`, `MathFunction` (have identity)
  - Value Objects: `LaTeX`, `PythonCode`, `Point` (immutable, value-based equality)
- `services.clj`: Domain services
  - `differentiate-expression`, `evaluate-expression`, `simplify-expression`
  - `tabulate-function`, `compare-at-points`
- **Pure Clojure + Emmy, no external dependencies**
- **No I/O, no rendering concerns**

**3. Infrastructure Layer** (`src/varcalc/infrastructure/`)
- `manim_adapter.clj`: Manim integration
  - Implements domain protocols for Python interop
  - `ManimMobject`, `ManimAnimation`, `ManimSceneBuilder`
  - Adapter pattern wrapping Manim Python objects
- **All Python/Manim code isolated here**
- **Depends on domain protocols, not vice versa**

### Key Design Patterns Used

- **Facade**: `api.clj` hides complexity of lower layers
- **Builder**: `ManimSceneBuilder` for fluent scene construction
- **Adapter**: `ManimMobject` adapts Python objects to Clojure protocols
- **Strategy**: Protocol-based polymorphism for different object types
- **Factory**: Factory functions for creating domain objects
- **Repository**: Protocols defined (not yet implemented) for persistence

## Critical Implementation Details

### Python Initialization

**ALWAYS initialize Python before any Manim operations:**
```clojure
(require '[varcalc.manim-quickstart :as mq])
(mq/init!)
```

This configures:
- Python executable path: `/home/lages/anaconda3/envs/manim/bin/python`
- Library path: `/home/lages/anaconda3/envs/manim/lib/libpython3.12.so`
- Adds conda site-packages to sys.path

**If paths differ on another system**, update `src/varcalc/manim_quickstart.clj:init!` function.

### Emmy → LaTeX → Python Pipeline

The conversion pipeline is critical to understand:

1. **Emmy expression** → `(e/D f)` computes derivative
2. **Emmy → LaTeX** → `(e/->TeX expr)` generates LaTeX string
3. **LaTeX → Python** → `latex2py` converts to Python code
4. **Python → Manim** → Manim renders the LaTeX

**Key functions:**
- `src/varcalc/emmy_manim.clj`: `emmy->latex`, `latex->python`, `emmy->python`
- Pipeline in `src/varcalc/emmy_manim_examples.clj:create-derivative-animation`

### Python Class Instantiation

When creating Python scene classes with keyword arguments:

```clojure
;; CORRECT: Direct function call with keyword args
(FunctionAndDerivative :func_latex "..." :deriv_latex "...")

;; INCORRECT: Do not use py/call-attr-kw
(py/call-attr-kw FunctionAndDerivative [] {:func_latex "..." ...})
```

### Protocol Method Naming

**WARNING**: `wait` is a reserved method name (conflicts with `Object.wait()`). Use `wait-duration` in protocols:

```clojure
(defprotocol IScene
  (wait-duration [this duration])  ;; NOT 'wait'
  ...)
```

### Emmy Function vs Expression

Emmy works with **functions** (callable) not raw expressions:

```clojure
;; CORRECT: Define as function
(defn f [x] (e/sin x))
(def df (e/D f))

;; Then evaluate symbolically
(f 'x)   ;; Returns symbolic expression
(df 'x)  ;; Returns derivative expression

;; INCORRECT: Using raw symbols
(def expr '(sin x))  ;; This is just a list, not an Emmy expression
```

### Forward References

When defining records that reference each other, use `declare`:

```clojure
;; Forward declaration
(declare render-scene-impl)

(defrecord ManimSceneBuilder [...]
  p/IScene
  (render-scene [this]
    (render-scene-impl this)))  ;; Can use before definition

;; Define later
(defn render-scene-impl [builder] ...)
```

## File Organization

### Entry Points
- `src/varcalc/core.clj`: Main `-main` function (invoked by `lein run`)
- `src/varcalc/api.clj`: Primary API for library users

### Integration Layers
- `src/varcalc/manim_quickstart.clj`: Basic Manim setup and initialization
- `src/varcalc/emmy_manim.clj`: Emmy ↔ Manim conversion utilities
- `src/varcalc/emmy_manim_examples.clj`: Complete example workflows

### Python Scene Definitions
- `manim_examples.py`: Basic Manim scenes (CreateCircle, SquareToCircle)
- `emmy_manim_scenes.py`: Emmy-driven scenes (FunctionAndDerivative, ChainRule, TaylorSeries)
- `equation_evaluation_scenes.py`: Evaluation and table scenes

Python files must be in project root (`/home/lages/Physics/varcalc`) so Clojure can import them via `py/import-module`.

### Tests
- `test/varcalc/manim_test.clj`: 14 integration tests
- Run with `lein test` (all should pass)

## Common Development Workflows

### Creating a New Mathematical Animation

**Option 1: Using High-Level API**
```clojure
(require '[varcalc.api :as v])
(v/init!)

;; Define function
(defn my-func [x] (e/exp (e/sin x)))

;; Create and animate derivative
(def e (v/expr (my-func 'x)))
(v/animate-derivative e)
```

**Option 2: Using Builder Pattern**
```clojure
(require '[varcalc.api :as v])
(v/init!)

(def e1 (v/expr '(sin x)))
(def e2 (v/derivative e1))

(-> (v/scene)
    (v/show e1)
    (v/wait 1)
    (v/transform e1 e2)
    (v/wait 2)
    (v/render!))
```

**Option 3: Using Python Scene Classes**
```clojure
(require '[varcalc.emmy-manim-examples :as ex])
(ex/render-emmy-scene "ChainRule")
```

### Adding a New Python Scene

1. Create scene class in `emmy_manim_scenes.py`:
```python
class MyScene(Scene):
    def __init__(self, **kwargs):
        self.param = kwargs.get('param', 'default')
        super().__init__()

    def construct(self):
        # Animation code
        pass
```

2. Import and use from Clojure:
```clojure
(let [sys (py/import-module "sys")]
  (py/call-attr (py/get-attr sys "path") "insert" 0 "/home/lages/Physics/varcalc"))

(let [scenes (py/import-module "emmy_manim_scenes")
      MyScene (py/get-attr scenes "MyScene")
      scene (MyScene :param "value")]
  (mq/render-scene! scene))
```

### Extending Domain with New Protocols

To add new mathematical capabilities:

1. Define protocol in `src/varcalc/domain/protocols.clj`:
```clojure
(defprotocol IIntegrable
  (integrate [this] "Compute indefinite integral"))
```

2. Implement for domain entities in `src/varcalc/domain/math_expression.clj`:
```clojure
(extend-type MathExpression
  p/IIntegrable
  (integrate [this]
    (let [result (e/integrate (:expr this))]
      (create-expression result))))
```

3. Add domain service in `src/varcalc/domain/services.clj`:
```clojure
(defn integrate-expression [math-expr]
  (p/integrate math-expr))
```

4. Expose in API (`src/varcalc/api.clj`):
```clojure
(defn integral [math-obj]
  (svc/integrate-expression math-obj))
```

## Debugging Tips

### Python Import Errors
If you see `ModuleNotFoundError: No module named 'manim'`:
- Check conda environment is activated: `conda activate manim`
- Verify paths in `manim_quickstart.clj:init!` match your system
- Ensure sys.path includes site-packages directory

### LaTeX Rendering Errors
If LaTeX fails to render:
- Install texlive packages: `sudo apt-get install texlive texlive-latex-extra texlive-fonts-extra`
- Check Manim works standalone: `manim --version`

### Emmy Evaluation Issues
Remember Emmy distinguishes between:
- Symbolic evaluation: `(f 'x)` → returns expression
- Numeric evaluation: `(f 3.14)` → returns number
- Use `e/simplify` to simplify symbolic results

### Video Not Generated
- Check console output for errors
- Try `low_quality` rendering first (faster)
- Verify `media/videos/` directory exists

## Testing Strategy

Tests are integration tests that verify:
- Python initialization
- Manim module imports
- Scene class creation
- Rendering pipeline

Run tests before committing:
```bash
lein test
```

All 14 tests should pass. If not, check Python environment configuration.

## Important Configuration Files

- `project.clj`: Leiningen dependencies and build config
- `.gitignore`: Excludes `media/`, `target/`, Python `__pycache__`
- Python scene files must be in project root for `py/import-module` to work

## Video Quality Settings

Edit `src/varcalc/manim_quickstart.clj:render-scene!`:
```clojure
(py/call-attr scene "render"
              :quality "low_quality"    ; 480p15 - fast
              :quality "medium_quality" ; 720p30 - balanced
              :quality "high_quality"   ; 1080p60 - best (default)
              :preview false)
```
