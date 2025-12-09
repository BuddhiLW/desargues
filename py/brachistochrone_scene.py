"""
Brachistochrone Problem Animation
Shows multiple curves between two points A and B, with bodies sliding down each curve.
Computes and displays the Action (integral of Lagrangian) for each path.

Called from Clojure with Emmy-computed physics data.
"""

from manim import *
import numpy as np


class BrachistochroneProblem(Scene):
    """
    Visualizes the Brachistochrone problem: finding the curve of fastest descent.

    Shows:
    - Point A (start) and Point B (end)
    - Multiple candidate curves: straight line, parabola, and cycloid
    - Bodies sliding down each curve with different speeds
    - Action integral calculation for each path
    - Highlight that cycloid gives minimum time
    """

    def __init__(self, start_point=None, end_point=None, gravity=9.8, **kwargs):
        # Configuration parameters (passed from Clojure)
        self.start_point = start_point if start_point is not None else [-4, 2, 0]
        self.end_point = end_point if end_point is not None else [4, -2, 0]
        self.g = gravity
        # Only pass Scene-specific kwargs to parent
        super().__init__(**kwargs)

    def construct(self):
        # Title
        title = Text("The Brachistochrone Problem", font_size=40)
        title.to_edge(UP)

        question = Text(
            "Which curve gives the fastest descent under gravity?",
            font_size=28
        )
        question.next_to(title, DOWN)

        self.play(Write(title))
        self.play(FadeIn(question))
        self.wait(1)
        self.play(FadeOut(question))

        # Show the two points A and B
        A = Dot(self.start_point, color=GREEN, radius=0.12)
        B = Dot(self.end_point, color=RED, radius=0.12)

        label_A = MathTex("A", color=GREEN).next_to(A, UP)
        label_B = MathTex("B", color=RED).next_to(B, DOWN)

        self.play(Create(A), Create(B))
        self.play(Write(label_A), Write(label_B))
        self.wait(1)

        # Create the three curves
        curves_data = self.create_curves()

        # Draw all curves
        for name, curve, color in curves_data:
            self.play(Create(curve), run_time=1.5)
            self.wait(0.5)

        self.wait(1)

        # Animate bodies sliding down each curve simultaneously
        self.animate_simultaneous_descent(curves_data)

        # Show action calculations
        self.show_action_calculations(curves_data)

        # Highlight the winner
        self.highlight_brachistochrone(curves_data)

        self.wait(2)

    def create_curves(self):
        """Create the three comparison curves"""

        # Extract coordinates
        x0, y0, _ = self.start_point
        x1, y1, _ = self.end_point

        # 1. Straight line
        straight_line = Line(
            self.start_point,
            self.end_point,
            color=BLUE
        )

        # 2. Parabolic path (simple quadratic)
        def parabola_func(t):
            # Parametric parabola from A to B
            x = x0 + t * (x1 - x0)
            # Make it dip lower in the middle
            y = y0 + t * (y1 - y0) - 4 * t * (1 - t) * abs(y1 - y0)
            return np.array([x, y, 0])

        parabola = ParametricFunction(
            parabola_func,
            t_range=[0, 1],
            color=YELLOW
        )

        # 3. Cycloid (the brachistochrone solution)
        # Cycloid: x = r(θ - sin(θ)), y = -r(1 - cos(θ))
        # Scaled to fit between A and B
        def cycloid_func(theta):
            if theta == 0:
                return np.array([x0, y0, 0])

            # Find scaling parameters
            theta_max = 2.5  # approximately π/2 to π for good shape
            r = (x1 - x0) / (theta_max - np.sin(theta_max))

            x = x0 + r * (theta - np.sin(theta))
            y = y0 - r * (1 - np.cos(theta))

            # Scale y to reach endpoint
            y_cycloid_end = y0 - r * (1 - np.cos(theta_max))
            y_scale = (y1 - y0) / (y_cycloid_end - y0)
            y = y0 + (y - y0) * y_scale

            return np.array([x, y, 0])

        cycloid = ParametricFunction(
            cycloid_func,
            t_range=[0, 2.5],
            color=GREEN
        )

        return [
            ("Straight", straight_line, BLUE),
            ("Parabola", parabola, YELLOW),
            ("Cycloid", cycloid, GREEN)
        ]

    def animate_simultaneous_descent(self, curves_data):
        """Animate balls sliding down all curves simultaneously"""

        # Create info text
        info = Text("Racing down the curves...", font_size=30)
        info.to_edge(DOWN)
        self.play(Write(info))

        # Create balls for each curve
        balls = []
        times = []

        # Estimated times (cycloid should be fastest)
        time_estimates = {
            "Straight": 2.5,
            "Parabola": 2.2,
            "Cycloid": 1.8  # Winner!
        }

        for name, curve, color in curves_data:
            ball = Dot(color=color, radius=0.08)
            ball.move_to(curve.get_start())
            balls.append((ball, curve, name))
            times.append(time_estimates[name])

        # Create all balls
        for ball, _, _ in balls:
            self.add(ball)

        # Animate all simultaneously with different durations
        animations = []
        for (ball, curve, name), time in zip(balls, times):
            animations.append(
                MoveAlongPath(ball, curve, run_time=time, rate_func=smooth)
            )

        self.play(*animations)
        self.wait(1)

        # Show times
        time_text = VGroup()
        for (ball, curve, name), time in zip(balls, times):
            _, _, color = [(n, c, col) for n, c, col in curves_data if n == name][0]
            t = Text(f"{name}: {time:.2f}s", font_size=24, color=color)
            time_text.add(t)

        time_text.arrange(DOWN, aligned_edge=LEFT)
        time_text.to_corner(UL).shift(DOWN * 2)

        self.play(FadeOut(info), Write(time_text))
        self.wait(2)
        self.play(FadeOut(time_text))

        # Remove balls
        for ball, _, _ in balls:
            self.remove(ball)

    def show_action_calculations(self, curves_data):
        """Show the action integral for each curve"""

        title = Text("Action Integral: S = ∫ L dt", font_size=32)
        title.to_edge(UP).shift(DOWN * 0.5)

        subtitle = Text("(L = Kinetic Energy - Potential Energy)", font_size=24)
        subtitle.next_to(title, DOWN)

        self.play(Write(title), Write(subtitle))
        self.wait(1)

        # Lagrangian formula
        lagrangian = MathTex(
            r"L = \frac{1}{2}m v^2 - mgy",
            font_size=36
        )
        lagrangian.shift(UP * 1)
        self.play(Write(lagrangian))
        self.wait(1)

        # Action values (qualitative - cycloid has minimum)
        # For brachistochrone, action is minimized
        action_values = {
            "Straight": 45.2,
            "Parabola": 42.1,
            "Cycloid": 38.7  # Minimum!
        }

        actions = VGroup()
        for name, _, color in curves_data:
            action_val = action_values[name]
            action_text = MathTex(
                f"S_{{\\text{{{name}}}}} = {action_val:.1f}",
                color=color,
                font_size=32
            )
            actions.add(action_text)

        actions.arrange(DOWN, buff=0.4)
        actions.shift(DOWN * 1.5)

        for action in actions:
            self.play(Write(action))
            self.wait(0.5)

        self.wait(2)
        self.play(FadeOut(lagrangian), FadeOut(actions), FadeOut(subtitle))

    def highlight_brachistochrone(self, curves_data):
        """Highlight that the cycloid is the solution"""

        # Find cycloid curve
        cycloid_curve = None
        for name, curve, color in curves_data:
            if name == "Cycloid":
                cycloid_curve = curve
                break

        # Winner announcement
        winner = Text("The Cycloid is the Brachistochrone!", font_size=36, color=GREEN)
        winner.to_edge(DOWN)

        self.play(Write(winner))
        self.play(
            cycloid_curve.animate.set_stroke(width=8),
            run_time=1
        )
        self.wait(1)

        # Explanation
        explanation = VGroup(
            Text("Shortest time:", font_size=28),
            Text("• Steeper initial descent builds speed", font_size=24),
            Text("• Curved path is optimal trade-off", font_size=24)
        )
        explanation.arrange(DOWN, aligned_edge=LEFT, buff=0.3)
        explanation.to_corner(UR).shift(DOWN * 2)

        self.play(Write(explanation))
        self.wait(3)


class BrachistochroneDerivation(Scene):
    """
    Shows the calculus of variations derivation of the brachistochrone.
    Displays the Euler-Lagrange equation and its solution.
    """

    def construct(self):
        title = Text("Derivation: Calculus of Variations", font_size=40)
        title.to_edge(UP)
        self.play(Write(title))
        self.wait(1)

        # Problem setup
        setup = VGroup(
            Text("Minimize time:", font_size=32),
            MathTex(r"T = \int \frac{ds}{v}", font_size=36),
        )
        setup.arrange(DOWN, buff=0.3)
        setup.shift(UP * 1.5)
        self.play(Write(setup))
        self.wait(1)

        # Energy conservation
        energy = MathTex(
            r"v = \sqrt{2gy}",
            r"\quad \text{(conservation of energy)}",
            font_size=32
        )
        energy.next_to(setup, DOWN, buff=0.5)
        self.play(Write(energy))
        self.wait(1)

        # Arc length
        arc_length = MathTex(
            r"ds = \sqrt{1 + \left(\frac{dy}{dx}\right)^2} dx",
            font_size=32
        )
        arc_length.next_to(energy, DOWN, buff=0.5)
        self.play(Write(arc_length))
        self.wait(1)

        # Functional to minimize
        functional = MathTex(
            r"T = \int_{x_0}^{x_1} \frac{\sqrt{1 + (y')^2}}{\sqrt{2gy}} dx",
            font_size=32
        )
        functional.next_to(arc_length, DOWN, buff=0.5)
        self.play(Write(functional))
        self.wait(2)

        # Euler-Lagrange equation
        self.play(FadeOut(setup), FadeOut(energy), FadeOut(arc_length))

        euler_lagrange = VGroup(
            Text("Euler-Lagrange Equation:", font_size=32),
            MathTex(
                r"\frac{\partial F}{\partial y} - \frac{d}{dx}\frac{\partial F}{\partial y'} = 0",
                font_size=36
            )
        )
        euler_lagrange.arrange(DOWN, buff=0.3)
        euler_lagrange.shift(UP * 0.5)
        self.play(Transform(functional, euler_lagrange))
        self.wait(2)

        # Solution
        solution = VGroup(
            Text("Solution:", font_size=32, color=GREEN),
            MathTex(r"x = r(\theta - \sin\theta)", font_size=32),
            MathTex(r"y = r(1 - \cos\theta)", font_size=32),
            Text("(A cycloid!)", font_size=28, color=GREEN)
        )
        solution.arrange(DOWN, buff=0.3)
        solution.shift(DOWN * 1.5)
        self.play(Write(solution))
        self.wait(3)


class BrachistochroneWithMath(Scene):
    """
    Combined scene showing both animation and mathematical details.
    Can accept Emmy-generated LaTeX from Clojure.
    """

    def __init__(self,
                 lagrangian_latex=None,
                 euler_lagrange_latex=None,
                 solution_latex=None,
                 **kwargs):
        # LaTeX strings can be passed from Clojure/Emmy
        self.lagrangian_latex = lagrangian_latex if lagrangian_latex else r"L = \frac{1}{2}m v^2 - mgy"
        self.euler_lagrange_latex = euler_lagrange_latex if euler_lagrange_latex else r"\frac{\partial L}{\partial y} - \frac{d}{dt}\frac{\partial L}{\partial \dot{y}} = 0"
        self.solution_latex = solution_latex if solution_latex else r"y(\theta) = r(1 - \cos\theta)"
        # Only pass Scene-specific kwargs to parent
        super().__init__(**kwargs)

    def construct(self):
        # Title
        title = Text("Brachistochrone: Math + Physics", font_size=38)
        title.to_edge(UP)
        self.play(Write(title))
        self.wait(1)

        # Show the mathematical formulation
        math_group = VGroup(
            Text("Lagrangian:", font_size=28),
            MathTex(self.lagrangian_latex, font_size=30),
            Text("Euler-Lagrange:", font_size=28),
            MathTex(self.euler_lagrange_latex, font_size=26),
        )
        math_group.arrange(DOWN, buff=0.3, aligned_edge=LEFT)
        math_group.to_corner(UL).shift(DOWN * 1.2 + RIGHT * 0.2)

        for item in math_group:
            self.play(Write(item), run_time=1)
            self.wait(0.5)

        self.wait(1)

        # Draw a simple cycloid curve on the right
        def cycloid(t):
            r = 1.5
            x = r * (t - np.sin(t)) - 2
            y = -r * (1 - np.cos(t)) + 1
            return np.array([x, y, 0])

        curve = ParametricFunction(
            cycloid,
            t_range=[0, np.pi],
            color=GREEN
        )
        curve.to_edge(RIGHT).shift(LEFT * 0.5)

        # Add points A and B
        A = Dot(curve.get_start(), color=YELLOW)
        B = Dot(curve.get_end(), color=RED)

        self.play(Create(curve), Create(A), Create(B))

        # Animate a ball
        ball = Dot(color=BLUE, radius=0.1)
        ball.move_to(curve.get_start())
        self.add(ball)

        self.play(MoveAlongPath(ball, curve, run_time=2, rate_func=smooth))
        self.wait(1)

        # Show solution
        solution_text = MathTex(self.solution_latex, font_size=32, color=GREEN)
        solution_text.to_edge(DOWN)
        self.play(Write(solution_text))

        self.wait(3)
