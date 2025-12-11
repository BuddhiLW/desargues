(ns desargues.manim.constants
  "Manim Constants - colors, directions, and mathematical constants.
   
   All constants are delay-wrapped for lazy initialization.
   Deref with @ to use: @RED, @UP, etc."
  (:require [desargues.manim.core :as core]))

;; ============================================================================
;; Helper Macro
;; ============================================================================

(defmacro ^:private defconst
  "Define a lazy constant from Manim."
  [name const-name & [docstring]]
  `(def ~(with-meta name {:doc (or docstring (str "Manim constant: " const-name))})
     (delay (core/get-constant ~const-name))))

(defmacro ^:private defconsts
  "Define multiple lazy constants."
  [& pairs]
  `(do
     ~@(for [[name const-name] (partition 2 pairs)]
         `(defconst ~name ~const-name))))

;; ============================================================================
;; Primary Colors
;; ============================================================================

(defconst WHITE "WHITE" "Pure white")
(defconst BLACK "BLACK" "Pure black")

(defconst RED "RED" "Standard red (alias for RED_C)")
(defconst RED_A "RED_A" "Lightest red")
(defconst RED_B "RED_B" "Light red")
(defconst RED_C "RED_C" "Medium red")
(defconst RED_D "RED_D" "Dark red")
(defconst RED_E "RED_E" "Darkest red")

(defconst BLUE "BLUE" "Standard blue (alias for BLUE_C)")
(defconst BLUE_A "BLUE_A" "Lightest blue")
(defconst BLUE_B "BLUE_B" "Light blue")
(defconst BLUE_C "BLUE_C" "Medium blue")
(defconst BLUE_D "BLUE_D" "Dark blue")
(defconst BLUE_E "BLUE_E" "Darkest blue")

(defconst GREEN "GREEN" "Standard green (alias for GREEN_C)")
(defconst GREEN_A "GREEN_A" "Lightest green")
(defconst GREEN_B "GREEN_B" "Light green")
(defconst GREEN_C "GREEN_C" "Medium green")
(defconst GREEN_D "GREEN_D" "Dark green")
(defconst GREEN_E "GREEN_E" "Darkest green")

(defconst YELLOW "YELLOW" "Standard yellow (alias for YELLOW_C)")
(defconst YELLOW_A "YELLOW_A" "Lightest yellow")
(defconst YELLOW_B "YELLOW_B" "Light yellow")
(defconst YELLOW_C "YELLOW_C" "Medium yellow")
(defconst YELLOW_D "YELLOW_D" "Dark yellow")
(defconst YELLOW_E "YELLOW_E" "Darkest yellow")

(defconst GOLD "GOLD" "Standard gold (alias for GOLD_C)")
(defconst GOLD_A "GOLD_A" "Lightest gold")
(defconst GOLD_B "GOLD_B" "Light gold")
(defconst GOLD_C "GOLD_C" "Medium gold")
(defconst GOLD_D "GOLD_D" "Dark gold")
(defconst GOLD_E "GOLD_E" "Darkest gold")

(defconst PURPLE "PURPLE" "Standard purple (alias for PURPLE_C)")
(defconst PURPLE_A "PURPLE_A" "Lightest purple")
(defconst PURPLE_B "PURPLE_B" "Light purple")
(defconst PURPLE_C "PURPLE_C" "Medium purple")
(defconst PURPLE_D "PURPLE_D" "Dark purple")
(defconst PURPLE_E "PURPLE_E" "Darkest purple")

(defconst MAROON "MAROON" "Standard maroon (alias for MAROON_C)")
(defconst MAROON_A "MAROON_A" "Lightest maroon")
(defconst MAROON_B "MAROON_B" "Light maroon")
(defconst MAROON_C "MAROON_C" "Medium maroon")
(defconst MAROON_D "MAROON_D" "Dark maroon")
(defconst MAROON_E "MAROON_E" "Darkest maroon")

(defconst TEAL "TEAL" "Standard teal (alias for TEAL_C)")
(defconst TEAL_A "TEAL_A" "Lightest teal")
(defconst TEAL_B "TEAL_B" "Light teal")
(defconst TEAL_C "TEAL_C" "Medium teal")
(defconst TEAL_D "TEAL_D" "Dark teal")
(defconst TEAL_E "TEAL_E" "Darkest teal")

;; ============================================================================
;; Gray Scale
;; ============================================================================

(defconst GRAY "GRAY" "Standard gray (alias for GRAY_C)")
(defconst GREY "GREY" "Standard grey (alias for GREY_C)")
(defconst GRAY_A "GRAY_A" "Lightest gray")
(defconst GRAY_B "GRAY_B" "Light gray")
(defconst GRAY_C "GRAY_C" "Medium gray")
(defconst GRAY_D "GRAY_D" "Dark gray")
(defconst GRAY_E "GRAY_E" "Darkest gray")
(defconst GREY_A "GREY_A" "Lightest grey")
(defconst GREY_B "GREY_B" "Light grey")
(defconst GREY_C "GREY_C" "Medium grey")
(defconst GREY_D "GREY_D" "Dark grey")
(defconst GREY_E "GREY_E" "Darkest grey")

(defconst LIGHTER_GRAY "LIGHTER_GRAY")
(defconst LIGHTER_GREY "LIGHTER_GREY")
(defconst LIGHT_GRAY "LIGHT_GRAY")
(defconst LIGHT_GREY "LIGHT_GREY")
(defconst DARK_GRAY "DARK_GRAY")
(defconst DARK_GREY "DARK_GREY")
(defconst DARKER_GRAY "DARKER_GRAY")
(defconst DARKER_GREY "DARKER_GREY")

;; ============================================================================
;; Other Colors
;; ============================================================================

(defconst ORANGE "ORANGE")
(defconst PINK "PINK")
(defconst LIGHT_PINK "LIGHT_PINK")
(defconst LIGHT_BROWN "LIGHT_BROWN")
(defconst DARK_BROWN "DARK_BROWN")
(defconst GRAY_BROWN "GRAY_BROWN")
(defconst GREY_BROWN "GREY_BROWN")
(defconst DARK_BLUE "DARK_BLUE")

;; Pure colors
(defconst PURE_RED "PURE_RED" "Pure RGB red #FF0000")
(defconst PURE_GREEN "PURE_GREEN" "Pure RGB green #00FF00")
(defconst PURE_BLUE "PURE_BLUE" "Pure RGB blue #0000FF")

;; Logo colors
(defconst LOGO_WHITE "LOGO_WHITE")
(defconst LOGO_GREEN "LOGO_GREEN")
(defconst LOGO_BLUE "LOGO_BLUE")
(defconst LOGO_RED "LOGO_RED")
(defconst LOGO_BLACK "LOGO_BLACK")

;; ============================================================================
;; Directions (3D vectors)
;; ============================================================================

(defconst UP "UP" "Unit vector pointing up [0, 1, 0]")
(defconst DOWN "DOWN" "Unit vector pointing down [0, -1, 0]")
(defconst LEFT "LEFT" "Unit vector pointing left [-1, 0, 0]")
(defconst RIGHT "RIGHT" "Unit vector pointing right [1, 0, 0]")
(defconst IN "IN" "Unit vector pointing into screen [0, 0, -1]")
(defconst OUT "OUT" "Unit vector pointing out of screen [0, 0, 1]")
(defconst ORIGIN "ORIGIN" "Origin point [0, 0, 0]")

;; Diagonal directions
(defconst UL "UL" "Upper-left direction [-1, 1, 0]")
(defconst UR "UR" "Upper-right direction [1, 1, 0]")
(defconst DL "DL" "Down-left direction [-1, -1, 0]")
(defconst DR "DR" "Down-right direction [1, -1, 0]")

;; Axes
(defconst X_AXIS "X_AXIS" "X-axis unit vector [1, 0, 0]")
(defconst Y_AXIS "Y_AXIS" "Y-axis unit vector [0, 1, 0]")
(defconst Z_AXIS "Z_AXIS" "Z-axis unit vector [0, 0, 1]")

;; ============================================================================
;; Mathematical Constants
;; ============================================================================

(defconst PI "PI" "Mathematical constant π ≈ 3.14159")
(defconst TAU "TAU" "Mathematical constant τ = 2π ≈ 6.28318")
(defconst DEGREES "DEGREES" "Conversion factor: degrees to radians (π/180)")

;; ============================================================================
;; Buffer Constants
;; ============================================================================

(defconst SMALL_BUFF "SMALL_BUFF" "Small buffer spacing (0.1)")
(defconst MED_SMALL_BUFF "MED_SMALL_BUFF" "Medium-small buffer (0.25)")
(defconst MED_LARGE_BUFF "MED_LARGE_BUFF" "Medium-large buffer (0.5)")
(defconst LARGE_BUFF "LARGE_BUFF" "Large buffer (1.0)")

(defconst DEFAULT_MOBJECT_TO_EDGE_BUFFER "DEFAULT_MOBJECT_TO_EDGE_BUFFER")
(defconst DEFAULT_MOBJECT_TO_MOBJECT_BUFFER "DEFAULT_MOBJECT_TO_MOBJECT_BUFFER")

;; ============================================================================
;; Font/Text Constants
;; ============================================================================

(defconst DEFAULT_FONT_SIZE "DEFAULT_FONT_SIZE")
(defconst DEFAULT_STROKE_WIDTH "DEFAULT_STROKE_WIDTH")

;; Font weights
(defconst NORMAL "NORMAL")
(defconst BOLD "BOLD")
(defconst ITALIC "ITALIC")
(defconst OBLIQUE "OBLIQUE")

;; Additional font weights  
(defconst THIN "THIN")
(defconst ULTRALIGHT "ULTRALIGHT")
(defconst LIGHT "LIGHT")
(defconst SEMILIGHT "SEMILIGHT")
(defconst BOOK "BOOK")
(defconst MEDIUM "MEDIUM")
(defconst SEMIBOLD "SEMIBOLD")
(defconst ULTRABOLD "ULTRABOLD")
(defconst HEAVY "HEAVY")
(defconst ULTRAHEAVY "ULTRAHEAVY")

;; ============================================================================
;; Animation Constants
;; ============================================================================

(defconst DEFAULT_WAIT_TIME "DEFAULT_WAIT_TIME")
(defconst DEFAULT_POINTWISE_FUNCTION_RUN_TIME "DEFAULT_POINTWISE_FUNCTION_RUN_TIME")

;; ============================================================================
;; Geometry Constants
;; ============================================================================

(defconst DEFAULT_DOT_RADIUS "DEFAULT_DOT_RADIUS")
(defconst DEFAULT_SMALL_DOT_RADIUS "DEFAULT_SMALL_DOT_RADIUS")
(defconst DEFAULT_DASH_LENGTH "DEFAULT_DASH_LENGTH")
(defconst DEFAULT_ARROW_TIP_LENGTH "DEFAULT_ARROW_TIP_LENGTH")

;; ============================================================================
;; Quality Settings
;; ============================================================================

(defconst DEFAULT_QUALITY "DEFAULT_QUALITY")

;; ============================================================================
;; Convenience Functions
;; ============================================================================

(defn color
  "Get a color constant by name (case-insensitive).
   
   Examples:
   (color :red)
   (color \"BLUE_A\")
   (color 'GREEN)"
  [name]
  (core/get-constant (clojure.string/upper-case (clojure.core/name name))))

(defn direction
  "Get a direction constant by name.
   
   Examples:
   (direction :up)
   (direction \"LEFT\")"
  [name]
  (core/get-constant (clojure.string/upper-case (clojure.core/name name))))

(defn degrees->radians
  "Convert degrees to radians."
  [deg]
  (* deg (/ Math/PI 180.0)))

(defn radians->degrees
  "Convert radians to degrees."
  [rad]
  (* rad (/ 180.0 Math/PI)))

;; ============================================================================
;; Color Manipulation
;; ============================================================================

(defn rgb
  "Create a color from RGB values (0-1 range)."
  [r g b]
  (let [ManimColor (core/get-class "ManimColor")]
    (ManimColor [r g b])))

(defn rgb255
  "Create a color from RGB values (0-255 range)."
  [r g b]
  (rgb (/ r 255.0) (/ g 255.0) (/ b 255.0)))

(defn hex-color
  "Create a color from a hex string.
   
   Examples:
   (hex-color \"#FF0000\")
   (hex-color \"FF0000\")"
  [hex-str]
  (let [ManimColor (core/get-class "ManimColor")
        hex (if (clojure.string/starts-with? hex-str "#")
              hex-str
              (str "#" hex-str))]
    (ManimColor hex)))

(defn interpolate-color
  "Interpolate between two colors.
   
   alpha - interpolation factor (0 = color1, 1 = color2)"
  [color1 color2 alpha]
  (let [interpolate-fn (core/get-constant "interpolate_color")]
    (interpolate-fn color1 color2 alpha)))

;; ============================================================================
;; Vector Operations
;; ============================================================================

(defn vec-add
  "Add two vectors."
  [v1 v2]
  (mapv + v1 v2))

(defn vec-sub
  "Subtract two vectors."
  [v1 v2]
  (mapv - v1 v2))

(defn vec-scale
  "Scale a vector by a scalar."
  [v scalar]
  (mapv #(* % scalar) v))

(defn vec-normalize
  "Normalize a vector to unit length."
  [v]
  (let [len (Math/sqrt (reduce + (map #(* % %) v)))]
    (if (zero? len)
      v
      (mapv #(/ % len) v))))

(defn rotate-vector
  "Rotate a 2D vector by an angle (radians)."
  [[x y] angle]
  (let [c (Math/cos angle)
        s (Math/sin angle)]
    [(- (* c x) (* s y))
     (+ (* s x) (* c y))]))

;; ============================================================================
;; Commonly Used Combinations
;; ============================================================================

(def UP+RIGHT "Diagonal direction up-right" (delay (vec-add [0 1 0] [1 0 0])))
(def UP+LEFT "Diagonal direction up-left" (delay (vec-add [0 1 0] [-1 0 0])))
(def DOWN+RIGHT "Diagonal direction down-right" (delay (vec-add [0 -1 0] [1 0 0])))
(def DOWN+LEFT "Diagonal direction down-left" (delay (vec-add [0 -1 0] [-1 0 0])))
