(ns varcalc.manim
  (:require [libpython-clj2.python :as py]
            [libpython-clj2.require :refer [require-python]]))

;; Initialize Python and set up the conda environment
(defn init-python! []
  ;; Point to your conda manim environment
  ;; Adjust the path if your conda installation is elsewhere
  (py/initialize!
   :python-executable "/home/lages/anaconda3/envs/manim/bin/python"
   :library-path "/home/lages/anaconda3/envs/manim/lib/libpython3.12.so")

  ;; Add conda environment's site-packages to Python path
  (let [sys (py/import-module "sys")]
    (py/call-attr (py/get-attr sys "path") "insert" 0
                  "/home/lages/anaconda3/envs/manim/lib/python3.12/site-packages")))

;; Import manim
(defn import-manim! []
  (require-python '[manim :as manim :refer [Scene Circle Create PINK]]))

;; CreateCircle scene - Clojure version
(defn create-circle-scene []
  ;; Define a Scene subclass
  (py/py.
   (py/create-class
    "CreateCircle"
    [Scene]
    {"construct"
     (py/->py-fn
      (fn [self]
        (let [circle (Circle)]
          ;; Set fill color and opacity
          (.set_fill circle PINK :opacity 0.5)
          ;; Play the Create animation
          (.play self (Create circle)))))})))

;; Alternative approach using defclass-like pattern
(defn make-create-circle-scene []
  (py/python-type
   "CreateCircle"
   [Scene]
   {"construct"
    (fn [self]
      (let [circle (Circle)]
        (py/call-attr circle "set_fill" PINK :opacity 0.5)
        (py/call-attr self "play" (Create circle))))}))

(comment
  ;; Usage instructions:
  ;; 1. Start a REPL
  ;; 2. Initialize Python with the manim environment:
  (init-python!)

  ;; 3. Import manim:
  (import-manim!)

  ;; 4. To render a scene, you would typically use manim's CLI
  ;; But you can also interact with manim objects directly:
  (let [circle (Circle)]
    (py/call-attr circle "set_fill" PINK :opacity 0.5)
    circle)

  ;; For actual rendering, you'll need to:
  ;; - Create a Python file with the scene class
  ;; - Use manim's CLI to render it
  ;; Or use manim's config and rendering API directly from Clojure
  )
