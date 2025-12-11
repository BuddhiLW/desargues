(ns desargues.emmy-python.equations
  (:require [emmy.env :as e :refer :all]))

(defn expt1 [x]
  (e/sin (+ x pi)))

(comment
  (->TeX 'pi)
  ;; => "\\pi"
  (simplify (expt1 0.01))
  ;; => -0.00999983333416633
  ;; => (sin (+ pi 0.01))
  (expt1 'x)
  ;; => (sin (+ x 3.141592653589793))
  (expt1 (literal-number 'x))
  ;; => (sin (+ x 3.141592653589793))

  (->TeX (expt1 'x))
  ;; => "\\sin\\left(x + 3.141592653589793\\right)"
        ;; => "\\sin\\left(x + \\pi\\right)"
  (->TeX ((D expt1) 'x))
  ;; => "\\cos\\left(x + 3.141592653589793\\right)"
        ;; => "\\cos\\left(x + \\pi\\right)"
  )
;; => "{\\sin}^{2}\\left(a + 3\\right)"
