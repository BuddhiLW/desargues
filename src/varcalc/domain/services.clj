(ns varcalc.domain.services
  "Domain services (operations not belonging to specific entities)"
  (:require [varcalc.domain.protocols :as p]
            [varcalc.domain.math-expression :as expr]
            [emmy.env :as e]))

;; ============================================================================
;; Mathematical Operations Service
;; ============================================================================

(defn differentiate-expression
  "Service: Compute derivative of an expression"
  [math-expr]
  (let [derivative-expr (e/D (:expr math-expr))]
    (expr/create-expression
     derivative-expr
     (assoc (:metadata math-expr) :derived-from (:id math-expr)))))

(defn evaluate-expression
  "Service: Evaluate expression at a point"
  [math-expr point]
  (let [var (:variable point)
        val (:value point)
        ;; Just evaluate the expression directly
        result ((:expr math-expr) val)
        simplified (e/simplify result)]
    (expr/evaluation-result point simplified (:expr math-expr))))

(defn simplify-expression
  "Service: Simplify an expression"
  [math-expr]
  (let [simplified (e/simplify (:expr math-expr))]
    (expr/create-expression
     simplified
     (assoc (:metadata math-expr) :simplified-from (:id math-expr)))))

;; ============================================================================
;; Conversion Service (Bridge between Emmy and representations)
;; ============================================================================

(defn expression-to-latex
  "Service: Convert expression to LaTeX"
  [math-expr]
  (expr/->LaTeX (e/->TeX (:expr math-expr))))

(defn expression-to-python
  "Service: Convert expression to Python (requires latex2py integration)"
  [math-expr latex-converter]
  ;; Inject dependency on latex converter (Dependency Inversion)
  (let [latex (expression-to-latex math-expr)]
    (expr/->PythonCode (latex-converter (:content latex)))))

;; ============================================================================
;; Function Operations Service
;; ============================================================================

(defn apply-function
  "Service: Apply a mathematical function to an argument"
  [math-func arg]
  (let [result ((:f math-func) arg)]
    (expr/create-expression
     result
     {:function (:name math-func)
      :argument arg})))

(defn compose-functions
  "Service: Compose two mathematical functions (f ∘ g)"
  [f g]
  (let [composed (comp (:f f) (:f g))
        name (str (:name f) "∘" (:name g))]
    (expr/create-function name composed)))

;; ============================================================================
;; Comparison Service
;; ============================================================================

(defn compare-at-points
  "Service: Evaluate multiple functions at same points"
  [functions points]
  (for [point points]
    {:point point
     :results (for [func functions]
                {:function (:name func)
                 :value (apply-function func (:value point))})}))

;; ============================================================================
;; Tabulation Service
;; ============================================================================

(defn tabulate-function
  "Service: Create a table of function values"
  [math-func points]
  {:function math-func
   :points points
   :values (map (fn [point]
                  {:point point
                   :value (apply-function math-func (:value point))})
                points)})

;; ============================================================================
;; Transformation Service
;; ============================================================================

(defn transform-expression
  "Service: Apply a transformation to an expression"
  [math-expr transformation]
  (let [transformed ((transformation (:expr math-expr)))]
    (expr/create-expression
     transformed
     (assoc (:metadata math-expr)
            :transformation transformation
            :source (:id math-expr)))))
