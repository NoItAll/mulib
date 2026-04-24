"""Test set sorters for ordering test suites."""

from __future__ import annotations

import abc
import random
from typing import Any, Callable, List, TYPE_CHECKING

if TYPE_CHECKING:
    from mulib_python.tcg.tcg import TestCase


class TestSetSorter(abc.ABC):
    """Abstract base class for test set sorters.

    Test set sorters order test cases according to various criteria
    (e.g., prioritizing shorter tests, boundary values, etc.).
    """

    @abc.abstractmethod
    def sort(self, test_cases: List["TestCase"]) -> List["TestCase"]:
        """Sort the test cases.

        Parameters
        ----------
        test_cases : list[TestCase]
            The test cases to sort.

        Returns
        -------
        list[TestCase]
            The sorted test cases.
        """


class IdentitySorter(TestSetSorter):
    """A sorter that maintains original order."""

    def sort(self, test_cases: List["TestCase"]) -> List["TestCase"]:
        return list(test_cases)


class RandomSorter(TestSetSorter):
    """Randomly shuffles test cases."""

    def __init__(self, seed: int | None = None) -> None:
        self._rng = random.Random(seed)

    def sort(self, test_cases: List["TestCase"]) -> List["TestCase"]:
        result = list(test_cases)
        self._rng.shuffle(result)
        return result


class DepthSorter(TestSetSorter):
    """Sorts by path depth (shallow paths first)."""

    def __init__(self, reverse: bool = False) -> None:
        self._reverse = reverse

    def sort(self, test_cases: List["TestCase"]) -> List["TestCase"]:
        return sorted(
            test_cases,
            key=lambda tc: tc.metadata.get("depth", 0),
            reverse=self._reverse,
        )


class InputSizeSorter(TestSetSorter):
    """Sorts by number of inputs (fewer inputs first)."""

    def __init__(self, reverse: bool = False) -> None:
        self._reverse = reverse

    def sort(self, test_cases: List["TestCase"]) -> List["TestCase"]:
        return sorted(
            test_cases,
            key=lambda tc: len(tc.inputs),
            reverse=self._reverse,
        )


class CustomSorter(TestSetSorter):
    """Sorts using a custom key function."""

    def __init__(
        self,
        key: Callable[["TestCase"], Any],
        reverse: bool = False,
    ) -> None:
        self._key = key
        self._reverse = reverse

    def sort(self, test_cases: List["TestCase"]) -> List["TestCase"]:
        return sorted(test_cases, key=self._key, reverse=self._reverse)


class CompositeSorter(TestSetSorter):
    """Sorts using multiple criteria (tie-breakers)."""

    def __init__(self, *sorters: TestSetSorter) -> None:
        self._sorters = list(sorters)

    def sort(self, test_cases: List["TestCase"]) -> List["TestCase"]:
        result = test_cases
        # Apply sorters in reverse order so first sorter has highest priority
        for sorter in reversed(self._sorters):
            result = sorter.sort(result)
        return result
