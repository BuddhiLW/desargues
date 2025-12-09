# Manim with libpython-clj Setup

This guide explains how to use Manim (Mathematical Animation Engine) from Clojure using libpython-clj.

## Prerequisites

✅ You've already completed:
- Created conda environment: `manim`
- Installed manim: `conda install -c conda-forge manim`
- Added libpython-clj to project.clj

## Project Structure

```
varcalc/
├── src/varcalc/
│   ├── manim.clj           # Basic manim integration
│   └── manim_renderer.clj  # Complete rendering examples
├── manim_examples.py       # Python examples (can be used from CLI or Clojure)
└── MANIM_SETUP.md         # This file
```

## Quick Start

### Option 1: Using Clojure REPL (Recommended for Interactive Development)

1. Start a REPL:
   ```bash
   lein repl
   ```

2. Load the manim renderer namespace:
   ```clojure
   (require '[varcalc.manim-renderer :as mr])
   ```

3. Initialize Python with your manim environment:
   ```clojure
   (mr/init!)
   ```

4. Setup manim:
   ```clojure
   (mr/setup-manim!)
   ```

5. Create and render the quickstart example:
   ```clojure
   (mr/create-and-render-circle!)
   ```

   The video will be saved in `media/videos/` folder!

### Option 2: Using Python CLI (Traditional Manim Way)

Activate your conda environment and use manim's CLI:

```bash
conda activate manim
manim -pql manim_examples.py CreateCircle
```

Flags:
- `-p`: Preview the video after rendering
- `-q`: Quality (l=low, m=medium, h=high)
- `-ql`: Quick low quality render

## Python Environment Path

The Clojure code expects your conda environment at:
```
/home/lages/anaconda3/envs/manim/
```

If your conda is installed elsewhere, update the paths in:
- `src/varcalc/manim.clj` (line 10-11)
- `src/varcalc/manim_renderer.clj` (line 10-11)

To find your conda path:
```bash
conda info --base
```

To find your manim environment's Python:
```bash
conda activate manim
which python
```

## Available Examples

### In Python file (`manim_examples.py`):
- `CreateCircle` - Basic quickstart example
- `SquareToCircle` - Transformation animation
- `SquareAndCircle` - Multiple objects
- `AnimatedSquareToCircle` - Advanced animation
- `DifferentRotations` - Rotation methods

Render any of them:
```bash
manim -pql manim_examples.py <SceneName>
```

### From Clojure REPL:

After initialization, you can create custom animations:

```clojure
(let [manim (py/import-module "manim")
      Scene (py/get-attr manim "Scene")
      Circle (py/get-attr manim "Circle")
      Square (py/get-attr manim "Square")
      Create (py/get-attr manim "Create")
      Transform (py/get-attr manim "Transform")
      BLUE (py/get-attr manim "BLUE")
      PINK (py/get-attr manim "PINK")

      ;; Create a custom scene
      MyScene
      (py/python-type
       "MyScene"
       [Scene]
       {"construct"
        (fn [self]
          (let [circle (Circle)
                square (Square)]
            (py/call-attr circle "set_fill" PINK :opacity 0.7)
            (py/call-attr square "set_fill" BLUE :opacity 0.7)
            (py/call-attr self "play" (Create square))
            (py/call-attr self "play" (Transform square circle))))})]

  ;; Render it
  (let [scene (MyScene)]
    (py/call-attr scene "render")))
```

## Troubleshooting

### Python library not found
If you get an error about libpython not found, check your Python shared library:

```bash
conda activate manim
find $CONDA_PREFIX -name "libpython*.so"
```

Update the `:library-path` in the `init!` function accordingly.

### Import errors
Make sure manim is properly installed:

```bash
conda activate manim
python -c "import manim; print(manim.__version__)"
```

### Video output location
By default, manim saves videos to:
```
media/videos/<python_file_name>/<quality>/<scene_name>.mp4
```

For Clojure-rendered scenes, check:
```
media/videos/
```

## Next Steps

- Read the [Manim documentation](https://docs.manim.community/)
- Explore more examples in the [Manim example gallery](https://docs.manim.community/en/stable/examples.html)
- Create your own scenes in Clojure!
- Combine with Emmy (already in project.clj) for mathematical visualizations

## Resources

- [Manim Documentation](https://docs.manim.community/)
- [libpython-clj Documentation](https://clj-python.github.io/libpython-clj/)
- [Manim Quickstart](https://docs.manim.community/en/stable/tutorials/quickstart.html)
