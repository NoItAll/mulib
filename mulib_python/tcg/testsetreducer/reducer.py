"""Test set reducers for minimizing test suites."""

from __future__ import annotations

import abc
from typing import List, Set, TYPE_CHECKING

if TYPE_CHECKING:
    from mulib_python.tcg.tcg import TestCase


class TestSetReducer(abc.ABC):
    """Abstract base class for test set reducers.

    Test set reducers minimize a collection of test cases while
    maintaining coverage or other properties.
    """

    @abc.abstractmethod
    def reduce(self, test_cases: List["TestCase"]) -> List["TestCase"]:
        """Reduce the test set.

        Parameters
        ----------
        test_cases : list[TestCase]
            The original test cases.

        Returns
        -------
        list[TestCase]
            The reduced test set.
        """


class IdentityReducer(TestSetReducer):
    """A reducer that does not reduce (returns all test cases)."""

    def reduce(self, test_cases: List["TestCase"]) -> List["TestCase"]:
        return list(test_cases)


class UniqueInputReducer(TestSetReducer):
    """Reduces test cases by keeping only unique input combinations."""

    def reduce(self, test_cases: List["TestCase"]) -> List["TestCase"]:
        seen: Set[tuple] = set()
        result = []
        
        for tc in test_cases:
            key = tuple(sorted(tc.inputs.items()))
            if key not in seen:
                seen.add(key)
                result.append(tc)
        
        return result


class UniqueOutputReducer(TestSetReducer):
    """Reduces test cases by keeping only unique outputs."""

    def reduce(self, test_cases: List["TestCase"]) -> List["TestCase"]:
        seen: Set[int] = set()
        result = []
        
        for tc in test_cases:
            try:
                key = hash(tc.expected_output)
            except TypeError:
                key = id(tc.expected_output)
            
            if key not in seen:
                seen.add(key)
                result.append(tc)
        
        return result


class BoundaryValueReducer(TestSetReducer):
    """Keeps test cases that represent boundary values.

    Identifies cases where inputs are at extremes (min, max, zero).
    """

    def reduce(self, test_cases: List["TestCase"]) -> List["TestCase"]:
        if not test_cases:
            return []
        
        # Find boundary values for each input
        boundaries: dict[str, Set[Any]] = {}
        
        for tc in test_cases:
            for name, value in tc.inputs.items():
                if name not in boundaries:
                    boundaries[name] = set()
                if isinstance(value, (int, float)):
                    boundaries[name].add(value)
        
        # Calculate boundary thresholds for each input
        boundary_values: dict[str, Set[Any]] = {}
        for name, values in boundaries.items():
            if values:
                sorted_vals = sorted(v for v in values if isinstance(v, (int, float)))
                if sorted_vals:
                    boundary_values[name] = {
                        sorted_vals[0],  # min
                        sorted_vals[-1],  # max
                        0,
                        1,
                        -1,
                    }
        
        # Keep test cases with boundary inputs
        result = []
        for tc in test_cases:
            is_boundary = False
            for name, value in tc.inputs.items():
                if name in boundary_values and value in boundary_values[name]:
                    is_boundary = True
                    break
            if is_boundary:
                result.append(tc)
        
        # Always include at least one test case
        if not result and test_cases:
            result.append(test_cases[0])
        
        return result


class CompositeReducer(TestSetReducer):
    """Applies multiple reducers in sequence."""

    def __init__(self, *reducers: TestSetReducer) -> None:
        self._reducers = list(reducers)

    def reduce(self, test_cases: List["TestCase"]) -> List["TestCase"]:
        result = test_cases
        for reducer in self._reducers:
            result = reducer.reduce(result)
        return result
