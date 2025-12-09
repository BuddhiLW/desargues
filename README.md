# Varcalc: Mathematical Animation Generator

**Emmy + Manim = Beautiful Mathematical Animations**

Varcalc is a Clojure library that combines [Emmy](https://github.com/mentat-collective/emmy) (symbolic mathematics) with [Manim Community Edition](https://www.manim.community/) (mathematical animations) to create beautiful visualizations of mathematical expressions, derivatives, and transformations.

## Features

- 🧮 **Symbolic Mathematics**: Use Emmy to define functions, compute derivatives, simplify expressions
- 🎬 **Mathematical Animations**: Render beautiful animations with Manim
- 🔄 **Automatic Conversion**: Emmy → LaTeX → Python → Manim pipeline
- 🏗️ **Clean Architecture**: SOLID principles and Domain-Driven Design
- 🎨 **Fluent API**: Easy-to-use builder pattern for creating animations
- ✨ **Pre-built Examples**: Chain rule, product rule, Taylor series, and more

## Prerequisites

- **Java**: JDK 8 or higher
- **Leiningen**: Clojure build tool
- **Anaconda/Miniconda**: For Python environment management
- **LaTeX**: For rendering mathematical expressions (usually comes with Manim)

## Installation

### 1. Clone the Repository

```bash
cd /home/lages/Physics/
git clone <your-repo-url> varcalc  # or use your existing directory
cd varcalc
```

### 2. Install Manim with Conda

Create a conda environment with Manim Community Edition:

```bash
# Create conda environment
conda create -n manim python=3.12

# Activate the environment
conda activate manim

# Install Manim
conda install -c conda-forge manim

# Install additional dependencies
pip install latex2py
```

### 3. Verify Manim Installation

```bash
# Test Manim
manim --version

# Should output something like:
# Manim Community v0.18.1
```

### 4. Update Python Paths in Code

The project is currently configured for the following paths:
- Python executable: `/home/lages/anaconda3/envs/manim/bin/python`
- Library path: `/home/lages/anaconda3/envs/manim/lib/libpython3.12.so`

If your conda installation is in a different location, update these paths in `src/varcalc/manim_quickstart.clj`:

```clojure
(defn init! []
  (py/initialize!
   :python-executable "/YOUR/PATH/TO/anaconda3/envs/manim/bin/python"
   :library-path "/YOUR/PATH/TO/anaconda3/envs/manim/lib/libpython3.12.so")
  ...)
```

### 5. Install Clojure Dependencies

```bash
lein deps
```

## Quick Start

### Run the Main Demo

Generate a derivative animation (sin(x) → cos(x)):

```bash
lein run
```

This will:
1. Initialize Python and Manim
2. Compute the derivative of sin(x) using Emmy
3. Generate LaTeX representations
4. Render a Manim animation
5. Save the video to `media/videos/1080p60/FunctionAndDerivative.mp4`

**Output:**
```
=== Varcalc: Mathematical Animation Generator ===

Initializing Python and Manim...
Python initialized!

Creating mathematical function: sin(x)
Computing derivative using Emmy...

LaTeX representations:
  f(x)  = \sin\left(x\right)
  f'(x) = \cos\left(x\right)

Rendering animation...
✓ Animation complete!
Video saved to: media/videos/1080p60/
```

### View the Generated Video

```bash
# On Linux with default video player
xdg-open media/videos/1080p60/FunctionAndDerivative.mp4

# Or manually browse to:
cd media/videos/1080p60/
ls -lh *.mp4
```

Videos are organized by resolution:
- `media/videos/1080p60/` - Full HD (default)
- `media/videos/480p15/` - Low quality (faster rendering)

## Running Tests

Run the test suite to verify everything works:

```bash
lein test
```

Expected output:
```
Testing varcalc.manim-test

Ran 14 tests containing 14 assertions.
0 failures, 0 errors.
```

## Usage Examples

### Interactive REPL

Start a REPL session to explore interactively:

```bash
lein repl
```

#### Basic Example: Explore a Function

```clojure
(require '[varcalc.emmy-manim-examples :as ex])
(require '[varcalc.manim-quickstart :as mq])
(require '[emmy.env :as e :refer [sin cos square D pi]])

;; Initialize
(mq/init!)

;; Explore a function and its derivative
(ex/explore-function #(square (sin %)))
;; Returns:
;; {:function {:expr (expt (sin x) 2)
;;             :latex "\\sin^{2}\\left(x\\right)"
;;             :python ...}
;;  :derivative {:expr (* 2 (sin x) (cos x))
;;               :latex "2 \\cos\\left(x\\right) \\sin\\left(x\\right)"
;;               :python ...}}
```

#### Pre-made Animation Examples

```clojure
;; Chain rule example: sin²(x + 3)
(ex/example-chain-rule)

;; Taylor series visualization
(ex/example-taylor-series)

;; Product rule demonstration
(ex/example-product-rule)

;; Quadratic function and derivative
(ex/example-quadratic)
```

#### Custom Function Animation

```clojure
(require '[emmy.env :as e])

;; Define your own function
(defn my-func [x]
  (* (e/exp x) (e/sin x)))

;; Create derivative animation
(ex/create-derivative-animation my-func)
```

#### Using the Clean API (SOLID/DDD)

```clojure
(require '[varcalc.api :as v])
(require '[varcalc.manim-quickstart :as mq])

;; Initialize
(mq/init!)

;; Create expressions
(def f (v/expr '(+ (* x x) (* 3 x) 2)))
(def df (v/derivative f))

;; Get LaTeX
(v/latex f)    ;; => "x^{2} + 3 x + 2"
(v/latex df)   ;; => "2 x + 3"

;; Build and render a scene
(-> (v/scene)
    (v/show f)
    (v/wait 1)
    (v/transform f df)
    (v/wait 1)
    (v/render!))
```

## Project Structure

```
varcalc/
├── src/varcalc/
│   ├── core.clj                          # Main entry point (lein run)
│   ├── manim_quickstart.clj              # Basic Manim setup
│   ├── emmy_manim.clj                    # Emmy → LaTeX → Python
│   ├── emmy_manim_examples.clj           # Complete examples & workflows
│   ├── emmy_evaluation.clj               # Equation evaluation
│   ├── api.clj                           # Clean Facade API
│   │
│   ├── domain/                           # Domain Layer (DDD)
│   │   ├── protocols.clj                 # Core protocols/interfaces
│   │   ├── math_expression.clj           # Domain entities
│   │   └── services.clj                  # Domain services
│   │
│   ├── infrastructure/                   # Infrastructure Layer
│   │   └── manim_adapter.clj             # Manim integration
│   │
│   └── emmy_python/                      # Emmy-Python bridge
│       └── equations.clj                 # Sample equations
│
├── test/varcalc/
│   └── manim_test.clj                    # Integration tests
│
├── *.py                                  # Python scene definitions
│   ├── manim_examples.py                 # Basic scenes
│   ├── emmy_manim_scenes.py              # Emmy-driven scenes
│   └── equation_evaluation_scenes.py     # Evaluation scenes
│
├── media/videos/                         # Generated videos (gitignored)
│
├── README.md                             # This file
├── ARCHITECTURE.md                       # SOLID/DDD architecture guide
├── CLEAN_API_GUIDE.md                    # Clean API usage guide
├── EMMY_MANIM_GUIDE.md                   # Emmy integration guide
└── project.clj                           # Leiningen project file
```

## Common Tasks

### Change Video Quality

Edit the quality settings in `src/varcalc/manim_quickstart.clj`:

```clojure
(defn render-scene!
  [scene]
  (py/call-attr scene "render"
                :quality "low_quality"    ; or "medium_quality", "high_quality"
                :preview false))
```

Quality options:
- `low_quality`: 480p15 - Fast rendering for testing
- `medium_quality`: 720p30 - Balanced
- `high_quality`: 1080p60 - Best quality (default)

### Add Custom Scenes

1. Create a Python scene in a `.py` file:

```python
from manim import *

class MyScene(Scene):
    def construct(self):
        text = Text("Hello from Manim!")
        self.play(Write(text))
        self.wait(2)
```

2. Render from Clojure:

```clojure
(require '[varcalc.emmy-manim-examples :as ex])

(ex/render-emmy-scene "MyScene")
```

### Explore Multiple Derivatives

```clojure
(require '[emmy.env :as e])
(require '[varcalc.emmy-manim :as em])

;; Function and its first 3 derivatives
(let [f #(e/sin %)
      df1 (e/D f)
      df2 (e/D df1)
      df3 (e/D df2)]
  (map #(em/emmy->latex (% 'x)) [f df1 df2 df3]))

;; => ["\\sin\\left(x\\right)"
;;     "\\cos\\left(x\\right)"
;;     "-\\sin\\left(x\\right)"
;;     "-\\cos\\left(x\\right)"]
```

## Troubleshooting

### Python Module Not Found

**Error:** `ModuleNotFoundError: No module named 'manim'`

**Solution:** Ensure the conda environment is activated and paths are correct:
```bash
conda activate manim
which python  # Should show /path/to/anaconda3/envs/manim/bin/python
```

Update the paths in `src/varcalc/manim_quickstart.clj` to match your system.

### LaTeX Rendering Issues

**Error:** `LaTeX Error: File 'preview.sty' not found`

**Solution:** Install LaTeX packages:
```bash
# Ubuntu/Debian
sudo apt-get install texlive texlive-latex-extra texlive-fonts-extra

# macOS
brew install --cask mactex
```

### Video Not Generated

**Issue:** No video file in `media/videos/`

**Solution:**
1. Check for errors in the console output
2. Try low quality rendering first: `:quality "low_quality"`
3. Verify Manim works standalone: `manim --version`

### Memory Issues

**Issue:** Java heap space errors

**Solution:** Increase JVM memory:
```bash
export LEIN_JVM_OPTS="-Xmx4g"
lein run
```

## Documentation

- **[ARCHITECTURE.md](ARCHITECTURE.md)**: SOLID/DDD design patterns used in the project
- **[CLEAN_API_GUIDE.md](CLEAN_API_GUIDE.md)**: Guide to the clean API facade
- **[EMMY_MANIM_GUIDE.md](EMMY_MANIM_GUIDE.md)**: Emmy integration and LaTeX conversion
- **[EQUATION_EVALUATION_GUIDE.md](EQUATION_EVALUATION_GUIDE.md)**: Evaluating functions and creating tables

## Resources

- **Emmy Documentation**: https://github.com/mentat-collective/emmy
- **Manim Community**: https://www.manim.community/
- **Manim Quickstart**: https://docs.manim.community/en/stable/tutorials/quickstart.html
- **libpython-clj**: https://github.com/clj-python/libpython-clj

## Examples Gallery

After running the examples, you'll find videos like:

1. **FunctionAndDerivative.mp4**: Side-by-side function and derivative
2. **ChainRule.mp4**: Demonstrates chain rule with sin²(x + 3)
3. **TaylorSeries.mp4**: Taylor series expansion visualization
4. **ProductRule.mp4**: Product rule for derivatives
5. **QuadraticFormula.mp4**: Quadratic formula derivation

## Contributing

Contributions welcome! Areas for improvement:

- [ ] More pre-built animation scenes
- [ ] Support for partial derivatives (multivariate calculus)
- [ ] 3D plotting with Manim's ThreeDScene
- [ ] Integration (not just differentiation)
- [ ] Physics simulations
- [ ] Better symbolic π rendering (use `'e/pi` for LaTeX)

## License

Copyright © 2025

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.

## Acknowledgments

- **Emmy**: Powerful symbolic mathematics in Clojure
- **Manim Community**: Beautiful mathematical animations
- **libpython-clj**: Seamless Clojure-Python interop
