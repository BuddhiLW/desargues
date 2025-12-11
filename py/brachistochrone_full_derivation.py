"""
Brachistochrone Problem: Full Derivation and Animation

Complete step-by-step derivation using:
1. T = ∫dt (minimize time)
2. dt = ds/v (time element)
3. v = √(2gy) (energy conservation)
4. ds = √(1+y'²)dx (arc length)
5. Beltrami identity: L - y'∂L/∂y' = C
6. Solution: y(1+y'²) = 2A → Cycloid!

Then shows the race animation.
"""

from manim import *
import numpy as np


# =============================================================================
# Color Scheme
# =============================================================================

STRAIGHT_COLOR = BLUE
PARABOLA_COLOR = YELLOW
CYCLOID_COLOR = GREEN
HIGHLIGHT_COLOR = TEAL
STEP_COLOR = YELLOW


# =============================================================================
# Curve Functions
# =============================================================================

def straight_line(t, x0=-4, y0=2, x1=4, y1=-2):
    x = x0 + t * (x1 - x0)
    y = y0 + t * (y1 - y0)
    return np.array([x, y, 0])


def parabolic_path(t, x0=-4, y0=2, x1=4, y1=-2, depth=2.0):
    x = x0 + t * (x1 - x0)
    y = y0 + t * (y1 - y0) - depth * t * (1 - t) * abs(y1 - y0)
    return np.array([x, y, 0])


def cycloid_path(theta, x0=-4, y0=2, x1=4, y1=-2):
    if theta == 0:
        return np.array([x0, y0, 0])
    theta_max = 2.5
    r = (x1 - x0) / (theta_max - np.sin(theta_max))
    x = x0 + r * (theta - np.sin(theta))
    y_raw = y0 - r * (1 - np.cos(theta))
    y_end = y0 - r * (1 - np.cos(theta_max))
    y_scale = (y1 - y0) / (y_end - y0)
    y = y0 + (y_raw - y0) * y_scale
    return np.array([x, y, 0])


def descent_time(curve_func, t_start, t_end, n_points=100, g=9.8):
    """Calculate descent time using energy conservation."""
    dt = (t_end - t_start) / n_points
    total_time = 0
    for i in range(n_points):
        t1 = t_start + i * dt
        t2 = t_start + (i + 1) * dt
        p1 = curve_func(t1)
        p2 = curve_func(t2)
        dx = p2[0] - p1[0]
        dy = p2[1] - p1[1]
        ds = np.sqrt(dx**2 + dy**2)
        y_avg = (p1[1] + p2[1]) / 2
        v = np.sqrt(2 * g * abs(y_avg)) if y_avg != 0 else 0.001
        if v > 0:
            total_time += ds / v
    return total_time


# =============================================================================
# Scene: Full Derivation
# =============================================================================

class BrachistochroneFullDerivation(Scene):
    """Complete brachistochrone derivation with all algebraic steps."""

    def construct(self):
        # Run all scenes in sequence
        self.intro_scene()
        self.step1_problem()
        self.step2_time_element()
        self.step3_energy_conservation()
        self.step4_arc_length()
        self.step5_functional()
        self.step6_lagrangian()
        self.step7_beltrami()
        self.step8_apply_beltrami()
        self.step9_key_equation()
        self.step10_solve_ode()
        self.step11_integration()
        self.step12_cycloid()
        self.cycloid_explanation()
        self.race_scene()
        self.tautochrone_bonus()

    def intro_scene(self):
        """Introduction to the problem."""
        title = Text("The Brachistochrone Problem", font_size=48)
        title.to_edge(UP)

        subtitle = MathTex(
            r"\beta\rho\alpha\chi\iota\sigma\tau o\varsigma \, \chi\rho o\nu o\varsigma",
            r"= \text{shortest time}",
            font_size=32
        )
        subtitle.next_to(title, DOWN)

        self.play(Write(title))
        self.play(FadeIn(subtitle))
        self.wait(1)
        self.play(FadeOut(subtitle))

        # Points A and B
        A = Dot([-4, 2, 0], color=GREEN, radius=0.12)
        B = Dot([4, -2, 0], color=RED, radius=0.12)
        label_A = MathTex("A", color=GREEN).next_to(A, UP)
        label_B = MathTex("B", color=RED).next_to(B, DOWN)

        self.play(Create(A), Create(B))
        self.play(Write(label_A), Write(label_B))

        # Gravity arrow
        g_arrow = Arrow([-5.5, 1, 0], [-5.5, -0.5, 0], color=YELLOW, stroke_width=4)
        g_label = MathTex("g", color=YELLOW, font_size=36).next_to(g_arrow, LEFT)
        self.play(GrowArrow(g_arrow), Write(g_label))

        # Draw curves
        straight = ParametricFunction(straight_line, t_range=[0, 1], color=STRAIGHT_COLOR, stroke_width=3)
        parabola = ParametricFunction(parabolic_path, t_range=[0, 1], color=PARABOLA_COLOR, stroke_width=3)
        cycloid = ParametricFunction(cycloid_path, t_range=[0, 2.5], color=CYCLOID_COLOR, stroke_width=3)

        self.play(Create(straight), run_time=1)
        self.play(Create(parabola), run_time=1)
        self.play(Create(cycloid), run_time=1)

        # Question
        question = Text("Which curve minimizes the descent time?", font_size=28)
        question.shift(DOWN * 2.5)
        self.play(Write(question))
        self.wait(2)

        # Store for later
        self.curves = {"straight": straight, "parabola": parabola, "cycloid": cycloid}
        self.points = {"A": A, "B": B, "label_A": label_A, "label_B": label_B}
        self.gravity = {"arrow": g_arrow, "label": g_label}

        # Clear for derivation
        self.play(
            FadeOut(title), FadeOut(question),
            FadeOut(straight), FadeOut(parabola), FadeOut(cycloid),
            FadeOut(A), FadeOut(B), FadeOut(label_A), FadeOut(label_B),
            FadeOut(g_arrow), FadeOut(g_label)
        )

    def step1_problem(self):
        """Step 1: Set up minimization problem."""
        title = Text("Step 1: The Minimization Problem", font_size=36, color=STEP_COLOR)
        title.to_edge(UP)

        eq = MathTex(r"\text{Minimize } T = \int_{A}^{B} dt", font_size=42)
        eq.shift(UP * 0.5)

        explanation = Text(
            "Find the curve y(x) that minimizes total travel time",
            font_size=26
        )
        explanation.shift(DOWN * 1)

        self.play(Write(title))
        self.play(Write(eq))
        self.wait(1)
        self.play(FadeIn(explanation))
        self.wait(2)
        self.play(FadeOut(title), FadeOut(eq), FadeOut(explanation))

    def step2_time_element(self):
        """Step 2: Express dt."""
        title = Text("Step 2: Time Element", font_size=36, color=STEP_COLOR)
        title.to_edge(UP)

        eq = MathTex(r"dt = \frac{ds}{v}", font_size=48)
        eq.shift(UP * 0.5)

        note = Text("(time = distance / velocity)", font_size=28)
        note.next_to(eq, DOWN, buff=0.5)

        self.play(Write(title))
        self.play(Write(eq))
        self.play(FadeIn(note))
        self.wait(2)
        self.play(FadeOut(title), FadeOut(eq), FadeOut(note))

    def step3_energy_conservation(self):
        """Step 3: Energy conservation gives v = √(2gy)."""
        title = Text("Step 3: Energy Conservation", font_size=36, color=STEP_COLOR)
        title.to_edge(UP)

        note = Text("Particle starts from rest, falls to height y", font_size=24)
        note.shift(UP * 2)

        eq1 = MathTex(r"\frac{1}{2}mv^2 = mgy", font_size=36)
        eq1.shift(UP * 0.8)

        eq2 = MathTex(r"v^2 = 2gy", font_size=36)
        eq2.shift(ORIGIN)

        eq3 = MathTex(r"v = \sqrt{2gy}", font_size=48, color=HIGHLIGHT_COLOR)
        eq3.shift(DOWN * 1.2)

        box = SurroundingRectangle(eq3, color=HIGHLIGHT_COLOR, buff=0.2)

        self.play(Write(title))
        self.play(FadeIn(note))
        self.play(Write(eq1))
        self.wait(1)
        self.play(TransformMatchingTex(eq1.copy(), eq2))
        self.wait(1)
        self.play(TransformMatchingTex(eq2.copy(), eq3))
        self.play(Create(box))
        self.wait(2)
        self.play(FadeOut(title), FadeOut(note), FadeOut(eq1), FadeOut(eq2), FadeOut(eq3), FadeOut(box))

    def step4_arc_length(self):
        """Step 4: Arc length ds = √(1+y'²)dx."""
        title = Text("Step 4: Arc Length Element", font_size=36, color=STEP_COLOR)
        title.to_edge(UP)

        eq1 = MathTex(r"ds^2 = dx^2 + dy^2", font_size=36)
        eq1.shift(UP * 1)

        eq2 = MathTex(r"ds^2 = dx^2\left(1 + \frac{dy^2}{dx^2}\right)", font_size=36)
        eq2.shift(ORIGIN)

        eq3 = MathTex(r"ds = \sqrt{1 + y'^2} \, dx", font_size=48, color=HIGHLIGHT_COLOR)
        eq3.shift(DOWN * 1.2)

        note = MathTex(r"\text{where } y' = \frac{dy}{dx}", font_size=28)
        note.shift(DOWN * 2.2)

        box = SurroundingRectangle(eq3, color=HIGHLIGHT_COLOR, buff=0.2)

        self.play(Write(title))
        self.play(Write(eq1))
        self.wait(1)
        self.play(TransformMatchingTex(eq1.copy(), eq2))
        self.wait(1)
        self.play(TransformMatchingTex(eq2.copy(), eq3))
        self.play(FadeIn(note))
        self.play(Create(box))
        self.wait(2)
        self.play(FadeOut(title), FadeOut(eq1), FadeOut(eq2), FadeOut(eq3), FadeOut(note), FadeOut(box))

    def step5_functional(self):
        """Step 5: Combine into the functional."""
        title = Text("Step 5: The Functional", font_size=36, color=STEP_COLOR)
        title.to_edge(UP)

        recall1 = MathTex(r"v = \sqrt{2gy}", font_size=28)
        recall1.to_corner(UL).shift(DOWN * 1.2)
        recall2 = MathTex(r"ds = \sqrt{1+y'^2}\,dx", font_size=28)
        recall2.to_corner(UR).shift(DOWN * 1.2)

        eq1 = MathTex(r"dt = \frac{ds}{v}", font_size=36)
        eq1.shift(UP * 0.3)

        eq2 = MathTex(r"dt = \frac{\sqrt{1 + y'^2} \, dx}{\sqrt{2gy}}", font_size=36)
        eq2.shift(DOWN * 0.5)

        eq3 = MathTex(r"T = \int_{x_0}^{x_1} \frac{\sqrt{1 + y'^2}}{\sqrt{2gy}} \, dx", font_size=42, color=HIGHLIGHT_COLOR)
        eq3.shift(DOWN * 1.8)

        box = SurroundingRectangle(eq3, color=HIGHLIGHT_COLOR, buff=0.2)

        self.play(Write(title))
        self.play(FadeIn(recall1), FadeIn(recall2))
        self.play(Write(eq1))
        self.wait(1)
        self.play(TransformMatchingTex(eq1.copy(), eq2))
        self.wait(1)
        self.play(Write(eq3))
        self.play(Create(box))
        self.wait(2)
        self.play(FadeOut(title), FadeOut(recall1), FadeOut(recall2),
                  FadeOut(eq1), FadeOut(eq2), FadeOut(eq3), FadeOut(box))

    def step6_lagrangian(self):
        """Step 6: Identify Lagrangian."""
        title = Text("Step 6: The Lagrangian", font_size=36, color=STEP_COLOR)
        title.to_edge(UP)

        eq1 = MathTex(r"T = \int L(y, y') \, dx", font_size=36)
        eq1.shift(UP * 0.8)

        eq2 = MathTex(r"L(y, y') = \frac{\sqrt{1 + y'^2}}{\sqrt{2gy}}", font_size=42, color=HIGHLIGHT_COLOR)
        eq2.shift(DOWN * 0.2)

        box = SurroundingRectangle(eq2, color=HIGHLIGHT_COLOR, buff=0.2)

        note = Text("Key: L depends on y and y', but NOT explicitly on x!", font_size=26, color=TEAL)
        note.shift(DOWN * 1.8)

        self.play(Write(title))
        self.play(Write(eq1))
        self.wait(1)
        self.play(Write(eq2))
        self.play(Create(box))
        self.wait(1)
        self.play(Write(note))
        self.wait(2)
        self.play(FadeOut(title), FadeOut(eq1), FadeOut(eq2), FadeOut(box), FadeOut(note))

    def step7_beltrami(self):
        """Step 7: Euler-Lagrange → Beltrami Identity."""
        title = Text("Step 7: Beltrami Identity", font_size=36, color=STEP_COLOR)
        title.to_edge(UP)

        eq_el = MathTex(r"\frac{\partial L}{\partial y} - \frac{d}{dx}\frac{\partial L}{\partial y'} = 0", font_size=32)
        eq_el.shift(UP * 1.5)
        el_label = Text("Euler-Lagrange equation", font_size=22)
        el_label.next_to(eq_el, RIGHT, buff=0.3)

        condition = MathTex(r"\frac{\partial L}{\partial x} = 0", font_size=32)
        condition.shift(UP * 0.3)
        cond_note = Text("(no explicit x dependence)", font_size=22)
        cond_note.next_to(condition, RIGHT, buff=0.3)

        beltrami = MathTex(r"L - y' \frac{\partial L}{\partial y'} = C", font_size=48, color=HIGHLIGHT_COLOR)
        beltrami.shift(DOWN * 1)

        beltrami_label = Text("(Beltrami Identity)", font_size=28, color=HIGHLIGHT_COLOR)
        beltrami_label.next_to(beltrami, DOWN, buff=0.3)

        box = SurroundingRectangle(beltrami, color=HIGHLIGHT_COLOR, buff=0.25)

        self.play(Write(title))
        self.play(Write(eq_el), FadeIn(el_label))
        self.wait(1)
        self.play(Write(condition), FadeIn(cond_note))
        self.wait(1)
        self.play(Write(beltrami))
        self.play(Create(box), Write(beltrami_label))
        self.wait(2)
        self.play(FadeOut(title), FadeOut(eq_el), FadeOut(el_label),
                  FadeOut(condition), FadeOut(cond_note),
                  FadeOut(beltrami), FadeOut(beltrami_label), FadeOut(box))

    def step8_apply_beltrami(self):
        """Step 8: Apply Beltrami Identity."""
        title = Text("Step 8: Apply Beltrami Identity", font_size=36, color=STEP_COLOR)
        title.to_edge(UP)

        recall = MathTex(r"L = \sqrt{\frac{1+y'^2}{y}}", font_size=28)
        recall.to_corner(UL).shift(DOWN * 1)

        eq1 = MathTex(r"\frac{\partial L}{\partial y'} = \frac{y'}{\sqrt{y(1+y'^2)}}", font_size=32)
        eq1.shift(UP * 1)

        eq2 = MathTex(r"\sqrt{\frac{1+y'^2}{y}} - y' \cdot \frac{y'}{\sqrt{y(1+y'^2)}} = C", font_size=28)
        eq2.shift(UP * 0.1)

        eq3 = MathTex(r"\frac{1+y'^2 - y'^2}{\sqrt{y(1+y'^2)}} = C", font_size=32)
        eq3.shift(DOWN * 0.8)

        eq4 = MathTex(r"\frac{1}{\sqrt{y(1+y'^2)}} = C", font_size=36, color=HIGHLIGHT_COLOR)
        eq4.shift(DOWN * 1.8)

        self.play(Write(title))
        self.play(FadeIn(recall))
        self.play(Write(eq1))
        self.wait(1)
        self.play(Write(eq2))
        self.wait(1)
        self.play(TransformMatchingTex(eq2.copy(), eq3))
        self.wait(1)
        self.play(TransformMatchingTex(eq3.copy(), eq4))
        self.wait(2)
        self.play(FadeOut(title), FadeOut(recall), FadeOut(eq1),
                  FadeOut(eq2), FadeOut(eq3), FadeOut(eq4))

    def step9_key_equation(self):
        """Step 9: Key equation y(1+y'²) = const."""
        title = Text("Step 9: The Key Equation", font_size=36, color=STEP_COLOR)
        title.to_edge(UP)

        eq1 = MathTex(r"\sqrt{y(1+y'^2)} = \frac{1}{C}", font_size=36)
        eq1.shift(UP * 0.8)

        eq2 = MathTex(r"y(1+y'^2) = \frac{1}{C^2}", font_size=42)
        eq2.shift(DOWN * 0.2)

        eq3 = MathTex(r"y(1+y'^2) = 2A", font_size=48, color=HIGHLIGHT_COLOR)
        eq3.shift(DOWN * 1.5)

        note = MathTex(r"\text{where } 2A = \frac{1}{C^2}", font_size=28)
        note.next_to(eq3, DOWN, buff=0.3)

        box = SurroundingRectangle(eq3, color=HIGHLIGHT_COLOR, buff=0.25)

        self.play(Write(title))
        self.play(Write(eq1))
        self.wait(1)
        self.play(TransformMatchingTex(eq1.copy(), eq2))
        self.wait(1)
        self.play(TransformMatchingTex(eq2.copy(), eq3))
        self.play(Create(box), FadeIn(note))
        self.wait(2)
        self.play(FadeOut(title), FadeOut(eq1), FadeOut(eq2), FadeOut(eq3), FadeOut(note), FadeOut(box))

    def step10_solve_ode(self):
        """Step 10: Solve the ODE with substitution."""
        title = Text("Step 10: Solve the Differential Equation", font_size=36, color=STEP_COLOR)
        title.to_edge(UP)

        eq1 = MathTex(r"y' = \frac{dy}{dx} = \sqrt{\frac{2A - y}{y}}", font_size=32)
        eq1.shift(UP * 1.3)

        eq2 = MathTex(r"\sqrt{\frac{y}{2A-y}} \, dy = dx", font_size=32)
        eq2.shift(UP * 0.4)

        sub_label = Text("Use substitution:", font_size=28)
        sub_label.shift(DOWN * 0.3)

        eq3 = MathTex(r"y = A(1 - \cos\phi)", font_size=32, color=TEAL)
        eq3.shift(DOWN * 1)

        eq4 = MathTex(r"dy = A\sin\phi \, d\phi", font_size=32)
        eq4.shift(DOWN * 1.7)

        self.play(Write(title))
        self.play(Write(eq1))
        self.wait(1)
        self.play(Write(eq2))
        self.wait(1)
        self.play(FadeIn(sub_label))
        self.play(Write(eq3))
        self.play(Write(eq4))
        self.wait(2)
        self.play(FadeOut(title), FadeOut(eq1), FadeOut(eq2),
                  FadeOut(sub_label), FadeOut(eq3), FadeOut(eq4))

    def step11_integration(self):
        """Step 11: Integration."""
        title = Text("Step 11: Integration", font_size=36, color=STEP_COLOR)
        title.to_edge(UP)

        eq1 = MathTex(r"\text{Using } \frac{1-\cos\phi}{1+\cos\phi} = \tan^2\frac{\phi}{2}", font_size=28)
        eq1.shift(UP * 1.2)

        eq2 = MathTex(r"A(1 - \cos\phi) \, d\phi = dx", font_size=36)
        eq2.shift(UP * 0.2)

        eq3 = MathTex(r"x = A(\phi - \sin\phi)", font_size=42, color=HIGHLIGHT_COLOR)
        eq3.shift(DOWN * 1)

        note = Text("(setting x₀ = 0)", font_size=24)
        note.next_to(eq3, DOWN, buff=0.3)

        self.play(Write(title))
        self.play(Write(eq1))
        self.wait(1)
        self.play(Write(eq2))
        self.wait(1)
        self.play(Write(eq3))
        self.play(FadeIn(note))
        self.wait(2)
        self.play(FadeOut(title), FadeOut(eq1), FadeOut(eq2), FadeOut(eq3), FadeOut(note))

    def step12_cycloid(self):
        """Step 12: Final Result - The Cycloid!"""
        title = Text("Step 12: The Solution", font_size=36, color=STEP_COLOR)
        title.to_edge(UP)

        result_label = Text("The curve that minimizes descent time:", font_size=28)
        result_label.shift(UP * 1.5)

        eq_x = MathTex(r"x = A(\phi - \sin\phi)", font_size=48)
        eq_x.shift(UP * 0.3)

        eq_y = MathTex(r"y = A(1 - \cos\phi)", font_size=48)
        eq_y.shift(DOWN * 0.7)

        result_name = Text("The Cycloid!", font_size=56, color=CYCLOID_COLOR)
        result_name.shift(DOWN * 2)

        box = SurroundingRectangle(VGroup(eq_x, eq_y), color=CYCLOID_COLOR, buff=0.3, stroke_width=4)

        self.play(Write(title))
        self.play(FadeIn(result_label))
        self.wait(0.5)
        self.play(Write(eq_x))
        self.play(Write(eq_y))
        self.play(Create(box))
        self.wait(1)
        self.play(Write(result_name))
        self.wait(3)
        self.play(FadeOut(title), FadeOut(result_label), FadeOut(eq_x), FadeOut(eq_y),
                  FadeOut(result_name), FadeOut(box))

    def cycloid_explanation(self):
        """Explain what a cycloid is."""
        title = Text("What is a Cycloid?", font_size=40)
        title.to_edge(UP)

        explanation = Text("The path traced by a point on a rolling circle", font_size=28)
        explanation.shift(UP * 1.5)

        # Draw a cycloid
        def cycloid_vis(t):
            r = 0.8
            x = r * (t - np.sin(t)) - 2
            y = -r * (1 - np.cos(t))
            return np.array([x, y, 0])

        cycloid_curve = ParametricFunction(cycloid_vis, t_range=[0, 2*PI], color=CYCLOID_COLOR, stroke_width=4)

        # Rolling circle animation
        circle = Circle(radius=0.8, color=WHITE)
        circle.move_to([-2, 0.8, 0])
        dot_on_circle = Dot([-2, 0, 0], color=RED, radius=0.1)

        self.play(Write(title))
        self.play(FadeIn(explanation))
        self.play(Create(cycloid_curve))
        self.wait(2)
        self.play(FadeOut(title), FadeOut(explanation), FadeOut(cycloid_curve))

    def race_scene(self):
        """Animate the race!"""
        title = Text("The Race!", font_size=40)
        title.to_edge(UP)

        # Recreate curves
        straight = ParametricFunction(straight_line, t_range=[0, 1], color=STRAIGHT_COLOR, stroke_width=3)
        parabola = ParametricFunction(parabolic_path, t_range=[0, 1], color=PARABOLA_COLOR, stroke_width=3)
        cycloid = ParametricFunction(cycloid_path, t_range=[0, 2.5], color=CYCLOID_COLOR, stroke_width=3)

        # Points
        A = Dot([-4, 2, 0], color=GREEN, radius=0.1)
        B = Dot([4, -2, 0], color=RED, radius=0.1)

        # Balls
        ball_s = Dot(color=STRAIGHT_COLOR, radius=0.12)
        ball_p = Dot(color=PARABOLA_COLOR, radius=0.12)
        ball_c = Dot(color=CYCLOID_COLOR, radius=0.12)

        ball_s.move_to(straight.get_start())
        ball_p.move_to(parabola.get_start())
        ball_c.move_to(cycloid.get_start())

        # Calculate times
        time_s = descent_time(straight_line, 0, 1) * 1.5
        time_p = descent_time(parabolic_path, 0, 1) * 1.5
        time_c = descent_time(cycloid_path, 0, 2.5) * 1.5

        self.play(Write(title))
        self.play(Create(A), Create(B))
        self.play(Create(straight), Create(parabola), Create(cycloid))
        self.add(ball_s, ball_p, ball_c)
        self.wait(0.5)

        # Race!
        self.play(
            MoveAlongPath(ball_s, straight, run_time=time_s, rate_func=linear),
            MoveAlongPath(ball_p, parabola, run_time=time_p, rate_func=linear),
            MoveAlongPath(ball_c, cycloid, run_time=time_c, rate_func=linear),
        )
        self.wait(1)

        # Show results
        results = VGroup(
            Text(f"Straight: {time_s/1.5:.3f}s", font_size=28, color=STRAIGHT_COLOR),
            Text(f"Parabola: {time_p/1.5:.3f}s", font_size=28, color=PARABOLA_COLOR),
            Text(f"Cycloid:  {time_c/1.5:.3f}s ← FASTEST!", font_size=28, color=CYCLOID_COLOR),
        )
        results.arrange(DOWN, buff=0.3, aligned_edge=LEFT)
        results.to_corner(UR).shift(DOWN * 1.5)

        self.play(Write(results))
        self.wait(3)

        self.play(FadeOut(title), FadeOut(A), FadeOut(B),
                  FadeOut(straight), FadeOut(parabola), FadeOut(cycloid),
                  FadeOut(ball_s), FadeOut(ball_p), FadeOut(ball_c),
                  FadeOut(results))

    def tautochrone_bonus(self):
        """Tautochrone property bonus."""
        title = Text("Bonus: The Tautochrone Property", font_size=36)
        title.to_edge(UP)

        fact = Text("Same curve, amazing property:", font_size=28)
        fact.shift(UP * 1.2)

        property_text = Text(
            "All starting points reach the bottom in EQUAL TIME!",
            font_size=32, color=CYCLOID_COLOR
        )
        property_text.shift(ORIGIN)

        explanation = VGroup(
            Text("• Balls released from ANY height on the cycloid", font_size=24),
            Text("• All arrive at the lowest point simultaneously", font_size=24),
            Text("• Used in Huygens' pendulum clocks (1673)", font_size=24),
        )
        explanation.arrange(DOWN, buff=0.3, aligned_edge=LEFT)
        explanation.shift(DOWN * 1.5)

        self.play(Write(title))
        self.play(FadeIn(fact))
        self.play(Write(property_text))
        self.wait(1)
        self.play(Write(explanation))
        self.wait(3)

        # Final message
        final = Text("The End!", font_size=48, color=CYCLOID_COLOR)
        self.play(
            FadeOut(title), FadeOut(fact), FadeOut(property_text), FadeOut(explanation),
            FadeIn(final)
        )
        self.wait(2)


# =============================================================================
# Quick test scene (shorter)
# =============================================================================

class BrachistochroneQuickDemo(Scene):
    """Shorter demo with just key steps and race."""

    def construct(self):
        # Title
        title = Text("The Brachistochrone Problem", font_size=48)
        self.play(Write(title))
        self.wait(1)
        self.play(FadeOut(title))

        # Key equation
        key = MathTex(r"T = \int \frac{\sqrt{1 + y'^2}}{\sqrt{2gy}} \, dx", font_size=42)
        self.play(Write(key))
        self.wait(1)

        # Beltrami
        beltrami = MathTex(r"L - y' \frac{\partial L}{\partial y'} = C", font_size=42, color=TEAL)
        beltrami.shift(DOWN)
        self.play(Write(beltrami))
        self.wait(1)

        # Solution
        solution = MathTex(r"y(1+y'^2) = 2A \implies \text{Cycloid!}", font_size=36, color=GREEN)
        solution.shift(DOWN * 2)
        self.play(Write(solution))
        self.wait(2)

        self.play(FadeOut(key), FadeOut(beltrami), FadeOut(solution))

        # Quick race
        straight = ParametricFunction(straight_line, t_range=[0, 1], color=BLUE, stroke_width=3)
        cycloid = ParametricFunction(cycloid_path, t_range=[0, 2.5], color=GREEN, stroke_width=3)

        ball_s = Dot(color=BLUE, radius=0.12).move_to(straight.get_start())
        ball_c = Dot(color=GREEN, radius=0.12).move_to(cycloid.get_start())

        self.play(Create(straight), Create(cycloid))
        self.add(ball_s, ball_c)

        time_s = descent_time(straight_line, 0, 1) * 1.5
        time_c = descent_time(cycloid_path, 0, 2.5) * 1.5

        self.play(
            MoveAlongPath(ball_s, straight, run_time=time_s, rate_func=linear),
            MoveAlongPath(ball_c, cycloid, run_time=time_c, rate_func=linear),
        )

        winner = Text("Cycloid wins!", font_size=48, color=GREEN)
        winner.to_edge(DOWN)
        self.play(Write(winner))
        self.wait(2)
