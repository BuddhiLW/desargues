(ns desargues.domain.protocols
  "Domain protocols (Interfaces for SOLID design)
   
   This namespace defines the core protocol interfaces following SOLID principles.
   Protocols are split according to Interface Segregation Principle (ISP):
   
   ## Conversion Protocols (focused, single-responsibility)
   - ILatexConvertible: Objects that can be converted to LaTeX
   - IPythonConvertible: Objects that can be converted to Python code
   - ISimplifiable: Objects that can be simplified
   - IMetadata: Objects that carry metadata
   
   ## Mathematical Operation Protocols
   - IEvaluable: Objects that can be evaluated at points
   - IDifferentiable: Objects that support differentiation
   - IIntegrable: Objects that support integration
   
   ## Rendering Protocols (Infrastructure)
   - IRenderable: Objects that can be rendered visually
   - IAnimatable: Objects that can be animated
   
   ## Scene Management Protocols
   - IScene: Animation scene construction
   - IAnimation: Animation control
   
   ## Number Theory Visualization Protocols
   - IFactorizable: Objects that can be factored into primes
   - IDotArrangement: Dot arrangement patterns for visualization
   - INestedGrouping: Nested groupings with visual hierarchy
   
   Type annotations are provided for compile-time checking with Typed Clojure."
  (:require [typed.clojure :as t]
            [desargues.types :as types]))

;; ============================================================================
;; Conversion Protocols (ISP: Single-responsibility interfaces)
;; ============================================================================

(t/ann-protocol ILatexConvertible
                to-latex [ILatexConvertible :-> types/LaTeXString])

(defprotocol ILatexConvertible
  "Contract for objects that can be converted to LaTeX.
   Implement this for any object that needs LaTeX rendering."
  (to-latex [this] "Convert to LaTeX representation"))

(t/ann-protocol IPythonConvertible
                to-python [IPythonConvertible :-> types/PythonCodeString])

(defprotocol IPythonConvertible
  "Contract for objects that can be converted to Python code.
   Only implement for objects that need Python interop (e.g., for Manim)."
  (to-python [this] "Convert to Python code"))

(t/ann-protocol ISimplifiable
                simplify [ISimplifiable :-> ISimplifiable])

(defprotocol ISimplifiable
  "Contract for mathematical objects that can be simplified.
   Implement for expressions, equations, and other reducible forms."
  (simplify [this] "Return a simplified version of this object"))

(t/ann-protocol IMetadata
                get-metadata [IMetadata :-> (t/Map t/Kw t/Any)]
                with-metadata [IMetadata (t/Map t/Kw t/Any) :-> IMetadata])

(defprotocol IMetadata
  "Contract for objects that carry metadata.
   Provides immutable metadata access following value object semantics."
  (get-metadata [this] "Get metadata map for this object")
  (with-metadata [this meta] "Return new object with updated metadata"))

;; ============================================================================
;; Mathematical Operation Protocols
;; ============================================================================

(t/ann-protocol IEvaluable
                evaluate [IEvaluable types/Point :-> t/Any]
                evaluate-symbolic [IEvaluable types/Point :-> types/EmmyExpr])

(defprotocol IEvaluable
  "Contract for objects that can be evaluated"
  (evaluate [this point] "Evaluate at a specific point")
  (evaluate-symbolic [this point] "Symbolically evaluate at a point"))

(t/ann-protocol IDifferentiable
                derivative [IDifferentiable :-> IDifferentiable]
                derivative-at [IDifferentiable types/Point :-> t/Num]
                nth-derivative [IDifferentiable t/Int :-> IDifferentiable])

(defprotocol IDifferentiable
  "Contract for objects that can be differentiated"
  (derivative [this] "Compute derivative")
  (derivative-at [this point] "Evaluate derivative at a point")
  (nth-derivative [this n] "Compute nth derivative"))

(t/ann-protocol IIntegrable
                integrate [IIntegrable :-> IIntegrable]
                definite-integral [IIntegrable t/Num t/Num :-> t/Num])

(defprotocol IIntegrable
  "Contract for objects that can be integrated"
  (integrate [this] "Compute indefinite integral")
  (definite-integral [this lower upper] "Compute definite integral"))

;; ============================================================================
;; Rendering Protocols (Infrastructure)
;; ============================================================================

(t/ann-protocol IRenderable
                render [IRenderable (t/Map t/Kw t/Any) :-> types/PyObject]
                to-mobject [IRenderable :-> types/ManimMobject])

(defprotocol IRenderable
  "Contract for objects that can be rendered visually"
  (render [this options] "Render to visual representation")
  (to-mobject [this] "Convert to Manim mobject"))

(t/ann-protocol IAnimatable
                create-animation [IAnimatable t/Kw (t/Map t/Kw t/Any) :-> types/ManimAnimation]
                animate-creation [IAnimatable :-> types/ManimAnimation]
                animate-transformation [IAnimatable IAnimatable :-> types/ManimAnimation])

(defprotocol IAnimatable
  "Contract for objects that can be animated"
  (create-animation [this animation-type options] "Create animation")
  (animate-creation [this] "Animate the creation of this object")
  (animate-transformation [this target] "Animate transformation to target"))

;; ============================================================================
;; Scene Management Protocols
;; ============================================================================

(t/ann-protocol IScene
                add-object [IScene IRenderable :-> IScene]
                remove-object [IScene IRenderable :-> IScene]
                play-animation [IScene types/ManimAnimation :-> IScene]
                wait-duration [IScene types/Duration :-> IScene]
                render-scene [IScene :-> types/PyObject])

(defprotocol IScene
  "Contract for animation scenes"
  (add-object [this obj] "Add an object to the scene")
  (remove-object [this obj] "Remove an object from the scene")
  (play-animation [this animation] "Play an animation")
  (wait-duration [this duration] "Wait for duration")
  (render-scene [this] "Render the complete scene"))

(t/ann-protocol IAnimation
                run-time [IAnimation :-> types/Duration]
                with-run-time [IAnimation types/Duration :-> IAnimation]
                reverse-animation [IAnimation :-> IAnimation])

(defprotocol IAnimation
  "Contract for animations"
  (run-time [this] "Get runtime of animation")
  (with-run-time [this time] "Set runtime")
  (reverse-animation [this] "Create reverse animation"))

;; ============================================================================
;; Conversion Protocols (Bridges)
;; ============================================================================

(t/ann-protocol IEmmyToPython
                emmy->python-expr [IEmmyToPython :-> types/PythonCodeString]
                emmy->python-func [IEmmyToPython :-> types/PyObject])

(defprotocol IEmmyToPython
  "Bridge from Emmy to Python"
  (emmy->python-expr [this] "Convert Emmy to Python expression")
  (emmy->python-func [this] "Convert Emmy function to Python callable"))

(t/ann-protocol IPythonToEmmy
                python->emmy-expr [IPythonToEmmy types/PythonCodeString :-> types/EmmyExpr]
                python-func->emmy [IPythonToEmmy types/PyObject :-> types/EmmyExpr])

(defprotocol IPythonToEmmy
  "Bridge from Python to Emmy"
  (python->emmy-expr [this code] "Convert Python code to Emmy")
  (python-func->emmy [this pyfunc] "Convert Python function to Emmy"))

;; ============================================================================
;; Repository Protocols (Data Access)
;; ============================================================================

(t/ann-protocol IExpressionRepository
                save-expression [IExpressionRepository t/Any :-> types/ExpressionId]
                find-expression [IExpressionRepository types/ExpressionId :-> (t/Option t/Any)]
                find-all-expressions [IExpressionRepository :-> (t/Seq t/Any)]
                delete-expression [IExpressionRepository types/ExpressionId :-> t/Bool])

(defprotocol IExpressionRepository
  "Repository for mathematical expressions.
   Works with any expression type - uses structural typing rather than
   requiring a specific protocol implementation."
  (save-expression [this expr] "Save an expression")
  (find-expression [this id] "Find expression by ID")
  (find-all-expressions [this] "Find all expressions")
  (delete-expression [this id] "Delete an expression"))

(t/ann-protocol ISceneRepository
                save-scene [ISceneRepository IScene :-> t/Sym]
                find-scene [ISceneRepository t/Sym :-> (t/Option IScene)]
                render-saved-scene [ISceneRepository t/Sym :-> types/PyObject])

(defprotocol ISceneRepository
  "Repository for animation scenes"
  (save-scene [this scene] "Save a scene configuration")
  (find-scene [this id] "Find scene by ID")
  (render-saved-scene [this id] "Render a previously saved scene"))

;; ============================================================================
;; Number Theory Visualization Protocols
;; ============================================================================

(t/ann-protocol IFactorizable
                prime-factors [IFactorizable :-> (t/Map t/Int t/Int)]
                factorization-tree [IFactorizable :-> t/Any])

(defprotocol IFactorizable
  "Contract for objects that can be factored into primes.
   Used for number theory visualizations."
  (prime-factors [this] "Return prime factorization as {prime exponent}")
  (factorization-tree [this] "Return nested factorization structure"))

(t/ann-protocol IDotArrangement
                to-dots [IDotArrangement :-> (t/Seq (t/Vec t/Num))]
                dimensions [IDotArrangement :-> (t/Vec t/Int)]
                arrangement-type [IDotArrangement :-> t/Kw])

(defprotocol IDotArrangement
  "Contract for dot arrangement patterns.
   Defines how dots are spatially arranged for visualization."
  (to-dots [this] "Convert to sequence of [x y z] dot positions")
  (dimensions [this] "Return dimensions as [rows cols] or [x y z]")
  (arrangement-type [this] "Return :grid-2d, :grid-3d, or :linear"))

(t/ann-protocol INestedGrouping
                nesting-depth [INestedGrouping :-> t/Int]
                children [INestedGrouping :-> (t/Seq t/Any)]
                grouping-levels [INestedGrouping :-> (t/Seq t/Any)])

(defprotocol INestedGrouping
  "Contract for nested groupings with visual hierarchy.
   Used for visualizing factorization as nested rectangles/boxes."
  (nesting-depth [this] "Return the maximum nesting depth")
  (children [this] "Return child groupings or leaf elements")
  (grouping-levels [this] "Return sequence of all grouping levels"))
