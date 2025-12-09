"""
Manim scenes for showing equation evaluation at specific values
Can be driven by Emmy expressions from Clojure
"""

from manim import *


class EquationEvaluation(Scene):
    """
    Show an equation, substitute a value, and display the result

    Example:
        f(x) = x² + 2x + 1
        f(3) = 3² + 2(3) + 1
             = 9 + 6 + 1
             = 16
    """
    def __init__(self,
                 equation_latex=None,
                 var_name=None,
                 var_value=None,
                 substituted_latex=None,
                 result=None,
                 **kwargs):
        self.equation_latex = equation_latex or r"f(x) = x^2 + 2x + 1"
        self.var_name = var_name or "x"
        self.var_value = var_value or "3"
        self.substituted_latex = substituted_latex or r"3^2 + 2(3) + 1"
        self.result = result or "16"
        super().__init__(**kwargs)

    def construct(self):
        # Title
        title = Text("Evaluating at a Point", font_size=40)
        title.to_edge(UP)
        self.play(Write(title))
        self.wait(0.5)

        # Original equation
        equation = MathTex(self.equation_latex)
        equation.shift(UP * 1.5)
        self.play(Write(equation))
        self.wait(1)

        # Show substitution
        subst_text = Text(f"Evaluate at {self.var_name} = {self.var_value}",
                         font_size=32)
        subst_text.next_to(equation, DOWN, buff=0.8)
        self.play(Write(subst_text))
        self.wait(0.5)

        # Substituted expression
        substituted = MathTex(f"f({self.var_value}) = {self.substituted_latex}")
        substituted.next_to(subst_text, DOWN, buff=0.5)
        self.play(Write(substituted))
        self.wait(1)

        # Result
        result_eq = MathTex(f"= {self.result}")
        result_eq.next_to(substituted, DOWN, buff=0.3)
        result_eq.set_color(YELLOW)
        self.play(Write(result_eq))
        self.wait(2)


class EquationEvaluationSteps(Scene):
    """
    Show step-by-step evaluation of an equation
    """
    def __init__(self,
                 equation_latex=None,
                 var_name=None,
                 var_value=None,
                 steps=None,
                 **kwargs):
        self.equation_latex = equation_latex or r"f(x) = \sin(x + \pi)"
        self.var_name = var_name or "x"
        self.var_value = var_value or "0"
        self.steps = steps or [
            r"f(0) = \sin(0 + \pi)",
            r"= \sin(\pi)",
            r"= 0"
        ]
        super().__init__(**kwargs)

    def construct(self):
        # Original equation
        equation = MathTex(self.equation_latex)
        equation.to_edge(UP)
        self.play(Write(equation))
        self.wait(1)

        # Evaluation label
        eval_label = Text(f"Evaluate at {self.var_name} = {self.var_value}:",
                         font_size=32)
        eval_label.shift(UP * 1)
        self.play(Write(eval_label))
        self.wait(0.5)

        # Show steps
        current = None
        for i, step in enumerate(self.steps):
            step_eq = MathTex(step)
            if i == 0:
                step_eq.shift(UP * 0.2)
                self.play(Write(step_eq))
                current = step_eq
            else:
                step_eq.next_to(current, DOWN, buff=0.4, aligned_edge=LEFT)
                if i == len(self.steps) - 1:
                    step_eq.set_color(YELLOW)
                self.play(Write(step_eq))
                current = step_eq
            self.wait(1)

        self.wait(2)


class EvaluationTable(Scene):
    """
    Show a table of expressions evaluated at different values
    """
    def __init__(self,
                 title_text=None,
                 headers=None,
                 rows=None,
                 **kwargs):
        self.title_text = title_text or "Function Evaluations"
        self.headers = headers or ["x", "f(x)", "Value"]
        self.rows = rows or [
            ["0", r"\sin(0)", "0"],
            ["\\pi/2", r"\sin(\pi/2)", "1"],
            ["\\pi", r"\sin(\pi)", "0"],
        ]
        super().__init__(**kwargs)

    def construct(self):
        # Title
        title = Text(self.title_text, font_size=40)
        title.to_edge(UP)
        self.play(Write(title))
        self.wait(0.5)

        # Create table
        table_data = [self.headers] + self.rows

        # Create table using MathTex for all cells
        table = MobjectTable(
            [[MathTex(cell) for cell in row] for row in table_data],
            include_outer_lines=True
        )
        table.scale(0.7)
        table.shift(DOWN * 0.5)

        # Highlight header row
        for cell in table.get_rows()[0]:
            cell.set_color(BLUE)

        # Animate table creation
        self.play(Create(table))
        self.wait(2)

        # Highlight results column
        result_col = [table.get_rows()[i][2] for i in range(1, len(table_data))]
        for cell in result_col:
            self.play(cell.animate.set_color(YELLOW), run_time=0.3)

        self.wait(2)


class MultipleEvaluations(Scene):
    """
    Evaluate the same expression at multiple points
    """
    def __init__(self,
                 equation_latex=None,
                 var_name=None,
                 evaluations=None,  # List of (value, result) tuples
                 **kwargs):
        self.equation_latex = equation_latex or r"f(x) = x^2"
        self.var_name = var_name or "x"
        self.evaluations = evaluations or [
            ("1", "1"),
            ("2", "4"),
            ("3", "9"),
            ("4", "16")
        ]
        super().__init__(**kwargs)

    def construct(self):
        # Show equation
        equation = MathTex(self.equation_latex)
        equation.to_edge(UP)
        self.play(Write(equation))
        self.wait(1)

        # Create evaluation rows
        eval_group = VGroup()

        for value, result in self.evaluations:
            eval_text = MathTex(f"f({value}) = {result}")
            eval_group.add(eval_text)

        eval_group.arrange(DOWN, buff=0.5, aligned_edge=LEFT)
        eval_group.shift(DOWN * 0.5)

        # Animate each evaluation
        for eval_text in eval_group:
            self.play(Write(eval_text), run_time=0.8)
            self.wait(0.3)

        self.wait(2)


class ComparisonTable(Scene):
    """
    Compare multiple functions evaluated at the same points
    """
    def __init__(self,
                 title_text=None,
                 function_names=None,
                 x_values=None,
                 results=None,  # 2D list: results[func_idx][x_idx]
                 **kwargs):
        self.title_text = title_text or "Function Comparison"
        self.function_names = function_names or [r"f(x) = \sin(x)", r"g(x) = \cos(x)"]
        self.x_values = x_values or ["0", r"\pi/2", r"\pi"]
        # results[func_idx][x_idx]
        self.results = results or [
            ["0", "1", "0"],      # sin values
            ["1", "0", "-1"],     # cos values
        ]
        super().__init__(**kwargs)

    def construct(self):
        # Title
        title = Text(self.title_text, font_size=40)
        title.to_edge(UP)
        self.play(Write(title))
        self.wait(0.5)

        # Display functions
        funcs = VGroup()
        for i, func_latex in enumerate(self.function_names):
            func = MathTex(func_latex)
            funcs.add(func)
        funcs.arrange(RIGHT, buff=1.5)
        funcs.shift(UP * 1.5)
        self.play(Write(funcs))
        self.wait(1)

        # Create comparison table
        # Header: x values
        headers = [r"x"] + self.x_values

        # Rows: function name + results
        rows = []
        for i, func_name in enumerate([r"f", r"g"]):
            row = [func_name] + self.results[i]
            rows.append(row)

        table_data = [headers] + rows

        table = MobjectTable(
            [[MathTex(cell) for cell in row] for row in table_data],
            include_outer_lines=True
        )
        table.scale(0.6)
        table.shift(DOWN * 0.8)

        # Highlight header
        for cell in table.get_rows()[0]:
            cell.set_color(BLUE)

        self.play(Create(table))
        self.wait(2)


class DerivativeAtPoint(Scene):
    """
    Show derivative evaluation at a specific point
    """
    def __init__(self,
                 func_latex=None,
                 deriv_latex=None,
                 point=None,
                 func_value=None,
                 deriv_value=None,
                 **kwargs):
        self.func_latex = func_latex or r"f(x) = \sin(x)"
        self.deriv_latex = deriv_latex or r"f'(x) = \cos(x)"
        self.point = point or r"\pi/4"
        self.func_value = func_value or r"\frac{\sqrt{2}}{2}"
        self.deriv_value = deriv_value or r"\frac{\sqrt{2}}{2}"
        super().__init__(**kwargs)

    def construct(self):
        # Title
        title = Text("Function and Derivative at a Point", font_size=36)
        title.to_edge(UP)
        self.play(Write(title))
        self.wait(0.5)

        # Function and derivative
        func = MathTex(self.func_latex)
        deriv = MathTex(self.deriv_latex)

        equations = VGroup(func, deriv).arrange(DOWN, buff=0.5, aligned_edge=LEFT)
        equations.shift(UP * 1)

        self.play(Write(func))
        self.wait(0.5)
        self.play(Write(deriv))
        self.wait(1)

        # Evaluation point
        point_text = MathTex(f"\\text{{At }} x = {self.point}")
        point_text.next_to(equations, DOWN, buff=0.8)
        self.play(Write(point_text))
        self.wait(0.5)

        # Function value
        func_val = MathTex(f"f({self.point}) = {self.func_value}")
        func_val.next_to(point_text, DOWN, buff=0.5)
        self.play(Write(func_val))
        self.wait(0.5)

        # Derivative value
        deriv_val = MathTex(f"f'({self.point}) = {self.deriv_value}")
        deriv_val.next_to(func_val, DOWN, buff=0.3)
        deriv_val.set_color(YELLOW)
        self.play(Write(deriv_val))
        self.wait(2)

        # Interpretation
        interp = Text("(slope of tangent line)", font_size=24, color=GRAY)
        interp.next_to(deriv_val, RIGHT, buff=0.3)
        self.play(FadeIn(interp))
        self.wait(2)


class EvaluationAnimation(Scene):
    """
    Animate the substitution process itself
    """
    def __init__(self,
                 equation_latex=None,
                 var_name=None,
                 var_value=None,
                 **kwargs):
        self.equation_latex = equation_latex or r"x^2 + 2x + 1"
        self.var_name = var_name or "x"
        self.var_value = var_value or "3"
        super().__init__(**kwargs)

    def construct(self):
        # Original expression
        original = MathTex(self.equation_latex)
        self.play(Write(original))
        self.wait(1)

        # Create substituted version by replacing x with value
        # This is simplified - in practice you'd construct it properly
        substituted_latex = self.equation_latex.replace(self.var_name,
                                                        f"({self.var_value})")
        substituted = MathTex(substituted_latex)

        # Transform
        self.play(Transform(original, substituted))
        self.wait(2)


class TabulateFunction(Scene):
    """
    Create a comprehensive table of a function's values
    """
    def __init__(self,
                 func_latex=None,
                 title_text=None,
                 x_values=None,
                 f_values=None,
                 **kwargs):
        self.func_latex = func_latex or r"f(x) = e^{-x^2}"
        self.title_text = title_text or "Gaussian Function Values"
        self.x_values = x_values or ["-2", "-1", "0", "1", "2"]
        self.f_values = f_values or ["0.018", "0.368", "1.000", "0.368", "0.018"]
        super().__init__(**kwargs)

    def construct(self):
        # Title
        title = Text(self.title_text, font_size=38)
        title.to_edge(UP)
        self.play(Write(title))
        self.wait(0.5)

        # Show function
        func = MathTex(self.func_latex)
        func.next_to(title, DOWN, buff=0.5)
        self.play(Write(func))
        self.wait(1)

        # Create table
        headers = [r"x", r"f(x)"]
        rows = [[x, f] for x, f in zip(self.x_values, self.f_values)]
        table_data = [headers] + rows

        table = MobjectTable(
            [[MathTex(cell) for cell in row] for row in table_data],
            include_outer_lines=True
        )
        table.scale(0.7)
        table.shift(DOWN * 0.3)

        # Highlight header
        for cell in table.get_rows()[0]:
            cell.set_color(BLUE)

        # Animate row by row
        self.play(Create(table.get_horizontal_lines()))
        self.play(Create(table.get_vertical_lines()))

        for row in table.get_rows():
            self.play(Write(row), run_time=0.5)
            self.wait(0.2)

        self.wait(2)
