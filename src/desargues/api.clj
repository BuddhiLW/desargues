(ns desargues.api
  "Clean API for mathematical animations (Facade Pattern)"
  (:require [desargues.domain.math-expression :as expr]
            [desargues.domain.protocols :as p]
            [desargues.domain.services :as svc]
            [desargues.domain.number-theory :as nt]
            [desargues.domain.number-theory-services :as nts]
            [desargues.infrastructure.manim-adapter :as manim]
            [desargues.manim-quickstart :as mq]
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
;; Number Theory / Factorization API
;; ============================================================================

(defn factorize
  "Create a prime factorization of n.
   Returns a PrimeFactorization record with :n and :factors {prime exponent}."
  [n]
  (nt/create-factorization n))

(defn factorization-str
  "Get string representation of factorization (e.g., '24 = 2^3 × 3')."
  [fact-or-n]
  (if (number? fact-or-n)
    (nts/factorization->str (nts/prime-factorize fact-or-n))
    (nt/factorization->str fact-or-n)))

(defn factorization-latex
  "Get LaTeX representation of factorization."
  [fact-or-n]
  (if (number? fact-or-n)
    (nts/factorization->latex (nts/prime-factorize fact-or-n))
    (nt/factorization->latex fact-or-n)))

(defn dot-array
  "Create a visual array of n dots.
   
   Options:
   - :spacing - distance between dots
   - :direction - :horizontal or :vertical
   - :color - dot color
   - :radius - dot radius"
  [n & opts]
  ;; Requires manim.factorization - lazy require to avoid init issues
  (require '[desargues.manim.factorization :as fact])
  (apply (resolve 'desargues.manim.factorization/create-dot-array) n opts))

(defn dot-grid
  "Create a visual grid of dots.
   
   For 2D: (dot-grid rows cols)
   For 3D: (dot-grid x y z :3d true)
   
   Options:
   - :spacing - distance between dots
   - :color - dot color
   - :radius - dot radius
   - :3d - use 3D spheres"
  [& args]
  (require '[desargues.manim.factorization :as fact])
  (let [[dims opts] (split-with number? args)
        opts-map (apply hash-map opts)]
    (if (:3d opts-map)
      (apply (resolve 'desargues.manim.factorization/create-dot-grid-3d)
             (concat dims (mapcat identity (dissoc opts-map :3d))))
      (apply (resolve 'desargues.manim.factorization/create-dot-grid-2d)
             (concat dims (mapcat identity opts-map))))))

(defn factorization-visual
  "Create the complete factorization visualization for n.
   Returns {:n :dots :levels :arrangement :factorization}
   
   Options:
   - :colors - vector of colors for nesting levels
   - :spacing - dot spacing
   - :radius - dot radius  
   - :prefer-3d - force 3D arrangement"
  [n & {:keys [colors spacing radius prefer-3d]
        :or {colors [:blue :red :green :yellow :purple]
             spacing 0.5
             radius 0.1
             prefer-3d false}}]
  (require '[desargues.manim.factorization :as fact])
  ((resolve 'desargues.manim.factorization/create-factorization-hierarchy)
   n :colors colors :spacing spacing :radius radius :prefer-3d prefer-3d))

(defn animate-factorization
  "Animate the prime factorization of n.
   Shows dots, then progressively groups them with colored rectangles.
   
   Options:
   - :colors - level colors (default [:blue :red :green :yellow :purple])
   - :prefer-3d - force 3D (default false)
   - :show-title - show equation title (default true)
   - :quality - render quality (default 'medium_quality')"
  [n & {:keys [colors prefer-3d show-title quality]
        :or {colors [:blue :red :green :yellow :purple]
             prefer-3d false
             show-title true
             quality "medium_quality"}}]
  (require '[desargues.manim.factorization :as fact])
  ((resolve 'desargues.manim.factorization/visualize-factorization!)
   n :colors colors :prefer-3d prefer-3d :show-title show-title :quality quality))

;; ============================================================================
;; Factorization Scene Builder
;; ============================================================================

(defrecord FactorizationSceneBuilder [n colors prefer-3d show-title quality])

(defn factorization-scene
  "Start building a factorization scene."
  []
  (->FactorizationSceneBuilder nil [:blue :red :green :yellow :purple] false true "medium_quality"))

(defn with-number
  "Set the number to factorize."
  [builder n]
  (assoc builder :n n))

(defn with-colors
  "Set the level colors."
  [builder colors]
  (assoc builder :colors colors))

(defn with-3d
  "Enable or disable 3D visualization."
  [builder enabled?]
  (assoc builder :prefer-3d enabled?))

(defn with-title
  "Enable or disable title display."
  [builder enabled?]
  (assoc builder :show-title enabled?))

(defn with-quality
  "Set render quality ('low_quality', 'medium_quality', 'high_quality')."
  [builder quality]
  (assoc builder :quality quality))

(defn build-factorization!
  "Build and render the factorization scene."
  [builder]
  (when-not (:n builder)
    (throw (ex-info "Must specify a number with (with-number builder n)" {})))
  (animate-factorization (:n builder)
                         :colors (:colors builder)
                         :prefer-3d (:prefer-3d builder)
                         :show-title (:show-title builder)
                         :quality (:quality builder)))

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
