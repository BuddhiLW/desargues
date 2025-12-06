(ns varcalc.core
  (:require [varcalc.emmy-manim-examples :as ex]
            [varcalc.manim-quickstart :as mq]
            [emmy.env :as e])
  (:gen-class))

(defn -main
  "Generate a derivative animation using Emmy + Manim"
  [& args]
  (println "=== Varcalc: Mathematical Animation Generator ===\n")

  ;; Initialize
  (println "Initializing Python and Manim...")
  (mq/init!)

  ;; Create a function: sin(x)
  (println "\nCreating mathematical function: sin(x)")
  (defn my-func [x]
    (e/sin x))

  ;; Compute derivative
  (println "Computing derivative using Emmy...")
  (def deriv-func (e/D my-func))

  ;; Display LaTeX
  (println "\nLaTeX representations:")
  (println "  f(x)  =" (e/->TeX (my-func 'x)))
  (println "  f'(x) =" (e/->TeX (deriv-func 'x)))
  (println "  (Should be cos(x))")

  ;; Create animation scene
  (println "\nBuilding animation scene...")
  (println "  Showing function and its derivative side-by-side")

  ;; Use the existing derivative animation function
  (println "\nRendering animation...")
  (ex/create-derivative-animation my-func)

  (println "\n✓ Animation complete!")
  (println "Video saved to: media/videos/1080p60/")
  (println "\n=== Done! ==="))
