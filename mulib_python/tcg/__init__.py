"""Test Case Generation package."""

from mulib_python.tcg.tcg import (
    TestCase,
    TestCaseGenerator,
    PythonTestRenderer,
    generate_tests,
)
from mulib_python.tcg.testsetreducer import (
    TestSetReducer,
    IdentityReducer,
    UniqueInputReducer,
    UniqueOutputReducer,
    BoundaryValueReducer,
    CompositeReducer,
)
from mulib_python.tcg.testsetsorter import (
    TestSetSorter,
    IdentitySorter,
    RandomSorter,
    DepthSorter,
    InputSizeSorter,
    CustomSorter,
    CompositeSorter,
)

__all__ = [
    # Core TCG
    "TestCase",
    "TestCaseGenerator",
    "PythonTestRenderer",
    "generate_tests",
    # Reducers
    "TestSetReducer",
    "IdentityReducer",
    "UniqueInputReducer",
    "UniqueOutputReducer",
    "BoundaryValueReducer",
    "CompositeReducer",
    # Sorters
    "TestSetSorter",
    "IdentitySorter",
    "RandomSorter",
    "DepthSorter",
    "InputSizeSorter",
    "CustomSorter",
    "CompositeSorter",
]
