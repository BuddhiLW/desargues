"""
Manim examples - can be called from both Python CLI and Clojure via libpython-clj
"""

from manim import *


class CreateCircle(Scene):
    """Basic quickstart example - creates a pink circle"""
    def construct(self):
        circle = Circle()  # create a circle
        circle.set_fill(PINK, opacity=0.5)  # set color and transparency
        self.play(Create(circle))  # show the circle on screen


class SquareToCircle(Scene):
    """Transform a square into a circle"""
    def construct(self):
        circle = Circle()
        circle.set_fill(BLUE, opacity=0.5)
        circle.set_stroke(BLUE_E, width=4)

        square = Square()
        square.rotate(PI / 4)

        self.play(Create(square))
        self.play(Transform(square, circle))
        self.play(FadeOut(square))


class SquareAndCircle(Scene):
    """Display a square and circle side by side"""
    def construct(self):
        circle = Circle()
        circle.set_fill(PINK, opacity=0.5)

        square = Square()
        square.set_fill(BLUE, opacity=0.5)

        square.next_to(circle, RIGHT, buff=0.5)
        self.play(Create(circle), Create(square))


class AnimatedSquareToCircle(Scene):
    """Animated transformation with rotation"""
    def construct(self):
        circle = Circle()
        square = Square()

        self.play(Create(square))
        self.play(square.animate.rotate(PI / 4))
        self.play(Transform(square, circle))
        self.play(square.animate.set_fill(PINK, opacity=0.5))


class DifferentRotations(Scene):
    """Show different rotation methods"""
    def construct(self):
        left_square = Square(color=BLUE, fill_opacity=0.7).shift(2 * LEFT)
        right_square = Square(color=GREEN, fill_opacity=0.7).shift(2 * RIGHT)

        self.play(
            left_square.animate.rotate(PI),
            Rotate(right_square, angle=PI),
            run_time=2
        )
        self.wait()
