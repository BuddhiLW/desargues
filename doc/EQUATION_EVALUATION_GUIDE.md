# Equation Evaluation with Emmy + Manim

Show equations being evaluated at specific values with beautiful animations!

## Overview

This feature lets you:
- ✅ Evaluate Emmy functions at specific values
- ✅ Show step-by-step substitution and calculation
- ✅ Create tables of values
- ✅ Compare multiple functions
- ✅ Visualize derivatives at specific points

## Quick Start

```clojure
(require '[varcalc.emmy-evaluation :as ev])
(require '[emmy.env :refer [sin cos square pi D]])
(require '[varcalc.manim-quickstart :as mq])

(mq/init!)

;; Evaluate sin(x) at x = π/2
(ev/create-single-evaluation #(sin %) (/ pi 2))
```

## Features

### 1. Single Evaluation

Show one function evaluated at one point:

```clojure
;; f(x) = x² at x = 3
(ev/create-single-evaluation #(square %) 3)

;; f(x) = sin(x + π) at x = 0
(ev/create-single-evaluation #(sin (+ % 'pi)) 0)
```

**Output:**
```
f(x) = x²
Evaluate at x = 3
f(3) = 9
= 9
```

### 2. Evaluation Table

Create a table showing a function at multiple points:

```clojure
;; sin(x) at 0, π/4, π/2, π
(ev/create-evaluation-table
 #(sin %)
 [0 (/ pi 4) (/ pi 2) pi])
```

**Output Table:**
```
| x    | f(x)      | Value        |
|------|-----------|--------------|
| 0    | sin(0)    | 0            |
| π/4  | sin(π/4)  | √2/2         |
| π/2  | sin(π/2)  | 1            |
| π    | sin(π)    | 0            |
```

### 3. Multiple Evaluations

Show the same function at several points:

```clojure
;; x² at 1, 2, 3, 4, 5
(ev/create-multiple-evaluations
 #(square %)
 [1 2 3 4 5])
```

**Output:**
```
f(x) = x²

f(1) = 1
f(2) = 4
f(3) = 9
f(4) = 16
f(5) = 25
```

### 4. Derivative at a Point

Show both function and derivative values:

```clojure
;; sin(x) at x = π/4
(ev/create-derivative-evaluation
 #(sin %)
 (/ pi 4))
```

**Output:**
```
f(x) = sin(x)
f'(x) = cos(x)

At x = π/4
f(π/4) = √2/2
f'(π/4) = √2/2  (slope of tangent line)
```

### 5. Compare Multiple Functions

Compare different functions at the same points:

```clojure
;; Compare sin and cos
(ev/create-comparison-table
 [#(sin %) #(cos %)]
 [0 (/ pi 2) pi])
```

**Output Table:**
```
f(x) = sin(x)    g(x) = cos(x)

| x    | f    | g    |
|------|------|------|
| 0    | 0    | 1    |
| π/2  | 1    | 0    |
| π    | 0    | -1   |
```

## Complete Examples

### Example 1: Your Quadratic from equations.clj

```clojure
(require '[varcalc.emmy-python.equations :as eq])

;; expt2: 2x² + 3x + 1
(ev/create-evaluation-table
 eq/expt2
 [0 1 2 3])
```

**Result:**
```
f(x) = 2x² + 3x + 1

| x | f(x)           | Value |
|---|----------------|-------|
| 0 | 2(0)² + 3(0) + 1 | 1     |
| 1 | 2(1)² + 3(1) + 1 | 6     |
| 2 | 2(2)² + 3(2) + 1 | 15    |
| 3 | 2(3)² + 3(3) + 1 | 28    |
```

### Example 2: Gaussian Function

```clojure
;; Gaussian: e^(-x²)
(ev/create-evaluation-table
 #(exp (- (square %)))
 [-2 -1 0 1 2])
```

### Example 3: Trig Functions Comparison

```clojure
;; Compare sin, cos, tan
(ev/create-comparison-table
 [#(sin %) #(cos %) #(tan %)]
 [0 (/ pi 4) (/ pi 2)])
```

### Example 4: Polynomial and its Derivative

```clojure
;; f(x) = x³ - 2x² + x
(defn cubic [x]
  (+ (* x x x) (- (* 2 x x)) x))

;; Show f and f' at x = 2
(ev/create-derivative-evaluation cubic 2)
```

**Result:**
```
f(x) = x³ - 2x² + x
f'(x) = 3x² - 4x + 1

At x = 2
f(2) = 2
f'(2) = 5  (slope of tangent line)
```

### Example 5: Your Original Use Case

From your example code:

```clojure
(require '[varcalc.emmy-python.equations :as eq])

;; expt1: sin(x + π)
;; Evaluate at several points
(ev/create-evaluation-table
 eq/expt1
 [0 (/ pi 2) pi (* 3 (/ pi 2)) (* 2 pi)])
```

## Advanced: Custom Workflows

### Workflow 1: Build Your Own Table

```clojure
(defn my-table [f points]
  (doseq [x points]
    (let [result (f x)]
      (println (str "f(" x ") = " result)))))

;; Use it
(my-table #(square %) [1 2 3 4 5])
```

### Workflow 2: Multiple Functions, One Point

```clojure
(defn evaluate-many [functions x-val]
  (doseq [f functions]
    (println (str "Result: " (f x-val)))))

;; Evaluate sin, cos, tan at π/4
(evaluate-many
 [#(sin %) #(cos %) #(tan %)]
 (/ pi 4))
```

### Workflow 3: Numeric Approximation

```clojure
(require '[emmy.env :refer [simplify]])

;; Get numeric value
(defn numeric-eval [f x]
  (double (simplify (f x))))

;; Example
(numeric-eval #(sin %) (/ pi 2))
;; => 1.0
```

## Available Scenes

All in `equation_evaluation_scenes.py`:

| Scene | Purpose | Parameters |
|-------|---------|-----------|
| `EquationEvaluation` | Single evaluation | equation, var, value, result |
| `EquationEvaluationSteps` | Step-by-step | equation, steps |
| `EvaluationTable` | Table of values | headers, rows |
| `MultipleEvaluations` | List of evaluations | equation, evaluations |
| `ComparisonTable` | Compare functions | functions, x_values, results |
| `DerivativeAtPoint` | f and f' at point | func, deriv, point, values |
| `TabulateFunction` | Full table | func, x_values, f_values |

## Tips

### Symbolic π

Use Emmy's symbolic pi for cleaner output:

```clojure
;; ✅ Good
(ev/create-evaluation-table
 #(sin (+ % 'pi))
 [0 (/ pi 2)])

;; ❌ Not as clean
(ev/create-evaluation-table
 #(sin (+ % 3.14159...))
 [0 1.5708...])
```

### Simplification

Emmy automatically simplifies results:

```clojure
(require '[emmy.env :refer [simplify]])

;; sin²(x) + cos²(x) at any point = 1
(defn pythagorean [x]
  (simplify (+ (square (sin x)) (square (cos x)))))

(pythagorean 'any-value)
;; => 1
```

### Rational vs Decimal

Emmy preserves exact values:

```clojure
;; π/4 stays as a fraction
(/ pi 4)

;; Force decimal
(double (/ pi 4))
;; => 0.7853981633974483
```

## Combining with Other Features

### With Derivatives

```clojure
;; Show f, f', f'' at a point
(defn show-derivatives [f x]
  (let [df (D f)
        ddf (D df)]
    {:f (f x)
     :df (df x)
     :ddf (ddf x)}))

(show-derivatives #(sin %) (/ pi 4))
```

### With LaTeX Export

```clojure
(require '[emmy.env :refer [->TeX]])

;; Get LaTeX for the evaluation
(defn latex-evaluation [f x]
  {:expr (->TeX (f 'x))
   :at-point (str "x = " x)
   :result (->TeX (f x))})

(latex-evaluation #(sin %) (/ pi 2))
```

## Output

All videos saved to: `media/videos/1080p60/`

## Full Example: Complete Workflow

```clojure
(require '[varcalc.emmy-evaluation :as ev])
(require '[varcalc.emmy-python.equations :as eq])
(require '[emmy.env :refer [sin cos square exp pi D ->TeX]])
(require '[varcalc.manim-quickstart :as mq])

;; Initialize
(mq/init!)

;; 1. Single evaluation
(ev/create-single-evaluation eq/expt1 0)

;; 2. Table of values
(ev/create-evaluation-table
 eq/expt2
 [0 1 2 3 4])

;; 3. Compare sin and cos
(ev/create-comparison-table
 [#(sin %) #(cos %)]
 [0 (/ pi 4) (/ pi 2) (* 3 (/ pi 4)) pi])

;; 4. Derivative at a point
(ev/create-derivative-evaluation
 #(sin %)
 (/ pi 6))

;; Done! Check media/videos/1080p60/
```

## Next Steps

1. Try evaluating your functions from `equations.clj`
2. Create comparison tables for related functions
3. Visualize derivatives at critical points
4. Build custom evaluation workflows

Happy evaluating! 📊✨
