"""Test Case Generation (TCG) package.

Provides utilities for generating test cases from symbolic execution solutions.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any, Callable, Dict, List, Optional, TYPE_CHECKING

if TYPE_CHECKING:
    from mulib_python.solution import Solution


@dataclass
class TestCase:
    """A generated test case.

    Attributes
    ----------
    inputs : dict
        Mapping from input variable names to concrete values.
    expected_output : Any
        The expected return value.
    description : str
        Human-readable description of the test case.
    metadata : dict
        Additional metadata (path depth, constraints, etc.).
    """
    inputs: Dict[str, Any]
    expected_output: Any
    description: str = ""
    metadata: Dict[str, Any] = field(default_factory=dict)

    def __repr__(self) -> str:
        return f"TestCase(inputs={self.inputs}, expected={self.expected_output})"


class TestCaseGenerator:
    """Generates test cases from symbolic execution solutions.

    Converts Solutions into TestCase objects that can be serialized
    or used directly in test frameworks.
    """

    def __init__(
        self,
        input_names: Optional[List[str]] = None,
        description_template: str = "Test case {index}",
    ) -> None:
        self._input_names = input_names or []
        self._description_template = description_template

    def generate_from_solution(
        self, solution: "Solution", index: int = 0
    ) -> TestCase:
        """Generate a test case from a single solution."""
        inputs = {}
        labels = solution.labels.id_to_label
        
        # Extract inputs
        for name in self._input_names:
            if name in labels:
                inputs[name] = labels[name]
        
        # If no input names specified, include all except "return"
        if not self._input_names:
            for name, value in labels.items():
                if name != "return":
                    inputs[name] = value
        
        return TestCase(
            inputs=inputs,
            expected_output=solution.return_value,
            description=self._description_template.format(index=index),
            metadata={"solution_labels": labels},
        )

    def generate_from_solutions(
        self, solutions: List["Solution"]
    ) -> List[TestCase]:
        """Generate test cases from multiple solutions."""
        return [
            self.generate_from_solution(sol, i)
            for i, sol in enumerate(solutions)
        ]


class PythonTestRenderer:
    """Renders test cases as Python unittest/pytest code."""

    def __init__(
        self,
        function_name: str,
        module_name: str = "",
        use_pytest: bool = True,
    ) -> None:
        self._function_name = function_name
        self._module_name = module_name
        self._use_pytest = use_pytest

    def render(self, test_cases: List[TestCase]) -> str:
        """Render test cases as Python code."""
        lines = []
        
        if self._use_pytest:
            lines.append("import pytest")
        else:
            lines.append("import unittest")
        
        if self._module_name:
            lines.append(f"from {self._module_name} import {self._function_name}")
        
        lines.append("")
        
        if self._use_pytest:
            for i, tc in enumerate(test_cases):
                lines.append(f"def test_{self._function_name}_{i}():")
                lines.append(f"    # {tc.description}")
                
                # Build function call
                args = ", ".join(f"{k}={v!r}" for k, v in tc.inputs.items())
                lines.append(f"    result = {self._function_name}({args})")
                lines.append(f"    assert result == {tc.expected_output!r}")
                lines.append("")
        else:
            lines.append(f"class Test{self._function_name.title()}(unittest.TestCase):")
            for i, tc in enumerate(test_cases):
                lines.append(f"    def test_{i}(self):")
                lines.append(f"        # {tc.description}")
                args = ", ".join(f"{k}={v!r}" for k, v in tc.inputs.items())
                lines.append(f"        result = {self._function_name}({args})")
                lines.append(f"        self.assertEqual(result, {tc.expected_output!r})")
                lines.append("")
        
        return "\n".join(lines)


def generate_tests(
    func: Callable,
    solutions: List["Solution"],
    input_names: Optional[List[str]] = None,
    use_pytest: bool = True,
) -> str:
    """Convenience function to generate test code from solutions.

    Parameters
    ----------
    func : callable
        The function being tested.
    solutions : list[Solution]
        Solutions from symbolic execution.
    input_names : list[str], optional
        Names of input variables.
    use_pytest : bool
        Whether to generate pytest or unittest code.

    Returns
    -------
    str
        Python test code.
    """
    generator = TestCaseGenerator(input_names=input_names)
    test_cases = generator.generate_from_solutions(solutions)
    
    renderer = PythonTestRenderer(
        function_name=func.__name__,
        use_pytest=use_pytest,
    )
    
    return renderer.render(test_cases)
