(ns desargues.core
  (:require [desargues.emmy-manim-examples :as ex]
            [desargues.manim-quickstart :as mq]
            [desargues.videos.render :as render]
            [emmy.env :as e])
  (:gen-class))

(defn -main
  "Generate mathematical animations using Emmy + Manim.

   Usage:
     lein run                    - Render brachistochrone full derivation
     lein run intro              - Render just the intro
     lein run step N             - Render derivation step N (1-12)
     lein run derivative         - Render sin(x) derivative demo"
  [& args]
  (println "=== Varcalc: Mathematical Animation Generator ===\n")

  (let [cmd (first args)]
    (case cmd
      ;; Default: full brachistochrone derivation
      nil
      (do
        (println "Rendering full brachistochrone derivation...")
        (render/render-full-derivation))

      ;; Just intro
      "intro"
      (do
        (println "Rendering brachistochrone intro...")
        (render/render-intro))

      ;; Specific step
      "step"
      (let [n (Integer/parseInt (second args))]
        (println (str "Rendering brachistochrone step " n "..."))
        (render/render-step n))

      ;; Quickstart demo (simple circle)
      "quickstart"
      (do
        (println "Running Manim quickstart demo...")
        (mq/quickstart!))

      ;; Original derivative demo
      "derivative"
      (do
        (println "Initializing Python and Manim...")
        (mq/init!)

        (println "\nCreating mathematical function: sin(x)")
        (defn my-func [x]
          (e/sin x))

        (println "Computing derivative using Emmy...")
        (def deriv-func (e/D my-func))

        (println "\nLaTeX representations:")
        (println "  f(x)  =" (e/->TeX (my-func 'x)))
        (println "  f'(x) =" (e/->TeX (deriv-func 'x)))

        (println "\nRendering animation...")
        (ex/create-derivative-animation my-func))

      ;; Unknown command
      (println (str "Unknown command: " cmd "\n"
                    "Usage: lein run [intro|step N|derivative]"))))

  (println "\n=== Done! ==="))
