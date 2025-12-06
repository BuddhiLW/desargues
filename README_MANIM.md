# Manim + Clojure Integration

This project integrates [Manim (Community Edition)](https://www.manim.community/) with Clojure using [libpython-clj](https://github.com/clj-python/libpython-clj), allowing you to create mathematical animations using Clojure!

## ✅ Setup Complete

Your environment is ready to go! All tests passed successfully.

## 🚀 Quick Start

### Option 1: One-Command Quickstart

Start a REPL and run:

```clojure
lein repl
```

Then:

```clojure
(require '[varcalc.manim-quickstart :as mq])
(mq/quickstart!)
```

This will render the classic Manim quickstart example (a pink circle being created).

### Option 2: Python CLI (Traditional Way)

```bash
conda activate manim
manim -pql manim_examples.py CreateCircle
```

## 📁 What's Been Set Up

### Clojure Namespaces

1. **`varcalc.manim-test`** - Test suite to verify integration
   ```clojure
   (require '[varcalc.manim-test :as mt])
   (mt/run-all-tests)  ; Run all tests
   ```

2. **`varcalc.manim-quickstart`** - Main quickstart example (⭐ START HERE)
   ```clojure
   (require '[varcalc.manim-quickstart :as mq])
   (mq/quickstart!)  ; Create and render the quickstart scene
   ```

3. **`varcalc.manim`** - Basic manim integration helpers

4. **`varcalc.manim-renderer`** - Advanced rendering utilities

### Python Files

- **`manim_examples.py`** - Traditional Python examples you can render with the CLI

### Documentation

- **`MANIM_SETUP.md`** - Detailed setup and troubleshooting guide
- **`README_MANIM.md`** - This file

## 📖 Examples

### Example 1: Quickstart (CreateCircle)

From the [official Manim tutorial](https://docs.manim.community/en/stable/tutorials/quickstart.html):

```clojure
(require '[varcalc.manim-quickstart :as mq])

;; Initialize (once per REPL session)
(mq/init!)

;; Create and render
(let [CreateCircle (mq/make-create-circle-scene)
      scene (CreateCircle)]
  (mq/render-scene! scene))
```

### Example 2: Square to Circle Transformation

```clojure
(require '[varcalc.manim-quickstart :as mq])

(mq/init!)

(let [SquareToCircle (mq/make-square-to-circle-scene)
      scene (SquareToCircle)]
  (mq/render-scene! scene))
```

### Example 3: Custom Scene

```clojure
(require '[varcalc.manim-quickstart :as mq])
(require '[libpython-clj2.python :as py])

(mq/init!)

(let [manim (mq/get-manim-module)
      Scene (py/get-attr manim "Scene")
      Circle (py/get-attr manim "Circle")
      Square (py/get-attr manim "Square")
      Create (py/get-attr manim "Create")
      RED (py/get-attr manim "RED")
      BLUE (py/get-attr manim "BLUE")

      ;; Define custom scene
      MyScene
      (py/python-type
       "MyScene"
       [Scene]
       {"construct"
        (fn [self]
          (let [circle (Circle)
                square (Square)]
            ;; Style objects
            (py/call-attr circle "set_fill" RED :opacity 0.7)
            (py/call-attr square "set_fill" BLUE :opacity 0.7)

            ;; Position square next to circle
            (py/call-attr square "next_to" circle (py/get-attr manim "RIGHT") :buff 0.5)

            ;; Animate both
            (py/call-attr self "play" (Create circle) (Create square))))})]

  ;; Render it
  (mq/render-scene! (MyScene)))
```

## 🎬 Output

Videos are saved to: `media/videos/`

The exact path depends on the quality settings and scene name.

## 🔧 Configuration

### Environment Details

- **Conda environment**: `manim`
- **Python version**: 3.12.12
- **Manim version**: 0.19.0
- **Python executable**: `/home/lages/anaconda3/envs/manim/bin/python`
- **libpython path**: `/home/lages/anaconda3/envs/manim/lib/libpython3.12.so`

### Changing Paths

If your conda is installed elsewhere, update the paths in:
- `src/varcalc/manim_quickstart.clj` (line 13-14)
- `src/varcalc/manim_test.clj` (line 8-9)

## 🧪 Testing

Verify everything works:

```clojure
lein repl
```

```clojure
(require '[varcalc.manim-test :as mt])
(mt/run-all-tests)
```

Expected output:
```
=== Testing Manim Integration ===

✓ Python initialized successfully
✓ Manim imported successfully
✓ Created Circle object successfully

=== Test Results ===
Python Init: ✓ PASS
Manim Import: ✓ PASS
Object Creation: ✓ PASS

All tests passed! 🎉
```

## 🎨 Available Manim Objects

Some commonly used objects and functions:

**Shapes**: Circle, Square, Triangle, Rectangle, Polygon, Arc, etc.

**Animations**: Create, Transform, FadeIn, FadeOut, Write, DrawBorderThenFill, etc.

**Colors**: RED, BLUE, GREEN, PINK, YELLOW, ORANGE, PURPLE, etc.

**Math**: Tex, MathTex, Axes, NumberPlane, Vector, etc.

Access them like this:
```clojure
(let [manim (mq/get-manim-module)
      Circle (py/get-attr manim "Circle")
      RED (py/get-attr manim "RED")]
  ;; Use them...
  )
```

## 📚 Resources

- [Manim Documentation](https://docs.manim.community/)
- [Manim Examples Gallery](https://docs.manim.community/en/stable/examples.html)
- [libpython-clj Guide](https://clj-python.github.io/libpython-clj/)
- [Manim Tutorial](https://docs.manim.community/en/stable/tutorials/quickstart.html)

## 🤝 Integration with Emmy

This project also includes [Emmy](https://github.com/mentat-collective/emmy) (a Clojure library for scientific computing). You can combine Emmy's symbolic math capabilities with Manim's visualization!

Example coming soon...

## 🎯 Next Steps

1. Run the quickstart example: `(mq/quickstart!)`
2. Explore the examples in `manim_examples.py`
3. Browse the [Manim example gallery](https://docs.manim.community/en/stable/examples.html)
4. Create your own mathematical animations!
5. Combine with Emmy for symbolic mathematics + visualization

## 🐛 Troubleshooting

See `MANIM_SETUP.md` for detailed troubleshooting steps.

Common issues:
- **Library not found**: Check Python path in init functions
- **Import errors**: Ensure conda environment is properly set up
- **Render failures**: Check `media/` directory permissions

---

**Happy animating!** 🎬✨
