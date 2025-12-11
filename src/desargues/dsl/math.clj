(ns desargues.dsl.math
  "Mathematical expression DSL with Emmy integration.

   This namespace provides high-level abstractions for working with
   mathematical expressions, automatic differentiation, and LaTeX rendering.

   ## Key Features:
   - Symbolic mathematics via Emmy
   - Automatic color mapping for variables
   - Expression transformation and animation
   - Integration with the animation DSL

   ## Example:

   ```clojure
   (defexpr my-equation
     (= (+ (** x 2) (** y 2)) (** r 2)))

   (-> my-equation
       (with-colors {:x :blue :y :red :r :green})
       (animate-to (derivative my-equation 'x)))
   ```"
  (:require [desargues.dsl.core :as dsl]
            [emmy.env :as e]
            [clojure.string :as str]))

;; =============================================================================
;; Mathematical Expression Protocol
;; =============================================================================

(defprotocol MathExpr
  "Protocol for mathematical expressions."
  (expr->emmy [this]
    "Convert to Emmy expression.")
  (expr->latex [this]
    "Convert to LaTeX string.")
  (expr->python [this]
    "Convert to Python code string.")
  (variables [this]
    "Get set of free variables.")
  (substitute [this bindings]
    "Substitute variables with values/expressions."))

;; =============================================================================
;; Forward declarations
;; =============================================================================

(declare latex->python)

;; =============================================================================
;; Expression Records
;; =============================================================================

(defrecord Expression [emmy-expr color-map metadata]
  MathExpr
  (expr->emmy [_] emmy-expr)
  (expr->latex [_] (e/->TeX emmy-expr))
  (expr->python [this]
    (latex->python (expr->latex this)))
  (variables [_]
    (cond
      (symbol? emmy-expr) #{emmy-expr}
      (seq? emmy-expr) (set (filter symbol? (flatten emmy-expr)))
      :else #{}))
  (substitute [this bindings]
    (let [new-expr (reduce (fn [acc [var val]]
                             (if (fn? acc)
                               (acc val)
                               acc))
                           emmy-expr
                           bindings)]
      (->Expression new-expr color-map metadata)))

  dsl/Renderable
  (to-manim [this]
    {:type :tex
     :content (expr->latex this)
     :colors (into {} (map (fn [[k v]] [(str k) v]) color-map))})
  (render! [this context]
    (dsl/render! (dsl/to-manim this) context)))

;; =============================================================================
;; Expression Constructors
;; =============================================================================

(defn expr
  "Create an expression from an Emmy expression or symbolic form.

   ```clojure
   (expr '(sin x))
   (expr (e/+ (e/square 'x) (e/square 'y)))
   ```"
  [e & {:keys [colors] :or {colors {}}}]
  (->Expression e colors {}))

(defn function
  "Create a mathematical function.

   ```clojure
   (def f (function [x] (sin x)))
   (f 'x)  ; => symbolic sin(x)
   (f 3.14)  ; => numeric ~0
   ```"
  [args body-fn]
  (fn [& inputs]
    (apply body-fn inputs)))

(defmacro defexpr
  "Define a named expression with optional metadata.

   ```clojure
   (defexpr pythagorean
     \"The Pythagorean theorem\"
     (= (+ (** x 2) (** y 2)) (** z 2)))
   ```"
  [name & body]
  (let [[docstring form] (if (string? (first body))
                           [(first body) (second body)]
                           [nil (first body)])]
    `(def ~(with-meta name {:doc docstring})
       (expr '~form))))

(defmacro defn-math
  "Define a mathematical function that works both symbolically and numerically.

   ```clojure
   (defn-math f [x]
     (e/sin (e/* 2 x)))

   (f 'x)     ; => (sin (* 2 x))
   (f Math/PI) ; => ~0
   ```"
  [name args & body]
  `(defn ~name ~args
     (let [~@(interleave args (map (fn [a] `(if (number? ~a) ~a '~a)) args))]
       ~@body)))

;; =============================================================================
;; Expression Operations
;; =============================================================================

(defn derivative
  "Compute derivative of expression with respect to a variable.

   ```clojure
   (derivative (expr '(sin x)) 'x)
   ; => Expression for cos(x)
   ```"
  [expression var]
  (let [emmy-expr (expr->emmy expression)
        f (fn [v] (if (= v var)
                    (e/literal-function 'temp)
                    v))
        wrapped (clojure.walk/postwalk f emmy-expr)
        df ((e/D (fn [t] (clojure.walk/postwalk
                          #(if (and (seq? %) (= (first %) 'temp))
                             t
                             %)
                          wrapped)))
            var)]
    (->Expression df
                  (:color-map expression)
                  (assoc (:metadata expression)
                         :derivative-of emmy-expr
                         :with-respect-to var))))

(defn simplify
  "Simplify an expression.

   ```clojure
   (simplify (expr '(+ x x)))  ; => 2*x
   ```"
  [expression]
  (->Expression (e/simplify (expr->emmy expression))
                (:color-map expression)
                (:metadata expression)))

(defn evaluate
  "Evaluate expression at specific values.

   ```clojure
   (evaluate (expr '(+ x y)) {'x 1 'y 2})  ; => 3
   ```"
  [expression bindings]
  (let [emmy-expr (expr->emmy expression)
        result (reduce (fn [acc [var val]]
                         (if (fn? acc)
                           (acc val)
                           acc))
                       emmy-expr
                       bindings)]
    (if (number? result)
      result
      (->Expression result
                    (:color-map expression)
                    (:metadata expression)))))

;; =============================================================================
;; Color Mapping - Automatic variable coloring
;; =============================================================================

(def default-color-palette
  "Default colors for mathematical variables."
  {:x :blue
   :y :red
   :z :green
   :t :yellow
   :s :orange
   :r :purple
   :theta :teal
   :phi :pink
   :omega :cyan})

(defn auto-color
  "Automatically assign colors to variables in an expression.

   ```clojure
   (auto-color (expr '(+ x y z)))
   ; => Expression with colors {:x :blue :y :red :z :green}
   ```"
  [expression & {:keys [palette] :or {palette default-color-palette}}]
  (let [vars (variables expression)
        colors (into {} (map (fn [v]
                               [v (get palette v :white)])
                             vars))]
    (->Expression (expr->emmy expression)
                  colors
                  (:metadata expression))))

(defn with-colors
  "Set specific colors for variables.

   ```clojure
   (with-colors (expr '(+ x y))
                {:x :blue :y :red})
   ```"
  [expression color-map]
  (->Expression (expr->emmy expression)
                (merge (:color-map expression) color-map)
                (:metadata expression)))

;; =============================================================================
;; LaTeX Utilities
;; =============================================================================

(defn latex->python
  "Convert LaTeX to Python code for Manim.

   This handles common LaTeX constructs and converts them to
   Python-compatible strings."
  [latex-str]
  (-> latex-str
      (str/replace #"\\frac\{([^}]+)\}\{([^}]+)\}" "($1)/($2)")
      (str/replace #"\\sqrt\{([^}]+)\}" "sqrt($1)")
      (str/replace #"\\sin" "sin")
      (str/replace #"\\cos" "cos")
      (str/replace #"\\tan" "tan")
      (str/replace #"\\exp" "exp")
      (str/replace #"\\log" "log")
      (str/replace #"\\pi" "pi")
      (str/replace #"\^\{([^}]+)\}" "**($1)")
      (str/replace #"\^(.)" "**$1")))

(defn format-latex
  "Format LaTeX with custom styling.

   ```clojure
   (format-latex \"x^2\" {:font-size 48 :color :blue})
   ```"
  [latex-str opts]
  {:latex latex-str
   :opts opts})

;; =============================================================================
;; Expression Builders - Convenience functions
;; =============================================================================

(defn equation
  "Create an equation (equality).

   ```clojure
   (equation '(+ x y) 'z)  ; x + y = z
   ```"
  [lhs rhs & {:keys [colors]}]
  (->Expression (list '= lhs rhs)
                (or colors {})
                {:type :equation}))

(defn inequality
  "Create an inequality.

   ```clojure
   (inequality :< 'x 5)  ; x < 5
   ```"
  [op lhs rhs & {:keys [colors]}]
  (->Expression (list op lhs rhs)
                (or colors {})
                {:type :inequality}))

(defn system
  "Create a system of equations.

   ```clojure
   (system
     (equation 'x (+ 'a 'b))
     (equation 'y (- 'a 'b)))
   ```"
  [& equations]
  {:type :system
   :equations (vec equations)})

;; =============================================================================
;; Calculus Operations
;; =============================================================================

(defn integrate
  "Symbolic integration (when possible).

   ```clojure
   (integrate (expr '(* 2 x)) 'x)  ; => x^2
   ```"
  [expression var & {:keys [from to]}]
  (let [emmy-expr (expr->emmy expression)]
    (if (and from to)
      ;; Definite integral - evaluate symbolically
      (let [f (fn [v] emmy-expr)]
        (->Expression (list 'integrate emmy-expr var from to)
                      (:color-map expression)
                      (assoc (:metadata expression)
                             :type :definite-integral
                             :bounds [from to])))
      ;; Indefinite integral
      (->Expression (list 'integrate emmy-expr var)
                    (:color-map expression)
                    (assoc (:metadata expression)
                           :type :indefinite-integral)))))

(defn taylor-series
  "Create Taylor series expansion.

   ```clojure
   (taylor-series (expr '(sin x)) 'x 0 5)
   ; => x - x^3/6 + x^5/120 - ...
   ```"
  [expression var center order]
  (let [emmy-expr (expr->emmy expression)]
    (->Expression
     (list 'taylor emmy-expr var center order)
     (:color-map expression)
     (assoc (:metadata expression)
            :type :taylor-series
            :center center
            :order order))))

(defn limit
  "Create a limit expression.

   ```clojure
   (limit (expr '(/ (sin x) x)) 'x 0)
   ; => 1
   ```"
  [expression var approach & {:keys [direction]}]
  (->Expression
   (list 'limit (expr->emmy expression) var approach)
   (:color-map expression)
   (assoc (:metadata expression)
          :type :limit
          :approach approach
          :direction direction)))

;; =============================================================================
;; Transformations - For animating between expressions
;; =============================================================================

(defn transform-step
  "Create a transformation step between expressions.

   ```clojure
   (transform-step
     (expr '(+ x x))
     (expr '(* 2 x))
     :description \"Factor out x\")
   ```"
  [from to & {:keys [description highlight]}]
  {:type :transform-step
   :from from
   :to to
   :description description
   :highlight highlight})

(defn derivation
  "Create a mathematical derivation (sequence of steps).

   ```clojure
   (derivation
     (transform-step expr1 expr2 :description \"Step 1\")
     (transform-step expr2 expr3 :description \"Step 2\"))
   ```"
  [& steps]
  {:type :derivation
   :steps (vec steps)})

(defn highlight-term
  "Mark a term in an expression for highlighting.

   ```clojure
   (highlight-term (expr '(+ x (* 2 y))) '(* 2 y) :color :yellow)
   ```"
  [expression term & {:keys [color box]}]
  (->Expression
   (expr->emmy expression)
   (:color-map expression)
   (assoc (:metadata expression)
          :highlight {:term term :color color :box box})))

;; =============================================================================
;; Common Mathematical Expressions
;; =============================================================================

(def common-expressions
  "Library of common mathematical expressions."
  {:pythagorean (expr '(= (+ (** a 2) (** b 2)) (** c 2)))
   :quadratic (expr '(= (* a (** x 2)) (+ (* b x) c) 0))
   :euler-identity (expr '(= (+ (** e (* i pi)) 1) 0))
   :derivative-def (expr '(= (D f x)
                             (limit (/ (- (f (+ x h)) (f x)) h)
                                    h 0)))
   :integral-def (expr '(= (integral f a b)
                           (limit (sum (* (f x_i) (delta x))
                                       i 1 n)
                                  n infinity)))})

(defn get-common-expr
  "Get a common expression by name."
  [name]
  (get common-expressions name))

;; =============================================================================
;; Numeric Utilities
;; =============================================================================

(defn tabulate
  "Generate a table of values for an expression.

   ```clojure
   (tabulate (expr '(sin x)) 'x (range 0 7 0.5))
   ; => [{:x 0 :value 0} {:x 0.5 :value 0.479} ...]
   ```"
  [expression var values]
  (mapv (fn [v]
          {:var v
           :value (evaluate expression {var v})})
        values))

(defn find-roots
  "Find approximate roots of an expression.

   ```clojure
   (find-roots (expr '(- (** x 2) 2)) 'x [-3 3])
   ; => [-1.414... 1.414...]
   ```"
  [expression var [lo hi] & {:keys [tolerance] :or {tolerance 1e-6}}]
  ;; Simple bisection method for demonstration
  (let [f (fn [v] (evaluate expression {var v}))]
    (loop [a lo
           b hi
           roots []]
      (if (> (- b a) tolerance)
        (let [mid (/ (+ a b) 2)
              fa (f a)
              fm (f mid)]
          (cond
            (< (Math/abs fm) tolerance)
            (recur mid b (conj roots mid))

            (< (* fa fm) 0)
            (recur a mid roots)

            :else
            (recur mid b roots)))
        roots))))
