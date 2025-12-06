(ns varcalc.domain.math-expression
  "Domain model for mathematical expressions (DDD Entity/Value Object)"
  (:require [emmy.env :as e]))

;; ============================================================================
;; Value Objects (Immutable mathematical values)
;; ============================================================================

(defrecord LaTeX [content]
  Object
  (toString [_] content))

(defrecord PythonCode [content]
  Object
  (toString [_] content))

(defrecord NumericValue [value]
  Object
  (toString [_] (str value)))

;; ============================================================================
;; Entity: MathExpression (has identity and behavior)
;; ============================================================================

(defrecord MathExpression [id expr latex python-code metadata]
  Object
  (toString [_] (str "MathExpression[" id "]: " latex)))

(defn create-expression
  "Factory: Create a mathematical expression from Emmy form"
  ([expr]
   (create-expression expr {}))
  ([expr metadata]
   (->MathExpression
    (gensym "expr-")
    expr
    nil  ;; Lazy - computed on demand
    nil  ;; Lazy - computed on demand
    metadata)))

;; ============================================================================
;; Entity: Function (mathematical function with domain)
;; ============================================================================

(defrecord MathFunction [id name f domain codomain metadata]
  Object
  (toString [_] (str "MathFunction[" name "]")))

(defn create-function
  "Factory: Create a mathematical function"
  ([name f]
   (create-function name f nil nil {}))
  ([name f domain codomain metadata]
   (->MathFunction
    (gensym "func-")
    name
    f
    domain
    codomain
    metadata)))

;; ============================================================================
;; Value Object: Point (for evaluation)
;; ============================================================================

(defrecord Point [variable value]
  Object
  (toString [_] (str variable " = " value)))

(defn point [var val]
  (->Point var val))

;; ============================================================================
;; Value Object: Evaluation Result
;; ============================================================================

(defrecord EvaluationResult [input output expression steps]
  Object
  (toString [_] (str input " → " output)))

(defn evaluation-result
  ([input output]
   (evaluation-result input output nil []))
  ([input output expr steps]
   (->EvaluationResult input output expr steps)))
