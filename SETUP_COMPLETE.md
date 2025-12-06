# Manim + Clojure Setup Complete! ✅

## What Has Been Set Up

Your project now has a complete integration between **Manim (Mathematical Animation Engine)** and **Clojure** using **libpython-clj**.

### Test Results

All **14 integration tests** passed successfully:

```bash
lein test varcalc.manim-test

Ran 14 tests containing 40 assertions.
0 failures, 0 errors.
```

## Quick Start Guide

### Run the Quickstart Example

```bash
lein repl
```

Then in the REPL:

```clojure
(require '[varcalc.manim-quickstart :as mq])
(mq/quickstart!)
```

This will create the classic Manim "pink circle" animation and save it to `media/videos/`.

### Run Unit Tests

```bash
lein test varcalc.manim-test
```

## Files Created

### 1. Test Files (`test/varcalc/`)

- **`manim_test.clj`** - Comprehensive unit tests (14 tests, 40 assertions)
  - Tests Python initialization
  - Tests Manim import
  - Tests object creation (Circle, Square, etc.)
  - Tests colors and constants
  - Tests animations
  - Run with: `lein test varcalc.manim-test`

### 2. Source Files (`src/varcalc/`)

- **`manim_quickstart.clj`** ⭐ **START HERE**
  - Complete quickstart example from Manim tutorial
  - Functions:
    - `(init!)` - Initialize Python environment
    - `(quickstart!)` - Run complete example in one command
    - `(make-create-circle-scene)` - Create the CreateCircle scene
    - `(make-square-to-circle-scene)` - Create transformation example
    - `(render-scene! scene)` - Render any scene
  - Includes extensive examples in the `comment` block

- **`manim_renderer.clj`**
  - Advanced rendering utilities
  - Quality settings configuration
  - Scene rendering with options

- **`manim.clj`**
  - Basic Manim integration helpers

### 3. Python Files

- **`manim_examples.py`**
  - Traditional Python examples that work with both:
    - Manim CLI: `manim -pql manim_examples.py CreateCircle`
    - Clojure (can be imported as a module)
  - Includes 5 example scenes:
    - `CreateCircle` - Basic quickstart
    - `SquareToCircle` - Transformation
    - `SquareAndCircle` - Multiple objects
    - `AnimatedSquareToCircle` - Advanced animation
    - `DifferentRotations` - Rotation methods

### 4. Documentation

- **`README_MANIM.md`** - Main user guide
- **`MANIM_SETUP.md`** - Detailed setup and troubleshooting
- **`SETUP_COMPLETE.md`** - This file

## Configuration Details

### Environment

- **Conda environment**: `manim` (already created and activated)
- **Python version**: 3.12.12
- **Manim version**: 0.19.0
- **Python executable**: `/home/lages/anaconda3/envs/manim/bin/python`
- **libpython path**: `/home/lages/anaconda3/envs/manim/lib/libpython3.12.so`

### Project Dependencies (in `project.clj`)

```clojure
:dependencies [[org.clojure/clojure "1.12.3"]
               [org.mentat/emmy "0.32.0"]
               [clj-python/libpython-clj "2.026"]]
```

## Example Usage

### Example 1: Quickstart (One Command)

```clojure
lein repl
```

```clojure
(require '[varcalc.manim-quickstart :as mq])
(mq/quickstart!)
```

### Example 2: Step-by-Step

```clojure
(require '[varcalc.manim-quickstart :as mq])
(require '[libpython-clj2.python :as py])

;; Initialize Python (once per REPL session)
(mq/init!)

;; Create the CreateCircle scene
(let [CreateCircle (mq/make-create-circle-scene)
      scene (CreateCircle)]
  (mq/render-scene! scene))
```

### Example 3: Custom Animation

```clojure
(require '[varcalc.manim-quickstart :as mq])
(require '[libpython-clj2.python :as py])

(mq/init!)

(let [manim (mq/get-manim-module)
      Circle (py/get-attr manim "Circle")
      Square (py/get-attr manim "Square")
      Create (py/get-attr manim "Create")
      RED (py/get-attr manim "RED")
      BLUE (py/get-attr manim "BLUE")

      MyScene
      (mq/create-scene-class
       "MyScene"
       (fn [self]
         (let [circle (Circle)
               square (Square)]
           ;; Style
           (py/call-attr-kw circle "set_fill" [RED] {:opacity 0.7})
           (py/call-attr-kw square "set_fill" [BLUE] {:opacity 0.7})

           ;; Position
           (py/call-attr-kw square "next_to"
                            [circle (py/get-attr manim "RIGHT")]
                            {:buff 0.5})

           ;; Animate
           (py/call-attr self "play" (Create circle) (Create square)))))]

  (mq/render-scene! (MyScene)))
```

### Example 4: Using Python CLI

```bash
conda activate manim
manim -pql manim_examples.py CreateCircle
```

Flags:
- `-p` - Preview video after rendering
- `-q` - Quality: `l` (low), `m` (medium), `h` (high)
- Examples: `-pql` (preview + quality low)

## Important Notes

### Keyword Arguments in Python Calls

When calling Python functions with keyword arguments, use `call-attr-kw`:

```clojure
;; CORRECT:
(py/call-attr-kw circle "set_fill" [PINK] {:opacity 0.5})

;; INCORRECT (will fail):
(py/call-attr circle "set_fill" PINK :opacity 0.5)
```

### Python Initialization

Python must be initialized once per REPL session:

```clojure
(require '[varcalc.manim-quickstart :as mq])
(mq/init!)  ;; Do this once
```

## Output Location

Videos are saved to:
```
media/videos/
```

The exact path depends on quality settings and scene name.

## Testing

Run all integration tests:

```bash
lein test varcalc.manim-test
```

Expected output:
```
Ran 14 tests containing 40 assertions.
0 failures, 0 errors.
```

## Next Steps

1. ✅ **Try the quickstart**: `(mq/quickstart!)`
2. Browse the [Manim Example Gallery](https://docs.manim.community/en/stable/examples.html)
3. Combine with Emmy (already in dependencies) for symbolic math visualizations
4. Create your own mathematical animations!

## Resources

- [Manim Documentation](https://docs.manim.community/)
- [Manim Quickstart Tutorial](https://docs.manim.community/en/stable/tutorials/quickstart.html)
- [libpython-clj Guide](https://clj-python.github.io/libpython-clj/)
- [Emmy (Scientific Computing)](https://github.com/mentat-collective/emmy)

## Troubleshooting

See `MANIM_SETUP.md` for detailed troubleshooting.

Common issues:
- **Library not found**: Check paths in `init!` functions
- **Import errors**: Verify conda environment: `conda activate manim && python -c "import manim"`
- **Render failures**: Check `media/` directory permissions

---

**Everything is ready to go! Happy animating!** 🎬✨

Try it now:
```clojure
lein repl
(require '[varcalc.manim-quickstart :as mq])
(mq/quickstart!)
```
