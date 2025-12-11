(ns desargues.types
  "Type aliases for Typed Clojure annotations.
   
   This namespace defines type aliases used throughout the codebase
   for compile-time type checking with Typed Clojure."
  (:require [typed.clojure :as t]))

;; =============================================================================
;; Geometric Types
;; =============================================================================

(t/defalias Point2D
  "A 2D point as [x y]"
  '[t/Num t/Num])

(t/defalias Point3D
  "A 3D point as [x y z]"
  '[t/Num t/Num t/Num])

(t/defalias Point
  "A point in 2D or 3D space"
  (t/U Point2D Point3D))

(t/defalias Range3
  "A 3D range as [min max step]"
  '[t/Num t/Num t/Num])

(t/defalias Vector2D
  "A 2D vector"
  '[t/Num t/Num])

(t/defalias Vector3D
  "A 3D vector"
  '[t/Num t/Num t/Num])

;; =============================================================================
;; Mathematical Types
;; =============================================================================

(t/defalias EmmyExpr
  "An Emmy symbolic expression - opaque type"
  t/Any)

(t/defalias LaTeXString
  "A LaTeX-formatted string"
  t/Str)

(t/defalias PythonCodeString
  "Python code as a string"
  t/Str)

(t/defalias SymbolicVar
  "A symbolic variable (Clojure symbol)"
  t/Sym)

;; =============================================================================
;; Physics Types
;; =============================================================================

(t/defalias PhysicsState
  "A physics state mapping keywords to numeric values"
  (t/Map t/Kw t/Num))

(t/defalias TrajectoryPoint
  "A single point in a trajectory"
  '{:time t/Num :state PhysicsState})

(t/defalias Trajectory
  "A sequence of trajectory points over time"
  (t/Seq TrajectoryPoint))

(t/defalias DerivativeFn
  "A function that computes state derivatives"
  [PhysicsState :-> PhysicsState])

;; =============================================================================
;; Animation/Timeline Types
;; =============================================================================

(t/defalias Duration
  "A positive duration in seconds"
  t/Num)

(t/defalias Alpha
  "Animation progress value between 0 and 1"
  t/Num)

(t/defalias RateFunc
  "A rate function mapping alpha to eased alpha"
  [Alpha :-> Alpha])

(t/defalias ColorKeyword
  "A color keyword"
  t/Kw)

(t/defalias RGB
  "An RGB color tuple [r g b] with values 0-1"
  '[t/Num t/Num t/Num])

(t/defalias Color
  "A color as keyword or RGB tuple"
  (t/U ColorKeyword RGB))

;; =============================================================================
;; Python Interop Types
;; =============================================================================

(t/defalias PyObject
  "An opaque Python object from libpython-clj"
  t/Any)

(t/defalias ManimMobject
  "A Manim Mobject (Python object)"
  PyObject)

(t/defalias ManimScene
  "A Manim Scene (Python object)"
  PyObject)

(t/defalias ManimAnimation
  "A Manim Animation (Python object)"
  PyObject)

;; =============================================================================
;; Domain Entity Types
;; =============================================================================

(t/defalias ExpressionId
  "Unique identifier for a math expression"
  t/Sym)

(t/defalias FunctionId
  "Unique identifier for a math function"
  t/Sym)

;; =============================================================================
;; Collection Types
;; =============================================================================

(t/defalias PointSeq
  "A sequence of points"
  (t/Seq Point))

(t/defalias NumSeq
  "A sequence of numbers"
  (t/Seq t/Num))

(t/defalias KeywordMap
  "A map with keyword keys"
  (t/Map t/Kw t/Any))
