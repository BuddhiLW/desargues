"""
Manim scenes that display Emmy-generated LaTeX equations
These can be called from Clojure with Emmy expressions
"""

from manim import *


class FunctionAndDerivative(Scene):
    """
    Display a function and its derivative side by side

    Usage from Clojure:
        (let [f-latex (emmy->latex (sin 'x))
              df-latex (emmy->latex ((D sin) 'x))
              scene-class (create-function-derivative-scene f-latex df-latex)]
          (render-scene! (scene-class)))
    """
    def __init__(self, func_latex=None, deriv_latex=None, **kwargs):
        self.func_latex = func_latex or r"f(x) = \sin(x)"
        self.deriv_latex = deriv_latex or r"f'(x) = \cos(x)"
        super().__init__(**kwargs)

    def construct(self):
        # Title
        title = Text("Function and Derivative", font_size=48)
        title.to_edge(UP)

        # Function
        func_label = Text("Function:", font_size=36).shift(UP * 1)
        func_eq = MathTex(self.func_latex).next_to(func_label, DOWN)

        # Derivative
        deriv_label = Text("Derivative:", font_size=36).shift(DOWN * 0.5)
        deriv_eq = MathTex(self.deriv_latex).next_to(deriv_label, DOWN)

        # Animate
        self.play(Write(title))
        self.wait(0.5)
        self.play(Write(func_label), Write(func_eq))
        self.wait(1)
        self.play(Write(deriv_label), Write(deriv_eq))
        self.wait(2)


class EquationTransform(Scene):
    """Transform one equation to another"""
    def __init__(self, eq1_latex=None, eq2_latex=None, **kwargs):
        self.eq1_latex = eq1_latex or r"\sin^2(x) + \cos^2(x)"
        self.eq2_latex = eq2_latex or "1"
        super().__init__(**kwargs)

    def construct(self):
        eq1 = MathTex(self.eq1_latex)
        eq2 = MathTex(self.eq2_latex)

        self.play(Write(eq1))
        self.wait(1)
        self.play(Transform(eq1, eq2))
        self.wait(2)


class DerivativeSteps(Scene):
    """Show step-by-step derivative calculation"""
    def construct(self):
        # Original function
        title = Text("Derivative Calculation", font_size=40)
        title.to_edge(UP)

        steps = [
            r"f(x) = \sin^2(x + 3)",
            r"f(x) = (\sin(x + 3))^2",
            r"f'(x) = 2 \sin(x + 3) \cdot \cos(x + 3)",
        ]

        equations = [MathTex(s) for s in steps]

        self.play(Write(title))
        self.wait(0.5)

        current = equations[0]
        self.play(Write(current))
        self.wait(1)

        for next_eq in equations[1:]:
            self.play(Transform(current, next_eq))
            self.wait(1.5)

        self.wait(2)


class EmmyShowcase(Scene):
    """Showcase multiple Emmy-generated equations"""
    def construct(self):
        title = Text("Emmy Symbolic Math", font_size=48)
        title.to_edge(UP)
        self.play(Write(title))

        # Example equations (these can be generated from Emmy)
        equations = [
            r"f(x) = \sin(x + \pi)",
            r"f'(x) = \cos(x + \pi)",
            r"f''(x) = -\sin(x + \pi)",
        ]

        labels = ["Function:", "First Derivative:", "Second Derivative:"]

        vgroup = VGroup()
        for label, eq in zip(labels, equations):
            label_text = Text(label, font_size=30)
            eq_text = MathTex(eq)
            pair = VGroup(label_text, eq_text).arrange(RIGHT, buff=0.5)
            vgroup.add(pair)

        vgroup.arrange(DOWN, buff=0.7, aligned_edge=LEFT)
        vgroup.shift(DOWN * 0.5)

        for pair in vgroup:
            self.play(Write(pair[0]), Write(pair[1]))
            self.wait(0.5)

        self.wait(2)


class QuadraticFormula(Scene):
    """Display and factor a quadratic equation"""
    def construct(self):
        # Standard form
        quad = MathTex(r"ax^2 + bx + c = 0")
        self.play(Write(quad))
        self.wait(1)

        # Solution
        solution = MathTex(r"x = \frac{-b \pm \sqrt{b^2 - 4ac}}{2a}")
        self.play(Transform(quad, solution))
        self.wait(2)


class TaylorSeries(Scene):
    """Display Taylor series expansion"""
    def construct(self):
        title = Text("Taylor Series: sin(x)", font_size=40)
        title.to_edge(UP)
        self.play(Write(title))

        # Progressive Taylor series
        series = [
            r"\sin(x) \approx x",
            r"\sin(x) \approx x - \frac{x^3}{6}",
            r"\sin(x) \approx x - \frac{x^3}{6} + \frac{x^5}{120}",
            r"\sin(x) = x - \frac{x^3}{6} + \frac{x^5}{120} - \frac{x^7}{5040} + \cdots",
        ]

        current = MathTex(series[0])
        self.play(Write(current))
        self.wait(1)

        for s in series[1:]:
            next_eq = MathTex(s)
            self.play(Transform(current, next_eq))
            self.wait(1.5)

        self.wait(2)


class ProductRule(Scene):
    """Demonstrate the product rule for derivatives"""
    def construct(self):
        title = Text("Product Rule", font_size=44)
        title.to_edge(UP)
        self.play(Write(title))

        # Product rule formula
        rule = MathTex(r"(f \cdot g)' = f' \cdot g + f \cdot g'")
        rule.shift(UP)
        self.play(Write(rule))
        self.wait(1)

        # Example
        example_label = Text("Example:", font_size=32).shift(DOWN * 0.5)
        example = MathTex(r"\frac{d}{dx}[x^2 \sin(x)]")
        example.next_to(example_label, DOWN)

        self.play(Write(example_label), Write(example))
        self.wait(1)

        # Solution
        solution = MathTex(r"= 2x \sin(x) + x^2 \cos(x)")
        solution.next_to(example, DOWN)
        self.play(Write(solution))
        self.wait(2)


class ChainRule(Scene):
    """Demonstrate the chain rule"""
    def construct(self):
        title = Text("Chain Rule", font_size=44)
        title.to_edge(UP)
        self.play(Write(title))

        # Chain rule formula
        rule = MathTex(r"\frac{d}{dx}[f(g(x))] = f'(g(x)) \cdot g'(x)")
        rule.shift(UP * 0.5)
        self.play(Write(rule))
        self.wait(1)

        # Example from user's code: sin^2(x + 3)
        example = MathTex(r"\frac{d}{dx}[\sin^2(x + 3)]")
        example.shift(DOWN * 0.5)
        self.play(Write(example))
        self.wait(1)

        # Solution steps
        step1 = MathTex(r"= 2\sin(x + 3) \cdot \cos(x + 3) \cdot 1")
        step1.next_to(example, DOWN)
        self.play(Write(step1))
        self.wait(1)

        step2 = MathTex(r"= 2\sin(x + 3)\cos(x + 3)")
        step2.next_to(step1, DOWN)
        self.play(Write(step2))
        self.wait(2)
