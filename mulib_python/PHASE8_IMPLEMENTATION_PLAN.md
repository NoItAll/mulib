# Phase 8 Implementation Plan: Test Case Generation (TCG)

## Overview

Phase 8 adds a `mulib_python/tcg/` sub-package that converts `PathSolution` objects (produced by the symbolic execution engine in earlier phases) into an executable pytest test file.  The pipeline mirrors the Java TCG pipeline:

```
PathSolutions
  → TestCases
      → [pre-sort] → [reduce] → [post-sort]
          → TestCasesStringGenerator
              → pytest test file (string)
```

---

## 1. File Structure

```
mulib_python/tcg/
├── __init__.py                         # re-exports public API
├── test_case.py                        # TestCase dataclass
├── test_cases.py                       # TestCases collection
├── tcg_config.py                       # TcgConfig + TcgConfigBuilder
├── test_cases_string_generator.py      # TestCasesStringGenerator (orchestrator)
├── test_method_generator.py            # PytestTestMethodGenerator
├── test_class_generator.py             # PytestTestClassGenerator
├── testsetreducer/
│   ├── __init__.py
│   ├── base.py                         # abstract TestSetReducer + NullTestSetReducer
│   ├── simple_forwards.py              # SimpleForwardsTestSetReducer
│   ├── simple_backwards.py             # SimpleBackwardsTestSetReducer
│   ├── simple_greedy.py                # SimpleGreedyTestSetReducer
│   ├── competing.py                    # CompetingTestSetReducer
│   └── sequential.py                  # SequentialTestSetReducer
└── testsetsorter/
    ├── __init__.py
    ├── base.py                         # abstract TestSetSorter + NullTestSetSorter
    └── std_sorter.py                   # StdTestSetSorter
```

---

## 2. `test_case.py` — `TestCase`

### Purpose
Holds a single concrete test case derived from one `PathSolution`.

### Fields
```python
@dataclass
class TestCase:
    test_case_number: int           # auto-incremented global counter (class-level counter)
    exceptional: bool               # True if return_value is an exception instance
    inputs: list[Any]               # positional args to the function under test, in order
    inputs_after_execution: list[Any]  # state of object inputs AFTER the call (post-state checks)
    return_value: Any               # concrete return value (or exception instance if exceptional)
    cover: set[int]                 # set of covered "bits" (branch ids, instruction ids, etc.)
    # Internal mapping: id(post_exec_input) -> pre_exec_input
    _post_to_pre: dict[int, Any] = field(default_factory=dict, repr=False)
```

### Constructor logic
- Accept `label_name_to_value: dict[str, Any]`, `return_value`, `cover: set[int]`, `exceptional: bool`, `config: TcgConfig`.
- Parse keys matching `r"^arg(\d+)$"` into `inputs` (sorted by index).
- If `config.generate_post_state_checks`, parse keys matching `r"^arg(\d+)PostExec$"` into `inputs_after_execution`; build `_post_to_pre`.
- Thread-safe counter via `itertools.count` wrapped in a class variable.

### Key methods
```python
def get_input_pre_execution(self, post_exec_obj: Any) -> Any:
    return self._post_to_pre[id(post_exec_obj)]
```

### Factory from `PathSolution`
```python
@classmethod
def from_path_solution(cls, ps: PathSolution, config: TcgConfig) -> "TestCase":
    ...
```
Map `PathSolution.named_values` (a `dict[str, Any]`) directly to `label_name_to_value`. The return value is `ps.return_value`; `exceptional = isinstance(ps.return_value, BaseException)`.  Cover is `ps.cover` (a `set[int]`, may be empty).

---

## 3. `test_cases.py` — `TestCases`

### Purpose
A typed collection of `TestCase` objects plus metadata about the function under test.

### Fields
```python
@dataclass
class TestCases:
    test_cases: list[TestCase]
    func_under_test: Callable          # the actual Python callable
    func_name: str                     # e.g. "bubble_sort"
    module_name: str                   # e.g. "sorting.algorithms"
    class_name: str | None = None      # set if method belongs to a class
    is_static: bool = True             # False if first arg is 'self'/'cls'
```

### Methods
```python
def get_number_of_test_cases(self) -> int: ...
```

### Factory from `PathSolution` list
```python
@classmethod
def from_path_solutions(
    cls,
    path_solutions: list[PathSolution],
    func: Callable,
    config: TcgConfig,
) -> "TestCases":
    test_cases = [TestCase.from_path_solution(ps, config) for ps in path_solutions]
    return cls(test_cases=test_cases, func_under_test=func, ...)
```

Derive `module_name` from `func.__module__`, `class_name` from `func.__qualname__` (split on `"."`), `is_static` by checking whether the first parameter is named `self` or `cls` using `inspect.signature`.

---

## 4. `tcg_config.py` — `TcgConfig` / `TcgConfigBuilder`

### Purpose
Immutable configuration object controlling generation behaviour.

### Fields (all public, set once via builder)
| Field | Type | Default | Description |
|---|---|---|---|
| `pre_reduce_sorter` | `TestSetSorter` | `NullTestSetSorter()` | Sort before reducing |
| `test_set_reducer` | `TestSetReducer` | `NullTestSetReducer()` | Which reducer to use |
| `post_reduce_sorter` | `TestSetSorter` | `NullTestSetSorter()` | Sort after reducing |
| `indent` | `str` | `"    "` | 4-space indent |
| `max_fp_delta` | `float` | `1e-8` | Tolerance for float assertions |
| `print_output` | `bool` | `False` | Print generated string to stdout |
| `generate_post_state_checks` | `bool` | `True` | Emit post-state assertions |
| `test_class_postfix` | `str \| None` | `None` | Appended to generated class name |

### Builder
```python
class TcgConfigBuilder:
    def set_reducer(self, r: TestSetReducer) -> "TcgConfigBuilder": ...
    def set_pre_reduce_sorter(self, s: TestSetSorter) -> "TcgConfigBuilder": ...
    def set_post_reduce_sorter(self, s: TestSetSorter) -> "TcgConfigBuilder": ...
    def set_indent(self, indent: str) -> "TcgConfigBuilder": ...
    def set_max_fp_delta(self, delta: float) -> "TcgConfigBuilder": ...
    def set_print_output(self, flag: bool) -> "TcgConfigBuilder": ...
    def set_generate_post_state_checks(self, flag: bool) -> "TcgConfigBuilder": ...
    def set_test_class_postfix(self, postfix: str) -> "TcgConfigBuilder": ...
    def build(self) -> TcgConfig: ...
```

Note: Python does not need Java's `ASSUME_GETTERS/SETTERS/EQUALS` flags because Python uses `==` and `dataclasses`/`__dict__` introspection natively; these are omitted.

---

## 5. `test_method_generator.py` — `PytestTestMethodGenerator`

### Purpose
Generates the string for a single pytest test function body.

### State (reset per test case)
- `_obj_to_name: dict[int, str]` — maps `id(obj)` → variable name  
- `_name_counter: dict[str, int]` — tracks how many of each type name have been used  
- `_encountered_types: set[type]` — for `import` generation  
- `_lines: list[str]` — accumulates lines  
- `_test_index: int` — auto-incremented

### Public method
```python
def generate(self, tc: TestCase) -> str:
    """Return a complete `def test_<name>_<index>():` string."""
```

### Internal steps (mirrors Java `execute()`)
1. `_gen_inputs(tc)` — emit variable declarations for each input
2. `_gen_return_value(tc)` — emit variable declaration for expected return
3. `_gen_call_and_assert(tc)` — emit the function call and assertions
4. If `config.generate_post_state_checks` and there are post-exec inputs, emit post-state assertions

### Value serialization (`_repr_value(v) -> str`)
Produce a Python literal representation:
- `None` → `"None"`
- `bool` → `"True"` / `"False"`  
- `int`, `float`, `complex` → `repr(v)`
- `str` → `repr(v)` (handles escaping)
- `bytes` / `bytearray` → `repr(v)`
- `list` → recursive  
- `tuple` → recursive
- `dict` → recursive, keys and values recursively serialized
- `set`, `frozenset` → recursive
- Custom objects → use `__dict__` introspection; emit constructor call if `__init__` parameters match `__dict__` keys, otherwise use `dataclasses.asdict`-style attribute assignment

### Variable naming
- Type name lowercased + counter: `int0`, `int1`, `myClass0`, `listAr0` (arrays → `list_0`)
- Tracks via `_obj_to_name[id(obj)]`; skips re-generation for aliased objects

### Exceptional test cases
```python
def test_my_func_3():
    arg0 = 42
    with pytest.raises(ValueError):
        my_func(arg0)
```

### Normal test case with return value
```python
def test_my_func_0():
    # Initialize inputs
    arg0 = 5
    arg1 = [1, 3, 2]

    # Expected return value
    expected = [1, 2, 3]

    # Assert correctness
    assert expected == my_func(arg0, arg1)
```

### Float return value
```python
    assert abs(expected - my_func(arg0)) < 1e-8
```

### Void function (return type `None`)
```python
    my_func(arg0, arg1)
    # post-state checks follow
```

### Post-state checks
```python
    # Assert post-execution state
    assert expected_arg1 == arg1
```

### Method (non-static)
```python
def test_MyClass_my_method_0():
    self0 = MyClass()
    self0.x = 5
    arg1 = 3
    expected = 8
    assert expected == self0.my_method(arg1)
```

---

## 6. `test_class_generator.py` — `PytestTestClassGenerator`

### Purpose
Wraps all method strings into a complete pytest module (file-level, not a class).

### Method
```python
def generate(
    self,
    module_name: str,
    func_name: str,
    class_name: str | None,
    encountered_types: set[type],
    initial_count: int,
    reduced_count: int,
    method_strings: list[str],
) -> str:
```

### Output format
```python
# Auto-generated by mulib_python TCG
# Generated: <ISO timestamp>
# Function under test: <module_name>.<func_name>
# Test cases before reduction: <N>, after: <M>

import pytest
from <module_name> import <func_name_or_class>
# additional imports for encountered types

<method_string_0>

<method_string_1>

...
```

No wrapping `class` — tests are module-level functions, which is idiomatic pytest.

If `config.test_class_postfix` is set, add it as a suffix to the file-level comment and the function prefix: `test_<func_name><postfix>_<index>`.

---

## 7. `test_cases_string_generator.py` — `TestCasesStringGenerator`

### Purpose
Orchestrates the 6-stage pipeline.

### Constructor
```python
def __init__(self, test_cases: TestCases, config: TcgConfig):
    self._test_cases = test_cases
    self._config = config
    self._method_gen = PytestTestMethodGenerator(test_cases.func_name, test_cases.is_static, config)
    self._class_gen = PytestTestClassGenerator(config)
```

### Pipeline
```python
def generate(self) -> str:
    # Stage 1: pre-sort
    cases = self._config.pre_reduce_sorter.apply(self._test_cases.test_cases)
    # Stage 2: reduce
    cases = self._config.test_set_reducer.apply(cases)
    # Stage 3: post-sort
    cases = self._config.post_reduce_sorter.apply(cases)
    # Stage 4: generate method strings
    method_strings = [self._method_gen.generate(tc) for tc in cases]
    # Stage 5: wrap in module string
    result = self._class_gen.generate(
        module_name=self._test_cases.module_name,
        func_name=self._test_cases.func_name,
        class_name=self._test_cases.class_name,
        encountered_types=self._method_gen.encountered_types,
        initial_count=self._test_cases.get_number_of_test_cases(),
        reduced_count=len(cases),
        method_strings=method_strings,
    )
    # Stage 6: optionally print
    if self._config.print_output:
        print(result)
    return result
```

---

## 8. Test Set Reducers

### `testsetreducer/base.py`

```python
from abc import ABC, abstractmethod

class TestSetReducer(ABC):
    @abstractmethod
    def apply(self, test_cases: list[TestCase]) -> list[TestCase]: ...

class NullTestSetReducer(TestSetReducer):
    """Identity reducer — returns all test cases unchanged."""
    def apply(self, test_cases: list[TestCase]) -> list[TestCase]:
        return list(test_cases)
```

### `simple_forwards.py` — `SimpleForwardsTestSetReducer`

Iterates test cases in order; keeps a test case only if it adds new coverage bits.  
Coverage is a `set[int]`.

```python
def apply(self, test_cases):
    result, current_cover = [], set()
    for tc in test_cases:
        new_cover = current_cover | tc.cover
        if len(new_cover) > len(current_cover):
            result.append(tc)
            current_cover = new_cover
    return result
```

### `simple_backwards.py` — `SimpleBackwardsTestSetReducer`

Same as Forwards but iterates in reverse order, then reverses the result to preserve original ordering of kept cases.

```python
def apply(self, test_cases):
    result, current_cover = [], set()
    for tc in reversed(test_cases):
        new_cover = current_cover | tc.cover
        if len(new_cover) > len(current_cover):
            result.append(tc)
            current_cover = new_cover
    result.reverse()
    return result
```

### `simple_greedy.py` — `SimpleGreedyTestSetReducer`

At each iteration, picks the remaining test case that maximises the cardinality of the union of the current cover and its own cover.  Terminates when no remaining case adds new coverage.

```python
def apply(self, test_cases):
    remaining = list(test_cases)
    current_cover = set()
    result = []
    while True:
        best, best_gain = None, 0
        prune = []
        for tc in remaining:
            gain = len(current_cover | tc.cover) - len(current_cover)
            if gain > best_gain:
                best, best_gain = tc, gain
            elif gain == 0:
                prune.append(tc)
        if best is None:
            break
        result.append(best)
        current_cover |= best.cover
        remaining = [tc for tc in remaining if tc is not best and tc not in prune]
    return result
```

### `competing.py` — `CompetingTestSetReducer`

For each coverage bit, keep the test case with the highest ratio  
`unique_bits_covered / total_bits_covered` (least "redundant").  
Then take the union of those winning test cases.

```python
def apply(self, test_cases):
    # Map each coverage bit to the best test case for that bit
    bit_to_best: dict[int, TestCase] = {}
    for tc in test_cases:
        for bit in tc.cover:
            current = bit_to_best.get(bit)
            if current is None or len(tc.cover) < len(current.cover):
                bit_to_best[bit] = tc
    # Deduplicate while preserving original order
    seen, result = set(), []
    for tc in test_cases:
        if id(tc) not in seen and tc in bit_to_best.values():
            seen.add(id(tc))
            result.append(tc)
    return result
```

### `sequential.py` — `SequentialTestSetReducer`

Keeps test cases in order up to the first one that is fully covered by the union of all previous test cases.  Stops adding once complete coverage is achieved.

```python
def apply(self, test_cases):
    all_cover = set()
    for tc in test_cases:
        all_cover |= tc.cover
    result, achieved = [], set()
    for tc in test_cases:
        if achieved == all_cover:
            break
        new = tc.cover - achieved
        if new:
            result.append(tc)
            achieved |= new
    return result
```

---

## 9. Test Set Sorters

### `testsetsorter/base.py`

```python
from abc import ABC, abstractmethod

class TestSetSorter(ABC):
    @abstractmethod
    def apply(self, test_cases: list[TestCase]) -> list[TestCase]: ...

class NullTestSetSorter(TestSetSorter):
    """Identity sorter — returns a copy without reordering."""
    def apply(self, test_cases):
        return list(test_cases)
```

### `std_sorter.py` — `StdTestSetSorter`

Sorts by `test_case_number` ascending (mirrors Java `Comparator.comparingLong(TestCase::getTestCaseNumber)`).  Accepts an optional `key` callable for custom ordering.

```python
class StdTestSetSorter(TestSetSorter):
    def __init__(self, key=None, reverse=False):
        self._key = key or (lambda tc: tc.test_case_number)
        self._reverse = reverse

    def apply(self, test_cases):
        return sorted(test_cases, key=self._key, reverse=self._reverse)
```

---

## 10. Generated pytest test format

### Complete example output for `def add(a: int, b: int) -> int`

```python
# Auto-generated by mulib_python TCG
# Generated: 2024-01-15T10:30:00
# Function under test: mypackage.math_utils.add
# Test cases before reduction: 5, after: 3

import pytest
from mypackage.math_utils import add


def test_add_0():
    # Initialize inputs
    int0 = 3
    int1 = 4

    # Expected return value
    int2 = 7

    # Assert correctness of the output value
    assert int2 == add(int0, int1)


def test_add_1():
    # Initialize inputs
    int0 = -1
    int1 = 1

    # Expected return value
    int2 = 0

    # Assert correctness of the output value
    assert int2 == add(int0, int1)


def test_add_2():
    # Initialize inputs
    int0 = 0
    int1 = 0

    # Expected return value
    int2 = 0

    # Assert correctness of the output value
    assert int2 == add(int0, int1)
```

### Exceptional test case
```python
def test_divide_3():
    # Initialize inputs
    int0 = 1
    int1 = 0

    with pytest.raises(ZeroDivisionError):
        divide(int0, int1)
```

### Float assertion
```python
    assert abs(float0 - my_func(arg0)) < 1e-8
```

### Post-state assertion (for object inputs modified in-place)
```python
def test_sort_inplace_0():
    # Initialize inputs
    list0 = [3, 1, 2]
    
    # Initialize expected post-execution state
    list1 = [1, 2, 3]

    # Execute
    sort_inplace(list0)

    # Assert post-execution state of inputs
    assert list1 == list0
```

### Instance method call
```python
def test_MyClass_push_0():
    # Initialize inputs
    myClass0 = MyClass()
    myClass0.items = []
    int0 = 5

    # Execute (void return)
    myClass0.push(int0)

    # Assert post-execution state
    expected_myClass = MyClass()
    expected_myClass.items = [5]
    assert expected_myClass.items == myClass0.items
```

---

## 11. `__init__.py` public API

```python
# mulib_python/tcg/__init__.py
from .test_case import TestCase
from .test_cases import TestCases
from .tcg_config import TcgConfig, TcgConfigBuilder
from .test_cases_string_generator import TestCasesStringGenerator
from .testsetreducer.null import NullTestSetReducer
from .testsetreducer.simple_forwards import SimpleForwardsTestSetReducer
from .testsetreducer.simple_backwards import SimpleBackwardsTestSetReducer
from .testsetreducer.simple_greedy import SimpleGreedyTestSetReducer
from .testsetreducer.competing import CompetingTestSetReducer
from .testsetreducer.sequential import SequentialTestSetReducer
from .testsetsorter.null import NullTestSetSorter
from .testsetsorter.std_sorter import StdTestSetSorter

__all__ = [
    "TestCase", "TestCases", "TcgConfig", "TcgConfigBuilder",
    "TestCasesStringGenerator",
    "NullTestSetReducer", "SimpleForwardsTestSetReducer",
    "SimpleBackwardsTestSetReducer", "SimpleGreedyTestSetReducer",
    "CompetingTestSetReducer", "SequentialTestSetReducer",
    "NullTestSetSorter", "StdTestSetSorter",
]
```

---

## 12. Implementation Notes & Constraints

1. **No Java reflection equivalents needed** — Python introspects with `inspect`, `__dict__`, `dataclasses`, `typing.get_type_hints`.
2. **Coverage as `set[int]`** — Java uses `BitSet`; Python replaces it with `set[int]`. All reducer logic translates directly.
3. **Thread safety** — The `TestCase` global counter should use `itertools.count` wrapped with a `threading.Lock` or `itertools.count` alone (atomic on CPython due to GIL).
4. **Object serialization for complex types** — For custom objects not known at generation time, emit `__dict__` attribute assignments. If the class has a `__init__` that matches `__dict__` keys, emit a constructor call instead.
5. **Imports in generated file** — Collect `encountered_types` from the `PytestTestMethodGenerator` after all test cases are processed; emit `from <module> import <Type>` for each. Skip builtins (`int`, `str`, `list`, etc.).
6. **Indent** — All generated code uses `config.indent` (default 4 spaces). Method body lines are one level deep.
7. **Test function naming** — `test_<func_name>_<index>` where index starts at 0. If `test_class_postfix` is set: `test_<func_name><postfix>_<index>`.
8. **Float comparison** — Check `isinstance(v, float)` (or `isinstance(v, complex)` for complex); emit `abs(expected - actual) < config.max_fp_delta` instead of `==`.
9. **None return** — Emit `assert my_func(arg0) is None`.
10. **Void return** — Detected when `return_value is None` AND `tc.exceptional is False` AND the function signature return annotation is `None`. Emit just the call without assertion.
