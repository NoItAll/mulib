"""Tests for Test Case Generation (TCG)."""

import pytest
from mulib_python.tcg.tcg import (
    TestCase,
    TestCaseGenerator,
    PythonTestRenderer,
    generate_tests,
)
from mulib_python.tcg.testsetreducer.reducer import (
    TestSetReducer,
    IdentityReducer,
    UniqueInputReducer,
    UniqueOutputReducer,
    BoundaryValueReducer,
    CompositeReducer,
)
from mulib_python.tcg.testsetsorter.sorter import (
    TestSetSorter,
    IdentitySorter,
    RandomSorter,
    DepthSorter,
    InputSizeSorter,
    CompositeSorter,
)
from mulib_python.solution import Labels, Solution


# =============================================================================
# TestCase Tests
# =============================================================================

def test_testcase_creation():
    tc = TestCase(
        inputs={"x": 5, "y": 10},
        expected_output=15,
        description="Addition test",
    )
    
    assert tc.inputs == {"x": 5, "y": 10}
    assert tc.expected_output == 15
    assert tc.description == "Addition test"


def test_testcase_repr():
    tc = TestCase(inputs={"x": 1}, expected_output=2)
    assert "TestCase" in repr(tc)


# =============================================================================
# TestCaseGenerator Tests
# =============================================================================

def create_mock_solution(inputs: dict, return_value):
    """Helper to create a mock solution."""
    id_to_label = dict(inputs)
    id_to_label["return"] = return_value
    labels = Labels(inputs, id_to_label)
    return Solution(return_value=return_value, labels=labels)


def test_generator_from_solution():
    solution = create_mock_solution({"x": 5, "y": 10}, 15)
    
    generator = TestCaseGenerator(input_names=["x", "y"])
    tc = generator.generate_from_solution(solution)
    
    assert tc.inputs == {"x": 5, "y": 10}
    assert tc.expected_output == 15


def test_generator_auto_inputs():
    solution = create_mock_solution({"a": 1, "b": 2, "c": 3}, 6)
    
    generator = TestCaseGenerator()
    tc = generator.generate_from_solution(solution)
    
    # Should include all except "return"
    assert "a" in tc.inputs
    assert "b" in tc.inputs
    assert "c" in tc.inputs


def test_generator_from_solutions():
    solutions = [
        create_mock_solution({"x": 1}, 1),
        create_mock_solution({"x": 2}, 4),
        create_mock_solution({"x": 3}, 9),
    ]
    
    generator = TestCaseGenerator()
    test_cases = generator.generate_from_solutions(solutions)
    
    assert len(test_cases) == 3


# =============================================================================
# PythonTestRenderer Tests
# =============================================================================

def test_renderer_pytest():
    test_cases = [
        TestCase(inputs={"x": 1}, expected_output=1, description="Test 1"),
        TestCase(inputs={"x": 2}, expected_output=4, description="Test 2"),
    ]
    
    renderer = PythonTestRenderer("square", use_pytest=True)
    code = renderer.render(test_cases)
    
    assert "import pytest" in code
    assert "def test_square_0" in code
    assert "def test_square_1" in code
    assert "assert result ==" in code


def test_renderer_unittest():
    test_cases = [
        TestCase(inputs={"x": 1}, expected_output=1),
    ]
    
    renderer = PythonTestRenderer("square", use_pytest=False)
    code = renderer.render(test_cases)
    
    assert "import unittest" in code
    assert "class TestSquare" in code
    assert "def test_0" in code
    assert "self.assertEqual" in code


def test_renderer_with_module():
    test_cases = [TestCase(inputs={"x": 1}, expected_output=1)]
    
    renderer = PythonTestRenderer("square", module_name="mymodule")
    code = renderer.render(test_cases)
    
    assert "from mymodule import square" in code


# =============================================================================
# generate_tests Convenience Function Tests
# =============================================================================

def test_generate_tests():
    def square(x):
        return x * x
    
    solutions = [
        create_mock_solution({"x": 2}, 4),
        create_mock_solution({"x": 3}, 9),
    ]
    
    code = generate_tests(square, solutions)
    
    assert "def test_square_" in code


# =============================================================================
# Test Set Reducer Tests
# =============================================================================

def test_identity_reducer():
    test_cases = [
        TestCase(inputs={"x": 1}, expected_output=1),
        TestCase(inputs={"x": 2}, expected_output=4),
    ]
    
    reducer = IdentityReducer()
    result = reducer.reduce(test_cases)
    
    assert len(result) == 2


def test_unique_input_reducer():
    test_cases = [
        TestCase(inputs={"x": 1}, expected_output=1),
        TestCase(inputs={"x": 1}, expected_output=1),  # duplicate
        TestCase(inputs={"x": 2}, expected_output=4),
    ]
    
    reducer = UniqueInputReducer()
    result = reducer.reduce(test_cases)
    
    assert len(result) == 2


def test_unique_output_reducer():
    test_cases = [
        TestCase(inputs={"x": 1}, expected_output=1),
        TestCase(inputs={"x": -1}, expected_output=1),  # same output
        TestCase(inputs={"x": 2}, expected_output=4),
    ]
    
    reducer = UniqueOutputReducer()
    result = reducer.reduce(test_cases)
    
    assert len(result) == 2


def test_boundary_value_reducer():
    test_cases = [
        TestCase(inputs={"x": 0}, expected_output=0),
        TestCase(inputs={"x": 5}, expected_output=25),
        TestCase(inputs={"x": 10}, expected_output=100),
    ]
    
    reducer = BoundaryValueReducer()
    result = reducer.reduce(test_cases)
    
    # Should keep boundary cases (0 and 10)
    assert len(result) >= 1


def test_composite_reducer():
    test_cases = [
        TestCase(inputs={"x": 1}, expected_output=1),
        TestCase(inputs={"x": 1}, expected_output=1),
        TestCase(inputs={"x": 2}, expected_output=4),
        TestCase(inputs={"x": -2}, expected_output=4),  # same output
    ]
    
    reducer = CompositeReducer(
        UniqueInputReducer(),
        UniqueOutputReducer(),
    )
    result = reducer.reduce(test_cases)
    
    # After unique inputs: 3, after unique outputs: 2
    assert len(result) == 2


# =============================================================================
# Test Set Sorter Tests
# =============================================================================

def test_identity_sorter():
    test_cases = [
        TestCase(inputs={"x": 3}, expected_output=9),
        TestCase(inputs={"x": 1}, expected_output=1),
        TestCase(inputs={"x": 2}, expected_output=4),
    ]
    
    sorter = IdentitySorter()
    result = sorter.sort(test_cases)
    
    assert result[0].inputs["x"] == 3  # Order preserved


def test_random_sorter():
    test_cases = [
        TestCase(inputs={"x": i}, expected_output=i*i)
        for i in range(10)
    ]
    
    sorter = RandomSorter(seed=42)
    result = sorter.sort(test_cases)
    
    # Should have same elements in different order
    assert len(result) == 10


def test_depth_sorter():
    test_cases = [
        TestCase(inputs={"x": 1}, expected_output=1, metadata={"depth": 5}),
        TestCase(inputs={"x": 2}, expected_output=4, metadata={"depth": 1}),
        TestCase(inputs={"x": 3}, expected_output=9, metadata={"depth": 3}),
    ]
    
    sorter = DepthSorter()
    result = sorter.sort(test_cases)
    
    # Should be sorted by depth: 1, 3, 5
    assert result[0].metadata["depth"] == 1
    assert result[2].metadata["depth"] == 5


def test_input_size_sorter():
    test_cases = [
        TestCase(inputs={"a": 1, "b": 2, "c": 3}, expected_output=6),
        TestCase(inputs={"x": 1}, expected_output=1),
        TestCase(inputs={"x": 1, "y": 2}, expected_output=3),
    ]
    
    sorter = InputSizeSorter()
    result = sorter.sort(test_cases)
    
    # Should be sorted by input count: 1, 2, 3
    assert len(result[0].inputs) == 1
    assert len(result[2].inputs) == 3


def test_composite_sorter():
    test_cases = [
        TestCase(inputs={"x": 1}, expected_output=1, metadata={"depth": 3}),
        TestCase(inputs={"x": 1, "y": 2}, expected_output=3, metadata={"depth": 1}),
        TestCase(inputs={"x": 1}, expected_output=1, metadata={"depth": 1}),
    ]
    
    # Sort by depth first, then by input size
    sorter = CompositeSorter(DepthSorter(), InputSizeSorter())
    result = sorter.sort(test_cases)
    
    # Depth has priority, so depth=1 cases come first
    assert result[0].metadata["depth"] == 1


# =============================================================================
# Edge Cases
# =============================================================================

def test_empty_test_cases():
    reducer = UniqueInputReducer()
    assert reducer.reduce([]) == []
    
    sorter = DepthSorter()
    assert sorter.sort([]) == []


def test_single_test_case():
    tc = TestCase(inputs={"x": 1}, expected_output=1)
    
    reducer = UniqueInputReducer()
    assert reducer.reduce([tc]) == [tc]
    
    sorter = RandomSorter()
    assert sorter.sort([tc]) == [tc]
