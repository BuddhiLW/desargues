(ns desargues.domain.protocols
  "Domain protocols (Interfaces for SOLID design)
   
   This namespace defines the core protocol interfaces following SOLID principles.
   Type annotations are provided for compile-time checking with Typed Clojure."
  (:require [typed.clojure :as t]
            [desargues.types :as types]))

;; ============================================================================
;; Core Domain Protocols
;; ============================================================================

(t/ann-protocol IMathematicalObject
                to-latex [IMathematicalObject :-> types/LaTeXString]
                to-python [IMathematicalObject :-> types/PythonCodeString]
                simplify [IMathematicalObject :-> IMathematicalObject]
                metadata* [IMathematicalObject :-> (t/Map t/Kw t/Any)])

(defprotocol IMathematicalObject
  "Contract for any mathematical object"
  (to-latex [this] "Convert to LaTeX representation")
  (to-python [this] "Convert to Python code")
  (simplify [this] "Simplify the mathematical object")
  (metadata* [this] "Get metadata about this object"))

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
                save-expression [IExpressionRepository IMathematicalObject :-> types/ExpressionId]
                find-expression [IExpressionRepository types/ExpressionId :-> (t/Option IMathematicalObject)]
                find-all-expressions [IExpressionRepository :-> (t/Seq IMathematicalObject)]
                delete-expression [IExpressionRepository types/ExpressionId :-> t/Bool])

(defprotocol IExpressionRepository
  "Repository for mathematical expressions"
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
