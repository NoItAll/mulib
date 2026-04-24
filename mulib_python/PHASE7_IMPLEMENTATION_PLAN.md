# Phase 7 Implementation Plan: Configuration & High-Level API

## Overview

Phase 7 wires together all lower-level components (expressions, constraints, search,
solver) into the user-facing API.  Because Python is already interpreted there is **no
bytecode transformation step** — the search region is an ordinary Python callable and
symbolic values are injected by calling `free_int(name, ...)` etc. inside that
callable.

Files to create/extend:

| File | New / Extend |
|---|---|
| `mulib_python/config.py` | **New** — `MulibConfig` dataclass + `MulibConfigBuilder` |
| `mulib_python/solution.py` | **New** — `Solution`, `PathSolution` |
| `mulib_python/context.py` | **New** — `MulibContext` |
| `mulib_python/mulib.py` | **New** — top-level API functions |
| `mulib_python/__init__.py` | **Extend** — re-export public symbols |
| `mulib_python/exceptions.py` | Already has `Fail`; no change needed |

---

## 1. `MulibConfig` — `mulib_python/config.py`

### 1.1 Enumerations (same file or a `mulib_python/enums.py`)

```python
import enum

class SearchStrategy(enum.Enum):
    DFS   = "DFS"
    BFS   = "BFS"
    IDDFS = "IDDFS"
    DSAS  = "DSAS"   # depth-first + BFS hybrid
    IDDSAS = "IDDSAS"

class ChoiceOptionDequeType(enum.Enum):
    SIMPLE           = "SIMPLE"
    BUDGETED         = "BUDGETED"
    BUDGETED_DEEPEST = "BUDGETED_DEEPEST"

class SolverType(enum.Enum):
    Z3_INCREMENTAL    = "Z3_INCREMENTAL"
    Z3_NON_INCREMENTAL = "Z3_NON_INCREMENTAL"
    # extend as further solvers are added
```

### 1.2 `MulibConfig` dataclass

Use a **frozen `@dataclass`** so instances are immutable after creation (matches the
Java final-field pattern).  All `Optional` Java fields become `Optional[T]`; Java
`0` as sentinel "not set" becomes `None`.

```python
from __future__ import annotations
from dataclasses import dataclass, field
from typing import Callable, Optional
import logging

@dataclass(frozen=True)
class MulibConfig:
    # ── TREE ──────────────────────────────────────────────────────────────
    TREE_INDENTATION: str = "    "
    TREE_ENLIST_LEAVES: bool = False

    # ── SEARCH ────────────────────────────────────────────────────────────
    SEARCH_MAIN_STRATEGY: SearchStrategy = SearchStrategy.DFS
    SEARCH_ADDITIONAL_PARALLEL_STRATEGIES: tuple[SearchStrategy, ...] = ()
    SEARCH_CHOICE_OPTION_DEQUE_TYPE: ChoiceOptionDequeType = ChoiceOptionDequeType.SIMPLE
    SEARCH_ACTIVATE_PARALLEL_FOR: int = 2        # activate extra strategy after N choices
    SEARCH_CONCOLIC: bool = False
    SEARCH_ALLOW_EXCEPTIONS: bool = False
    SEARCH_LABEL_RESULT_VALUE: bool = True
    SEARCH_RANDOMIZE_SELECTION_FROM_NEW_CHOICE: bool = False

    # ── SHUTDOWN ──────────────────────────────────────────────────────────
    SHUTDOWN_PARALLEL_TIMEOUT_IN_MS: int = 5000

    # ── SOLVER ────────────────────────────────────────────────────────────
    SOLVER_GLOBAL_TYPE: SolverType = SolverType.Z3_INCREMENTAL
    SOLVER_ARGS: dict[str, object] = field(default_factory=dict)
    SOLVER_HIGH_LEVEL_SYMBOLIC_OBJECT_APPROACH: bool = False
    SOLVER_KEEP_TRACK_OF_ORIGINAL_CONSTRAINTS: bool = False

    # ── BUDGETS ───────────────────────────────────────────────────────────
    # None means "no budget".  Int > 0 means "hard cap".
    BUDGETS_FIXED_ACTUAL_CP: Optional[int] = None     # max choice-point depth
    BUDGETS_INCR_ACTUAL_CP: Optional[int] = None      # incremental CP for IDDFS
    BUDGETS_GLOBAL_TIME_IN_NANOSECONDS: Optional[int] = None
    BUDGETS_MAX_FAILS: Optional[int] = None
    BUDGETS_MAX_PATH_SOLUTIONS: Optional[int] = None
    BUDGETS_MAX_EXCEEDED_BUDGET: Optional[int] = None

    # ── ARRAYS ────────────────────────────────────────────────────────────
    ARRAYS_USE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS: bool = False
    ARRAYS_USE_EAGER_INDEXES_FOR_FREE_ARRAY_PRIMITIVE_ELEMENTS: bool = False
    ARRAYS_THROW_EXCEPTION_ON_OOB: bool = False

    # ── VALS (global default domains) ─────────────────────────────────────
    # None → unconstrained globally; per-call lb/ub overrides these.
    VALS_SYMSINT_LB: Optional[int] = None
    VALS_SYMSINT_UB: Optional[int] = None
    VALS_SYMSLONG_LB: Optional[int] = None
    VALS_SYMSLONG_UB: Optional[int] = None
    VALS_SYMSDOUBLE_LB: Optional[float] = None
    VALS_SYMSDOUBLE_UB: Optional[float] = None
    VALS_SYMSFLOAT_LB: Optional[float] = None
    VALS_SYMSFLOAT_UB: Optional[float] = None
    VALS_SYMSSHORT_LB: Optional[int] = None
    VALS_SYMSSHORT_UB: Optional[int] = None
    VALS_SYMSBYTE_LB: Optional[int] = None
    VALS_SYMSBYTE_UB: Optional[int] = None
    VALS_SYMSCHAR_LB: Optional[str] = None   # single-char string
    VALS_SYMSCHAR_UB: Optional[str] = None

    # ── LOG ────────────────────────────────────────────────────────────────
    LOG_TIME_FOR_EACH_PATH_SOLUTION: bool = False
    LOG_TIME_FOR_FIRST_PATH_SOLUTION: bool = False

    # ── CALLBACKS ─────────────────────────────────────────────────────────
    # Callables invoked with (path_solution | backtrack | fail | exceeded)
    # Use Optional[Callable] defaulting to None (no-op).
    CALLBACK_PATH_SOLUTION: Optional[Callable] = None
    CALLBACK_BACKTRACK: Optional[Callable] = None
    CALLBACK_FAIL: Optional[Callable] = None
    CALLBACK_EXCEEDED_BUDGET: Optional[Callable] = None
```

> **Implementation note**: `frozen=True` means the dict/tuple fields must use
> `field(default_factory=...)`.  Nested mutable containers (e.g. `SOLVER_ARGS`)
> should be stored as `types.MappingProxyType` inside `__post_init__` (use
> `object.__setattr__` to replace the mutable dict).

### 1.3 `MulibConfigBuilder` — fluent builder

```python
class MulibConfigBuilder:
    """Fluent builder that produces an immutable MulibConfig."""

    def __init__(self) -> None:
        self._kwargs: dict[str, object] = {}

    # ── TREE ──────────────────────────────────────────────────────────────
    def set_tree_indentation(self, v: str) -> "MulibConfigBuilder": ...
    def set_tree_enlist_leaves(self, v: bool) -> "MulibConfigBuilder": ...

    # ── SEARCH ────────────────────────────────────────────────────────────
    def set_search_main_strategy(self, v: SearchStrategy) -> "MulibConfigBuilder": ...
    def set_search_additional_parallel_strategies(self, *strategies: SearchStrategy) -> "MulibConfigBuilder": ...
    def set_search_choice_option_deque_type(self, v: ChoiceOptionDequeType) -> "MulibConfigBuilder": ...
    def set_search_activate_parallel_for(self, v: int) -> "MulibConfigBuilder": ...
    def set_search_concolic(self, v: bool) -> "MulibConfigBuilder": ...
    def set_search_allow_exceptions(self, v: bool) -> "MulibConfigBuilder": ...
    def set_search_label_result_value(self, v: bool) -> "MulibConfigBuilder": ...
    def set_search_randomize_selection(self, v: bool) -> "MulibConfigBuilder": ...

    # ── SOLVER ────────────────────────────────────────────────────────────
    def set_solver_global_type(self, v: SolverType) -> "MulibConfigBuilder": ...
    def put_solver_arg(self, key: str, val: object) -> "MulibConfigBuilder": ...
    def set_solver_high_level_symbolic_object_approach(self, v: bool) -> "MulibConfigBuilder": ...
    def set_solver_keep_track_of_original_constraints(self, v: bool) -> "MulibConfigBuilder": ...

    # ── BUDGETS ───────────────────────────────────────────────────────────
    def set_budget_fixed_actual_cp(self, v: int) -> "MulibConfigBuilder": ...
    def set_budget_incr_actual_cp(self, v: int) -> "MulibConfigBuilder": ...
    def set_budget_global_time_in_seconds(self, seconds: float) -> "MulibConfigBuilder":
        # converts to nanoseconds before storing
        ...
    def set_budget_max_fails(self, v: int) -> "MulibConfigBuilder": ...
    def set_budget_max_path_solutions(self, v: int) -> "MulibConfigBuilder": ...
    def set_budget_max_exceeded(self, v: int) -> "MulibConfigBuilder": ...

    # ── ARRAYS ────────────────────────────────────────────────────────────
    def set_arrays_use_eager_indexes_for_object_elements(self, v: bool) -> "MulibConfigBuilder": ...
    def set_arrays_use_eager_indexes_for_primitive_elements(self, v: bool) -> "MulibConfigBuilder": ...
    def set_arrays_throw_exception_on_oob(self, v: bool) -> "MulibConfigBuilder": ...

    # ── VALS ──────────────────────────────────────────────────────────────
    def set_vals_symsint_domain(self, lb: int, ub: int) -> "MulibConfigBuilder": ...
    def set_vals_symslong_domain(self, lb: int, ub: int) -> "MulibConfigBuilder": ...
    def set_vals_symsdouble_domain(self, lb: float, ub: float) -> "MulibConfigBuilder": ...
    def set_vals_symsfloat_domain(self, lb: float, ub: float) -> "MulibConfigBuilder": ...
    def set_vals_symsshort_domain(self, lb: int, ub: int) -> "MulibConfigBuilder": ...
    def set_vals_symsbyte_domain(self, lb: int, ub: int) -> "MulibConfigBuilder": ...
    def set_vals_symschar_domain(self, lb: str, ub: str) -> "MulibConfigBuilder": ...
    def assume_mulib_default_value_ranges(self) -> "MulibConfigBuilder":
        """Convenience: set full-width domains matching Java defaults."""
        ...

    # ── LOG ───────────────────────────────────────────────────────────────
    def set_log_time_for_each_path_solution(self, v: bool) -> "MulibConfigBuilder": ...
    def set_log_time_for_first_path_solution(self, v: bool) -> "MulibConfigBuilder": ...

    # ── CALLBACKS ─────────────────────────────────────────────────────────
    def set_callback_path_solution(self, cb: Callable) -> "MulibConfigBuilder": ...
    def set_callback_backtrack(self, cb: Callable) -> "MulibConfigBuilder": ...
    def set_callback_fail(self, cb: Callable) -> "MulibConfigBuilder": ...
    def set_callback_exceeded_budget(self, cb: Callable) -> "MulibConfigBuilder": ...

    # ── BUILD ─────────────────────────────────────────────────────────────
    def build(self) -> MulibConfig:
        """Validate and return an immutable MulibConfig."""
        # Validation examples:
        #   - BUDGETS_FIXED_ACTUAL_CP > 0 when not None
        #   - lb <= ub for every VALS domain pair
        #   - ARRAYS_USE_EAGER_INDEXES_FOR_PRIMITIVE implies OBJECT is also True
        return MulibConfig(**self._kwargs)
```

Each setter stores into `self._kwargs` and returns `self`.  `build()` does final
validation then unpacks into the frozen dataclass constructor.

---

## 2. `Solution` and `PathSolution` — `mulib_python/solution.py`

### 2.1 `Labels`

```python
@dataclass(frozen=True)
class Labels:
    """Name → concrete value mapping for a single execution path."""
    id_to_label: dict[str, object]  # frozen copy

    def get(self, name: str) -> object:
        return self.id_to_label[name]
```

### 2.2 `Solution`

`Solution` is a **lightweight value object** containing only the concrete values that
were extracted from the solver — no search-tree information.

```python
@dataclass(frozen=True)
class Solution:
    """Concrete values assigned to symbolic inputs on one satisfiable path.

    Attributes
    ----------
    labels:
        Named symbolic inputs remembered via ``remember()`` / ``remembered_free_*``.
    return_value:
        The concrete return value of the search region (may be ``None``).
    """
    labels: Labels
    return_value: object
```

### 2.3 `PathSolution`

`PathSolution` **extends** `Solution` with the additional path metadata needed for
test-case generation and tree inspection.

```python
@dataclass(frozen=True)
class PathSolution(Solution):
    """A solution enriched with search-tree path metadata.

    Extra Attributes
    ----------------
    path_constraints:
        The list of Constraint objects that were active when this leaf was reached.
    depth:
        Depth of the leaf node in the search tree.
    exceeded_budget:
        True if execution was cut off by a budget limit (this is an
        "exceeded-budget leaf" rather than a clean path solution).
    threw_exception:
        If SEARCH_ALLOW_EXCEPTIONS is True and the path threw, the exception
        is stored here (otherwise None).
    """
    path_constraints: tuple  # tuple[Constraint, ...]
    depth: int
    exceeded_budget: bool = False
    threw_exception: Optional[BaseException] = None
```

**Key difference**: `Solution` is what users get from `get_solution` /
`get_solutions` — it is minimal and easy to work with.  `PathSolution` is what users
get from `get_path_solutions` — it includes path metadata needed for test generation
and debugging.  `PathSolution` IS-A `Solution`, so all solution-handling code can
accept either.

---

## 3. `MulibContext` — `mulib_python/context.py`

### 3.1 Responsibility

Holds a **prepared search region** (a Python callable) and a `MulibConfig`.  Because
there is no bytecode transformation, preparation is trivial: store the callable and
config.  The context creates a fresh `SearchTree` + `SolverManager` for **each
invocation** (matching the Java behaviour where
`generateNewMulibExecutorManagerForPreInitializedContext` is called on every call to
`getPathSolutions` etc.).

### 3.2 Thread-local execution state

During execution the search engine needs to call back into `free_int`, `assume` etc.
These module-level functions read from a **thread-local** `_ExecutionState` that
`MulibContext` sets before invoking the search region and clears after.

```python
import threading
_state: threading.local = threading.local()

def _get_state() -> "_ExecutionState":
    s = getattr(_state, "current", None)
    if s is None:
        raise MulibRuntimeException("free_* / assume called outside a search region")
    return s
```

### 3.3 `_ExecutionState`

```python
@dataclass
class _ExecutionState:
    config: MulibConfig
    solver_manager: "SolverManager"          # from Phase 5/6
    remembered: dict[str, object]            # name → symbolic var (for remembered_free_*)
```

### 3.4 `MulibContext` class

```python
class MulibContext:
    def __init__(self, fn: Callable, config: MulibConfig) -> None:
        self._fn = fn
        self._config = config

    # ── Core execution helpers ─────────────────────────────────────────────
    def _run_search(self, args: tuple, kwargs: dict) -> list[PathSolution]:
        """Drive the search tree, collecting PathSolutions."""
        # 1. Create a fresh SolverManager (Z3 or other per config.SOLVER_GLOBAL_TYPE)
        # 2. Create a fresh SearchTree (depth-first / BFS / etc. per config.SEARCH_MAIN_STRATEGY)
        # 3. Push _ExecutionState onto threading.local
        # 4. While search tree has unvisited nodes:
        #      a. restore solver state to current node's constraint snapshot
        #      b. call self._fn(*args, **kwargs) wrapped in try/except Fail/Exception
        #      c. on normal return  → create PathSolution, fire CALLBACK_PATH_SOLUTION
        #      d. on Fail           → mark node as Fail leaf, fire CALLBACK_FAIL
        #      e. on budget exceeded → mark ExceededBudget, fire CALLBACK_EXCEEDED_BUDGET
        #      f. on backtrack      → fire CALLBACK_BACKTRACK
        #      g. check all budget limits; break early if any hit
        # 5. Pop _ExecutionState from threading.local
        # 6. Return collected PathSolutions
        ...

    # ── Public API (mirrors Java MulibContext) ─────────────────────────────
    def get_path_solutions(self, *args, **kwargs) -> list[PathSolution]:
        start = time.perf_counter_ns()
        result = self._run_search(args, kwargs)
        self._log_timing("get_path_solutions", start)
        return result

    def get_path_solution(self, *args, **kwargs) -> Optional[PathSolution]:
        results = self._run_search_up_to_n(1, args, kwargs)
        return results[0] if results else None

    def get_solutions(self, *args, **kwargs) -> list[Solution]:
        return [ps for ps in self.get_path_solutions(*args, **kwargs)
                if not ps.exceeded_budget and ps.threw_exception is None]

    def get_solution(self, *args, **kwargs) -> Optional[Solution]:
        sols = self.get_up_to_n_solutions(1, *args, **kwargs)
        return sols[0] if sols else None

    def get_up_to_n_solutions(self, n: int, *args, **kwargs) -> list[Solution]:
        path_sols = self._run_search_up_to_n(n, args, kwargs)
        return [Solution(ps.labels, ps.return_value) for ps in path_sols
                if not ps.exceeded_budget and ps.threw_exception is None]

    def _run_search_up_to_n(self, n: int, args, kwargs) -> list[PathSolution]:
        """Like _run_search but stops after n valid solutions."""
        ...

    def _log_timing(self, label: str, start_ns: int) -> None:
        elapsed_ms = (time.perf_counter_ns() - start_ns) / 1e6
        log.debug("MulibContext.%s took %.3f ms (config=%r)", label, elapsed_ms, self._config)
```

---

## 4. Top-Level API — `mulib_python/mulib.py`

This module exposes the public-facing functions that users call directly.

### 4.1 Module-level logger

```python
import logging
log = logging.getLogger("mulib")

def set_log_level(level: int) -> None:
    log.setLevel(level)
    for h in log.handlers:
        h.setLevel(level)
```

On module import, attach a single `logging.StreamHandler` (console) mirroring the
Java `ConsoleHandler` setup:

```python
_handler = logging.StreamHandler()
_handler.setLevel(logging.WARNING)
log.addHandler(_handler)
log.propagate = False
```

### 4.2 `fail()`

```python
from mulib_python.exceptions import Fail

def fail() -> None:
    """Prune the current execution path. Raises Fail."""
    raise Fail()
```

### 4.3 `assume(condition)`

```python
def assume(condition: bool | "SymbolicBool") -> None:
    """Assert condition holds; prune path if unsatisfiable."""
    state = _get_state()
    # If condition is a concrete False → immediate Fail
    if condition is False:
        raise Fail()
    if condition is True:
        return
    # Otherwise add symbolic constraint to solver, check SAT
    state.solver_manager.add_constraint(condition)
    if not state.solver_manager.is_sat():
        raise Fail()
```

### 4.4 Free-variable creation

Each function:
1. Looks up the global default domain from `_get_state().config`.
2. Merges with any per-call `lb`/`ub` (per-call wins).
3. Creates and registers a symbolic variable via `state.solver_manager.new_symbolic_int(name, lb, ub)` etc.
4. Returns the symbolic variable (an `Expression` node from Phase 1).

```python
def free_int(name: str, lb: Optional[int] = None, ub: Optional[int] = None):
    state = _get_state()
    lb = lb if lb is not None else state.config.VALS_SYMSINT_LB
    ub = ub if ub is not None else state.config.VALS_SYMSINT_UB
    return state.solver_manager.new_symbolic_int(name, lb, ub)

def free_bool(name: str):
    state = _get_state()
    return state.solver_manager.new_symbolic_bool(name)

def free_float(name: str, lb: Optional[float] = None, ub: Optional[float] = None):
    state = _get_state()
    lb = lb if lb is not None else state.config.VALS_SYMSFLOAT_LB
    ub = ub if ub is not None else state.config.VALS_SYMSFLOAT_UB
    return state.solver_manager.new_symbolic_float(name, lb, ub)

def free_double(name: str, lb: Optional[float] = None, ub: Optional[float] = None):
    state = _get_state()
    lb = lb if lb is not None else state.config.VALS_SYMSDOUBLE_LB
    ub = ub if ub is not None else state.config.VALS_SYMSDOUBLE_UB
    return state.solver_manager.new_symbolic_double(name, lb, ub)

def free_long(name: str, lb: Optional[int] = None, ub: Optional[int] = None):
    state = _get_state()
    lb = lb if lb is not None else state.config.VALS_SYMSLONG_LB
    ub = ub if ub is not None else state.config.VALS_SYMSLONG_UB
    return state.solver_manager.new_symbolic_long(name, lb, ub)

def free_short(name: str, lb: Optional[int] = None, ub: Optional[int] = None): ...
def free_byte(name: str, lb: Optional[int] = None, ub: Optional[int] = None): ...
def free_char(name: str, lb: Optional[str] = None, ub: Optional[str] = None): ...
```

> **Python note**: `long`, `short`, `byte`, `char` do not exist as Python primitives.
> All integer variants return a Python `int`-backed symbolic variable; the type tag
> (for bound checking) lives in the symbolic variable's metadata.

### 4.5 `remembered_free_int` (and other remembered variants)

```python
def remembered_free_int(name: str, lb: Optional[int] = None, ub: Optional[int] = None):
    """Return the same symbolic variable on every re-execution with the same name."""
    state = _get_state()
    if name in state.remembered:
        return state.remembered[name]
    sym = free_int(name, lb, ub)
    state.remembered[name] = sym
    return sym
```

All other `remembered_free_*` variants follow the same pattern.

### 4.6 High-level entry points

These are **module-level convenience functions** that construct a temporary
`MulibContext` internally — mirroring `Mulib.getPathSolutions(...)` etc. in Java.

```python
def _default_config() -> MulibConfig:
    return MulibConfig()   # all defaults

def get_solution(method: Callable, *args,
                 config: Optional[MulibConfig] = None,
                 **kwargs) -> Optional[Solution]:
    cfg = config or _default_config()
    return MulibContext(method, cfg).get_solution(*args, **kwargs)

def get_solutions(method: Callable, *args,
                  config: Optional[MulibConfig] = None,
                  **kwargs) -> list[Solution]:
    cfg = config or _default_config()
    return MulibContext(method, cfg).get_solutions(*args, **kwargs)

def get_path_solutions(method: Callable, *args,
                       config: Optional[MulibConfig] = None,
                       **kwargs) -> list[PathSolution]:
    cfg = config or _default_config()
    return MulibContext(method, cfg).get_path_solutions(*args, **kwargs)

def get_up_to_n_solutions(n: int, method: Callable, *args,
                          config: Optional[MulibConfig] = None,
                          **kwargs) -> list[Solution]:
    cfg = config or _default_config()
    return MulibContext(method, cfg).get_up_to_n_solutions(n, *args, **kwargs)

def get_mulib_context(method: Callable,
                      config: Optional[MulibConfig] = None) -> MulibContext:
    """Return a reusable MulibContext for repeated calls to the same search region."""
    cfg = config or _default_config()
    return MulibContext(method, cfg)
```

---

## 5. `Solution` vs `PathSolution` — Detailed Distinction

| Aspect | `Solution` | `PathSolution` |
|---|---|---|
| Inherits from | `object` | `Solution` |
| Contains | `labels`, `return_value` | everything in Solution + `path_constraints`, `depth`, `exceeded_budget`, `threw_exception` |
| Use case | Day-to-day result consumption | Test-case generation, debugging, tree visualisation |
| Returned by | `get_solution`, `get_solutions`, `get_up_to_n_solutions` | `get_path_solutions`, `get_path_solution` |
| Memory overhead | Minimal | Larger (keeps full constraint list) |

The API always returns `Solution` unless the caller explicitly asks for
`PathSolution`s.  `PathSolution` IS-A `Solution` so any code that works on
`Solution` also works on `PathSolution`.

---

## 6. Logging Setup

```
mulib           (root logger for this library)
├── mulib.config
├── mulib.context
└── mulib.search
```

- Default level: `WARNING` (silent unless explicitly lowered).
- A single `StreamHandler` attached at module import in `mulib.py` (not propagated to
  the root Python logger — `log.propagate = False`).
- `set_log_level(level)` sets both the logger and its handler, matching the Java
  pattern.
- Each phase that creates a logger uses `logging.getLogger("mulib.<submodule>")`.
- Timing logs use `logging.DEBUG`.  Budget/path-solution count logs use
  `logging.INFO`.  Misconfiguration warnings use `logging.WARNING`.

---

## 7. `__init__.py` public surface

```python
# mulib_python/__init__.py
from mulib_python.exceptions import Fail, MulibException, MisconfigurationException
from mulib_python.config import MulibConfig, MulibConfigBuilder, SearchStrategy, SolverType, ChoiceOptionDequeType
from mulib_python.solution import Solution, PathSolution, Labels
from mulib_python.context import MulibContext
from mulib_python.mulib import (
    log, set_log_level, fail, assume,
    free_int, free_bool, free_float, free_double,
    free_long, free_short, free_byte, free_char,
    remembered_free_int, remembered_free_bool,
    remembered_free_float, remembered_free_double,
    remembered_free_long, remembered_free_short,
    remembered_free_byte, remembered_free_char,
    get_solution, get_solutions, get_path_solutions,
    get_up_to_n_solutions, get_mulib_context,
)

__all__ = [
    "Fail", "MulibException", "MisconfigurationException",
    "MulibConfig", "MulibConfigBuilder",
    "SearchStrategy", "SolverType", "ChoiceOptionDequeType",
    "Solution", "PathSolution", "Labels",
    "MulibContext",
    "log", "set_log_level", "fail", "assume",
    "free_int", "free_bool", "free_float", "free_double",
    "free_long", "free_short", "free_byte", "free_char",
    "remembered_free_int", "remembered_free_bool",
    "remembered_free_float", "remembered_free_double",
    "remembered_free_long", "remembered_free_short",
    "remembered_free_byte", "remembered_free_char",
    "get_solution", "get_solutions", "get_path_solutions",
    "get_up_to_n_solutions", "get_mulib_context",
]
```

---

## 8. Validation & Tests

### `config.py` — Unit tests

- Builder default produces correct field values.
- `set_budget_global_time_in_seconds(5)` → `BUDGETS_GLOBAL_TIME_IN_NANOSECONDS == 5_000_000_000`.
- `set_vals_symsint_domain(10, 5)` raises `MisconfigurationException` (lb > ub).
- `assume_mulib_default_value_ranges()` sets all domains to full-width values.
- Frozen dataclass: mutation raises `FrozenInstanceError`.

### `mulib.py` — Integration smoke tests

```python
def test_free_int_basic():
    def search_region():
        x = free_int("x", lb=0, ub=10)
        assume(x > 5)
        return x

    sols = get_solutions(search_region)
    for s in sols:
        assert s.labels.get("x") > 5

def test_fail_prunes_path():
    def search_region():
        x = free_int("x", lb=0, ub=1)
        if x == 0:
            fail()
        return x

    sols = get_solutions(search_region)
    assert all(s.labels.get("x") != 0 for s in sols)

def test_remembered_free_int_same_name():
    def search_region():
        a = remembered_free_int("v", lb=0, ub=100)
        b = remembered_free_int("v", lb=0, ub=100)
        # a and b must be the same symbolic object
        assume(a is b)
        return a

    sols = get_solutions(search_region)
    assert len(sols) > 0

def test_get_solution_none_when_unsat():
    def search_region():
        x = free_int("x", lb=0, ub=0)
        assume(x > 0)
        return x

    sol = get_solution(search_region)
    assert sol is None

def test_path_solution_has_constraints():
    def search_region():
        x = free_int("x", lb=0, ub=1)
        return x

    psols = get_path_solutions(search_region)
    assert all(hasattr(ps, "path_constraints") for ps in psols)
```

---

## 9. Implementation Order

1. **`config.py`** — `SearchStrategy`, `SolverType`, `ChoiceOptionDequeType` enums,
   `MulibConfig` frozen dataclass, `MulibConfigBuilder`.
2. **`solution.py`** — `Labels`, `Solution`, `PathSolution`.
3. **`mulib.py`** — `_ExecutionState`, thread-local helpers, `fail()`, `assume()`,
   all `free_*` and `remembered_free_*` functions.  Top-level `get_*` wrappers left
   as stubs until `MulibContext` is ready.
4. **`context.py`** — `MulibContext` wiring `_run_search` to the search engine from
   earlier phases.
5. **Finish `mulib.py`** — fill in stubs for `get_solution`, `get_solutions`, etc.
6. **`__init__.py`** — re-exports.
7. **Tests** — unit + integration as described in Section 8.
