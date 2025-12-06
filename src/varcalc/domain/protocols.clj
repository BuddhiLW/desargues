(ns varcalc.domain.protocols
  "Domain protocols (Interfaces for SOLID design)")

;; ============================================================================
;; Core Domain Protocols
;; ============================================================================

(defprotocol IMathematicalObject
  "Contract for any mathematical object"
  (to-latex [this] "Convert to LaTeX representation")
  (to-python [this] "Convert to Python code")
  (simplify [this] "Simplify the mathematical object")
  (metadata* [this] "Get metadata about this object"))

(defprotocol IEvaluable
  "Contract for objects that can be evaluated"
  (evaluate [this point] "Evaluate at a specific point")
  (evaluate-symbolic [this point] "Symbolically evaluate at a point"))

(defprotocol IDifferentiable
  "Contract for objects that can be differentiated"
  (derivative [this] "Compute derivative")
  (derivative-at [this point] "Evaluate derivative at a point")
  (nth-derivative [this n] "Compute nth derivative"))

(defprotocol IIntegrable
  "Contract for objects that can be integrated"
  (integrate [this] "Compute indefinite integral")
  (definite-integral [this lower upper] "Compute definite integral"))

;; ============================================================================
;; Rendering Protocols (Infrastructure)
;; ============================================================================

(defprotocol IRenderable
  "Contract for objects that can be rendered visually"
  (render [this options] "Render to visual representation")
  (to-mobject [this] "Convert to Manim mobject"))

(defprotocol IAnimatable
  "Contract for objects that can be animated"
  (create-animation [this animation-type options] "Create animation")
  (animate-creation [this] "Animate the creation of this object")
  (animate-transformation [this target] "Animate transformation to target"))

;; ============================================================================
;; Scene Management Protocols
;; ============================================================================

(defprotocol IScene
  "Contract for animation scenes"
  (add-object [this obj] "Add an object to the scene")
  (remove-object [this obj] "Remove an object from the scene")
  (play-animation [this animation] "Play an animation")
  (wait-duration [this duration] "Wait for duration")
  (render-scene [this] "Render the complete scene"))

(defprotocol IAnimation
  "Contract for animations"
  (run-time [this] "Get runtime of animation")
  (with-run-time [this time] "Set runtime")
  (reverse-animation [this] "Create reverse animation"))

;; ============================================================================
;; Conversion Protocols (Bridges)
;; ============================================================================

(defprotocol IEmmyToPython
  "Bridge from Emmy to Python"
  (emmy->python-expr [this] "Convert Emmy to Python expression")
  (emmy->python-func [this] "Convert Emmy function to Python callable"))

(defprotocol IPythonToEmmy
  "Bridge from Python to Emmy"
  (python->emmy-expr [this code] "Convert Python code to Emmy")
  (python-func->emmy [this pyfunc] "Convert Python function to Emmy"))

;; ============================================================================
;; Repository Protocols (Data Access)
;; ============================================================================

(defprotocol IExpressionRepository
  "Repository for mathematical expressions"
  (save-expression [this expr] "Save an expression")
  (find-expression [this id] "Find expression by ID")
  (find-all-expressions [this] "Find all expressions")
  (delete-expression [this id] "Delete an expression"))

(defprotocol ISceneRepository
  "Repository for animation scenes"
  (save-scene [this scene] "Save a scene configuration")
  (find-scene [this id] "Find scene by ID")
  (render-saved-scene [this id] "Render a previously saved scene"))
