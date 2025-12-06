(ns varcalc.api
  "Clean API for mathematical animations (Facade Pattern)"
  (:require [varcalc.domain.math-expression :as expr]
            [varcalc.domain.protocols :as p]
            [varcalc.domain.services :as svc]
            [varcalc.infrastructure.manim-adapter :as manim]
            [varcalc.manim-quickstart :as mq]
            [emmy.env :as e]))

;; ============================================================================
;; Initialization
;; ============================================================================

(defn init!
  "Initialize the animation system"
  []
  (mq/init!))

;; ============================================================================
;; Expression Creation (Factory Methods)
;; ============================================================================

(defn expr
  "Create a mathematical expression from Emmy form"
  ([form]
   (expr/create-expression form))
  ([form metadata]
   (expr/create-expression form metadata)))

(defn func
  "Create a mathematical function"
  ([name f]
   (expr/create-function name f))
  ([name f domain codomain]
   (expr/create-function name f domain codomain {})))

(defn point
  "Create an evaluation point"
  [var val]
  (expr/point var val))

;; ============================================================================
;; Mathematical Operations (Domain Services)
;; ============================================================================

(defn derivative
  "Compute derivative of an expression or function"
  [math-obj]
  (svc/differentiate-expression math-obj))

(defn evaluate-at
  "Evaluate at a specific point"
  [math-obj point]
  (svc/evaluate-expression math-obj point))

(defn simplify
  "Simplify an expression"
  [math-expr]
  (svc/simplify-expression math-expr))

(defn to-latex
  "Convert to LaTeX"
  [math-obj]
  (svc/expression-to-latex math-obj))

;; ============================================================================
;; Scene Construction (Fluent API)
;; ============================================================================

(defn scene
  "Start building a scene"
  []
  (manim/->scene))

(defn show
  "Add object to scene with creation animation"
  [builder obj]
  (-> builder
      (manim/with-object obj)
      (manim/with-animation (p/animate-creation obj))))

(defn transform
  "Transform one object to another"
  [builder from to]
  (manim/with-animation builder (p/animate-transformation from to)))

(defn wait
  "Wait for duration"
  [builder duration]
  (manim/with-wait builder duration))

(defn render!
  "Render the scene"
  [builder]
  (manim/build-and-render builder))

;; ============================================================================
;; High-Level Workflows (Use Cases)
;; ============================================================================

(defn animate-expression
  "Show an expression with animation"
  [math-expr]
  (-> (scene)
      (show math-expr)
      (wait 2)
      (render!)))

(defn animate-derivative
  "Show function and its derivative"
  [math-expr]
  (let [deriv (derivative math-expr)]
    (-> (scene)
        (show math-expr)
        (wait 1)
        (show deriv)
        (wait 2)
        (render!))))

(defn animate-transformation
  "Transform one expression into another"
  [from to]
  (-> (scene)
      (show from)
      (wait 1)
      (transform from to)
      (wait 2)
      (render!)))

(defn animate-simplification
  "Show expression being simplified"
  [math-expr]
  (let [simplified (simplify math-expr)]
    (animate-transformation math-expr simplified)))

;; ============================================================================
;; Evaluation and Tables
;; ============================================================================

(defn tabulate
  "Create table of function values"
  [func points]
  (svc/tabulate-function func points))

(defn compare-functions
  "Compare multiple functions at same points"
  [functions points]
  (svc/compare-at-points functions points))

;; ============================================================================
;; Convenience Macros
;; ============================================================================

(defmacro defexpr
  "Define a named mathematical expression"
  [name form]
  `(def ~name (expr '~form {:name ~(str name)})))

(defmacro deffunc
  "Define a named mathematical function"
  [name args & body]
  `(def ~name
     (func ~(str name)
           (fn ~args ~@body))))

;; ============================================================================
;; REPL Examples
;; ============================================================================

(comment
  ;; === Setup ===
  (init!)

  ;; === Create Expressions ===

  ;; Simple expression
  (def e1 (expr '(square (sin (+ x 3)))))

  ;; With metadata
  (def e2 (expr '(+ (* a (square x)) (* b x) c)
                {:name "quadratic"
                 :description "General quadratic form"}))

  ;; === Create Functions ===

  ;; Simple function
  (def f1 (func "f" #(e/sin %)))

  ;; With domain/codomain
  (def f2 (func "g"
                #(e/square %)
                [:real-numbers]
                [:non-negative-reals]))

  ;; === Operations ===

  ;; Derivative
  (def e1-prime (derivative e1))

  ;; Evaluate
  (evaluate-at e1 (point 'x 0))

  ;; Simplify
  (def simplified (simplify e2))

  ;; LaTeX
  (to-latex e1)

  ;; === Animations (Fluent API) ===

  ;; Simple animation
  (-> (scene)
      (show e1)
      (wait 2)
      (render!))

  ;; Derivative animation
  (-> (scene)
      (show e1)
      (wait 1)
      (show e1-prime)
      (wait 2)
      (render!))

  ;; Transformation
  (-> (scene)
      (show e1)
      (wait 1)
      (transform e1 simplified)
      (wait 2)
      (render!))

  ;; === High-Level Workflows ===

  ;; Show expression
  (animate-expression e1)

  ;; Show derivative
  (animate-derivative e1)

  ;; Show simplification
  (animate-simplification e2)

  ;; === Using Macros ===

  ;; Define expression
  (defexpr pythagorean
    (+ (square (sin x)) (square (cos x))))

  ;; Animate it
  (animate-expression pythagorean)

  ;; Define function
  (deffunc gaussian [x]
    (e/exp (- (e/square x))))

  ;; Use it
  (def gauss-expr (expr (gaussian 'x)))
  (animate-expression gauss-expr)

  ;; === Tables and Comparisons ===

  ;; Tabulate
  (tabulate f1 [(point 'x 0)
                (point 'x (/ e/pi 2))
                (point 'x e/pi)])

  ;; Compare
  (compare-functions
   [f1 f2]
   [(point 'x 0) (point 'x 1) (point 'x 2)]))
