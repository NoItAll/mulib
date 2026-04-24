# Phase 6 Implementation Plan: Execution Engine

## Overview

This document is the **complete, self-contained specification** for the Phase 6 implementation
agent.  It covers every file to create, every class/method signature, all Python-specific design
decisions, and cross-cutting notes on correctness.

### Prerequisites (already implemented by earlier phases)

```
mulib_python/
├── expressions.py          — immutable AST nodes (Expression, Sum, Sub, Mul, Div, Mod, Neg,
│                             BitwiseAnd/Or/Xor, ShiftLeft/Right/LogicalShiftRight, ExpressionIte)
├── constraints.py          — immutable constraint nodes (Constraint, And, Or, Not, Xor,
│                             Implication, Equivalence, Lt, Lte, Eq, BoolIte, TRUE, FALSE,
│                             ArrayConstraint, PartnerClassObjectConstraint, …)
├── exceptions.py           — MulibException hierarchy including Fail, ExceededBudgetException,
│                             MulibRuntimeException, MulibIllegalStateException, …
├── substitutions/          — Sint/Sbool/Sbyte/Schar/Sshort/Slong/Sdouble/Sfloat (Phase 2)
│   ├── markers.py              Substituted, Sym, Conc
│   ├── primitives/
│   │   ├── sprimitive.py       Sprimitive, SymSprimitive, SymSprimitiveLeaf
│   │   ├── snumber.py          Snumber, ConcSnumber, SymSnumber, AbstractSnumber, Sfpnumber
│   │   ├── sint.py             Sint + sub-types; Sbool + sub-types; Sbyte; Schar; Sshort
│   │   ├── slong.py            Slong + sub-types
│   │   ├── sdouble.py          Sdouble + sub-types
│   │   └── sfloat.py           Sfloat + sub-types
│   └── value_factory.py    — ValueFactory ABC + SymbolicValueFactory
├── solver_manager.py       — SolverManager ABC (Phase 3)
├── solution.py             — Solution, Labels (Phase 3)
└── search/                 — search infrastructure (Phase 5)
    ├── strategy.py             SearchStrategy enum
    ├── trees/
    │   ├── choice.py           Choice + ChoiceOption
    │   ├── search_tree.py      SearchTree, AccumulatedChoiceOptionConstraints
    │   ├── choice_option_deque.py
    │   ├── path_solution.py
    │   ├── fail.py
    │   └── exceeded_budget.py
    ├── budget/
    │   ├── execution_budget_manager.py
    │   └── global_execution_budget_manager.py
    └── choice_points/
        ├── backtrack.py        Backtrack singleton exception
        └── choice_point_factory.py  ChoicePointFactory ABC + SymbolicChoicePointFactory
```

---

## 1. Files to Create

```
mulib_python/executor/
├── __init__.py                         Public re-exports
├── concolic_containers.py              ConcolicMathematicalContainer + ConcolicConstraintContainer
├── static_variables.py                 StaticVariables (executor-local "static field" store)
├── mulib_executor.py                   MulibExecutor ABC (interface)
├── symbolic_execution.py               SymbolicExecution (thread-local singleton)
├── calculation_factory.py              CalculationFactory ABC
├── symbolic_calculation_factory.py     SymbolicCalculationFactory
├── concolic_calculation_factory.py     ConcolicCalculationFactory
├── abstract_mulib_executor.py          AbstractMulibExecutor (main search loop)
├── generic_executor.py                 GenericExecutor (DFS/BFS/IDDFS/DSAS/IDDSAS)
├── executor_manager.py                 MulibExecutorManager ABC
├── single_executor_manager.py          SingleExecutorManager
└── multi_executors_manager.py          MultiExecutorsManager
```

---

## 2. `concolic_containers.py`

**Purpose:** Hold a paired (symbolic_value, concrete_label) for concolic execution.
These are the Python equivalents of Java's `ConcolicMathematicalContainer` and
`ConcolicConstraintContainer`.  They are referenced by `ConcolicCalculationFactory`
and by helpers that strip or extract the symbolic/concrete halves.

```python
# mulib_python/executor/concolic_containers.py
from __future__ import annotations
from typing import Any

from mulib_python.substitutions.primitives.snumber import SymSnumber, ConcSnumber
from mulib_python.substitutions.primitives.sint import Sbool


class ConcolicMathematicalContainer:
    """Wraps a SymSnumber (symbolic expression) together with a ConcSnumber (concrete label)
    produced during a concolic run.
    """
    __slots__ = ("sym", "conc")

    def __init__(self, sym: SymSnumber, conc: ConcSnumber) -> None:
        self.sym = sym
        self.conc = conc

    # ---- helpers (static-method equivalents) --------------------------------

    @staticmethod
    def try_get_sym_from_concolic(value: Any) -> Any:
        """If *value* wraps a ConcolicMathematicalContainer, return its symbolic part;
        otherwise return *value* unchanged."""
        from mulib_python.substitutions.primitives.snumber import AbstractSnumber
        if isinstance(value, AbstractSnumber) and hasattr(value, "_concolic"):
            return value._concolic.sym
        return value

    @staticmethod
    def get_conc_numeric_from_concolic(value: Any) -> ConcSnumber:
        """Extract the concrete label from a concolic number; raises if absent."""
        from mulib_python.substitutions.primitives.snumber import AbstractSnumber
        if isinstance(value, AbstractSnumber) and hasattr(value, "_concolic"):
            return value._concolic.conc
        if isinstance(value, ConcSnumber):
            return value
        raise TypeError(f"Not a concolic number: {value!r}")


class ConcolicConstraintContainer:
    """Wraps a SymSbool (symbolic constraint) together with a ConcSbool (concrete boolean
    label) produced during a concolic run.
    """
    __slots__ = ("sym", "conc")

    def __init__(self, sym: "Sbool.SymSbool", conc: "Sbool.ConcSbool") -> None:
        self.sym = sym
        self.conc = conc

    @staticmethod
    def try_get_sym_from_concolic(value: Any) -> Any:
        """Return the symbolic part of a concolic Sbool, or *value* unchanged."""
        if isinstance(value, Sbool) and hasattr(value, "_concolic"):
            return value._concolic.sym
        return value

    @staticmethod
    def get_conc_sbool_from_concolic(value: Any) -> "Sbool.ConcSbool":
        """Extract the concrete boolean label from a concolic Sbool."""
        if isinstance(value, Sbool) and hasattr(value, "_concolic"):
            return value._concolic.conc
        if isinstance(value, Sbool.ConcSbool):
            return value
        raise TypeError(f"Not a concolic Sbool: {value!r}")
```

### Design note on "concolic wrapper" representation

In Python the cleanest approach is to give `AbstractSnumber` (and `Sbool`) an optional
`_concolic: ConcolicMathematicalContainer | ConcolicConstraintContainer | None`
slot.  `ConcolicCalculationFactory` sets this slot when building concolic results.
This avoids a parallel subclass explosion and keeps the `try_get_sym_from_concolic`
helpers simple.  The Phase 2 agent should add `_concolic = None` to the base
`AbstractSnumber.__slots__` and `Sbool.__slots__` if not already present; Phase 6 fills
the slot only in concolic mode.

---

## 3. `static_variables.py`

**Purpose:** Give each `MulibExecutor` its own dictionary of "static" variables.
In Java, static fields of the transformed search-region class are intercepted via
bytecode transformation.  Python has no bytecode transformation requirement; instead,
user code calls `se.get_static_field(name)` and `se.set_static_field(name, value)`
explicitly (or the generator infrastructure does so).  The `StaticVariables` object
manages per-executor copies so that different executors cannot interfere.

```python
# mulib_python/executor/static_variables.py
from __future__ import annotations
from typing import Any, Dict, Optional


class StaticVariables:
    """Maintains per-executor copies of "static" variables for the search region.

    A *prototype* instance is created once with the initial values (possibly
    symbolic).  Each executor calls ``copy_from_prototype()`` to receive its own
    independent copy; actual deep-copying of symbolic objects is deferred to the
    first read access, driven by a ``MulibValueCopier``.
    """

    __slots__ = ("_initial_values", "_current_values", "_copier")

    def __init__(
        self,
        initial_values: Dict[str, Any],
        *,
        _is_prototype: bool = True,
    ) -> None:
        # field_name -> initial (prototype) value
        self._initial_values: Dict[str, Any] = initial_values
        # field_name -> per-run value; populated lazily on first read
        self._current_values: Optional[Dict[str, Any]] = None
        self._copier: Any = None  # set by the executor before each run

    # ------------------------------------------------------------------
    # Public interface
    # ------------------------------------------------------------------

    def set_copier(self, copier: Any) -> None:
        """Called by the executor before invoking the search region."""
        self._copier = copier

    def get_static_field(self, field_name: str) -> Any:
        """Return the current value of *field_name*, copy-on-first-access."""
        if self._current_values is None:
            self._current_values = {}
        if field_name not in self._current_values:
            prototype = self._initial_values.get(field_name)
            self._current_values[field_name] = (
                self._copier.copy(prototype) if self._copier is not None else prototype
            )
        return self._current_values[field_name]

    def set_static_field(self, field_name: str, value: Any) -> None:
        if self._current_values is None:
            self._current_values = {}
        self._current_values[field_name] = value

    def reset(self) -> None:
        """Called after each search-region invocation to discard run-local values."""
        if self._current_values is not None:
            self._current_values.clear()

    def copy_from_prototype(self) -> "StaticVariables":
        """Return a new instance sharing the same initial values."""
        return StaticVariables(self._initial_values, _is_prototype=False)
```

### Key design decisions

- `_initial_values` is **shared** (read-only) across all copies; this is safe because
  values are never mutated in-place, only replaced.
- The `MulibValueCopier` referenced here is not yet defined; Phase 6 should provide
  a simple `MulibValueCopier` class (see §11 below) that deep-copies symbolic objects
  so that a fresh execution run starts from a clean symbolic state.

---

## 4. `mulib_executor.py` — `MulibExecutor` ABC

**Purpose:** Define the interface contract that `AbstractMulibExecutor` implements and
that `SymbolicExecution` calls into.  Keeping it as a separate ABC avoids circular
imports.

```python
# mulib_python/executor/mulib_executor.py
from __future__ import annotations
import abc
from typing import Any, Dict, List, Optional

from mulib_python.constraints import Constraint
from mulib_python.search.trees.choice import Choice
from mulib_python.search.trees.path_solution import PathSolution
from mulib_python.solution import Solution
from mulib_python.substitutions.primitives.sprimitive import Sprimitive
from mulib_python.search.strategy import SearchStrategy


class MulibExecutor(abc.ABC):
    """Interface contract for a single search executor."""

    # ---- constraint management ------------------------------------------

    @abc.abstractmethod
    def add_new_constraint(self, constraint: Constraint) -> None:
        """Add *constraint* to the current scope and to the live ChoiceOption."""

    @abc.abstractmethod
    def add_constraint_after_backtracking_point(self, constraint: Constraint) -> None:
        """Push a new solver scope and add *constraint* into it."""

    @abc.abstractmethod
    def check_with_new_constraint(self, constraint: Constraint) -> bool:
        """Return True iff adding *constraint* keeps the solver satisfiable
        (does NOT persist the constraint)."""

    @abc.abstractmethod
    def is_satisfiable(self) -> bool: ...

    # ---- decision procedure during execution ----------------------------

    @abc.abstractmethod
    def decide_on_next_choice_option_during_execution(
        self,
        options: List[Choice.ChoiceOption],
    ) -> Optional[Choice.ChoiceOption]:
        """Called by ChoicePointFactory when a new choice is encountered *during*
        execution.  Returns the chosen option or None to signal backtrack."""

    # ---- search tree bookkeeping ----------------------------------------

    @abc.abstractmethod
    def notify_new_choice(
        self,
        depth: int,
        choice_options: List[Choice.ChoiceOption],
    ) -> None: ...

    # ---- labeling helpers -----------------------------------------------

    @abc.abstractmethod
    def label(self, var: Any) -> Any: ...

    @abc.abstractmethod
    def concretize(self, var: Any) -> Any: ...

    # ---- remembered primitives ------------------------------------------

    @abc.abstractmethod
    def remember_sprimitive(self, name: str, remembered: Sprimitive) -> None: ...

    # ---- static variables -----------------------------------------------

    @abc.abstractmethod
    def get_static_field(self, field_name: str) -> Any: ...

    @abc.abstractmethod
    def set_static_field(self, field_name: str, value: Any) -> None: ...

    # ---- executor lifecycle ---------------------------------------------

    @abc.abstractmethod
    def get_path_solution(self) -> Optional[PathSolution]:
        """Drive the search loop and return the next PathSolution, or None."""

    @abc.abstractmethod
    def terminate(self) -> None: ...

    @abc.abstractmethod
    def get_search_strategy(self) -> SearchStrategy: ...

    @abc.abstractmethod
    def get_executor_manager(self) -> Any: ...  # MulibExecutorManager
```

---

## 5. `symbolic_execution.py` — `SymbolicExecution`

**Purpose:** Thread-local singleton representing the *current* execution run.
Every symbolic operation in the search region goes through this object.
It holds the predetermined path (the trail of ChoiceOptions from the root to the
targeted leaf), the per-run free-variable counters, the named-variable registry,
and proxy methods forwarding to the `CalculationFactory` and `ChoicePointFactory`.

### 5.1 Thread-local storage

```python
import threading
_se_local: threading.local = threading.local()
```

`SymbolicExecution.get()` returns `_se_local.current` (or `None`).
`SymbolicExecution._set()` writes `_se_local.current = self`.
`SymbolicExecution.remove()` deletes the attribute.

This is exactly equivalent to Java's `ThreadLocal<SymbolicExecution>`.

### 5.2 Constructor

```python
class SymbolicExecution:
    def __init__(
        self,
        executor: "MulibExecutor",
        choice_point_factory: "ChoicePointFactory",
        value_factory: "ValueFactory",
        calculation_factory: "CalculationFactory",
        navigate_to: "Choice.ChoiceOption",
        execution_budget_manager_prototype: "ExecutionBudgetManager",
        next_partner_class_object_nr: int,
        config: "MulibConfig",
    ) -> None:
```

Key fields set in `__init__`:

| Python field | Java counterpart | Type |
|---|---|---|
| `self._executor` | `mulibExecutor` | `MulibExecutor` |
| `self._choice_point_factory` | `choicePointFactory` | `ChoicePointFactory` |
| `self._value_factory` | `valueFactory` | `ValueFactory` |
| `self._calculation_factory` | `calculationFactory` | `CalculationFactory` |
| `self._predetermined_path` | `predeterminedPath` | `collections.deque[ChoiceOption]` |
| `self._current_choice_option` | `currentChoiceOption` | `ChoiceOption` |
| `self._execution_budget_manager` | `executionBudgetManager` | `ExecutionBudgetManager` |
| `self._named_variables` | (inline `addNamedVariable` map) | `dict[str, Substituted]` |
| `self._next_number_sym_sint_leaf` | `nextNumberSymSintLeaf` | `int` |
| `self._next_number_sym_sdouble_leaf` | `nextNumberSymSdoubleLeaf` | `int` |
| `self._next_number_sym_sfloat_leaf` | `nextNumberSymSfloatLeaf` | `int` |
| `self._next_number_sym_sbool_leaf` | `nextNumberSymSboolLeaf` | `int` |
| `self._next_number_sym_slong_leaf` | `nextNumberSymSlongLeaf` | `int` |
| `self._next_number_sym_sshort_leaf` | `nextNumberSymSshortLeaf` | `int` |
| `self._next_number_sym_sbyte_leaf` | `nextNumberSymSbyteLeaf` | `int` |
| `self._next_number_sym_schar_leaf` | `nextNumberSymScharLeaf` | `int` |
| `self._next_number_initialized_sym_object` | `nextNumberInitializedSymObject` | `int` |
| `self._config` | `config` | `MulibConfig` |

After setting all fields, the constructor calls `self._set()` to register this instance
in the thread-local.

### 5.3 Predetermined path

```python
# Built in __init__ by SearchTree.get_path_to(navigate_to)
self._predetermined_path: collections.deque[ChoiceOption] = SearchTree.get_path_to(navigate_to)
self._current_choice_option: ChoiceOption = self._predetermined_path[0]  # peek front
```

`SearchTree.get_path_to(option)` returns the deque **from root down to** `option`
(inclusive), so the first pop in `transition_to_next_choice_option_and_check_if_on_known_path()`
removes the outermost already-consumed option and peeks at the next one.

```python
def transition_to_next_choice_option_and_check_if_on_known_path(self) -> bool:
    """Pop the current head of the predetermined path.

    Returns True  ↔  we are STILL on the known path (more options ahead).
    Returns False ↔  we have just left the known path (new territory).
    """
    if self._predetermined_path:
        self._predetermined_path.popleft()
    # After popping, if the deque is now empty we just stepped off the known path.
    if not self._predetermined_path:
        return False   # new territory
    self._current_choice_option = self._predetermined_path[0]
    return True

def next_is_on_known_path(self) -> bool:
    """True iff at least two entries remain in the predetermined path."""
    return len(self._predetermined_path) > 1
```

**Critical invariant:** `_predetermined_path` starts with **all** ChoiceOptions from root
to `navigate_to` (inclusive).  The ChoicePointFactory pops one entry at each choice
point it encounters.  When `next_is_on_known_path()` becomes `False`, the
ChoicePointFactory knows it has reached new territory and must consult the executor
for a new decision.

### 5.4 Counter management

Each symbolic leaf variable has a globally unique integer ID per run.  The counters
are simple integers that are incremented and returned by:

```python
def get_next_number_sym_sint_leaf(self) -> int:
    n = self._next_number_sym_sint_leaf
    self._next_number_sym_sint_leaf += 1
    return n

# … identical methods for:
#   get_next_number_sym_sdouble_leaf()
#   get_next_number_sym_sfloat_leaf()
#   get_next_number_sym_sbool_leaf()
#   get_next_number_sym_slong_leaf()
#   get_next_number_sym_sshort_leaf()
#   get_next_number_sym_sbyte_leaf()
#   get_next_number_sym_schar_leaf()
#   get_next_number_initialized_sym_object()
```

These are called exclusively from `ValueFactory` methods (e.g. `sym_sint(se)`)
to give every leaf a unique, per-run name such as `"Sint_3"`.

### 5.5 `bool_choice(sbool)` — the core decision procedure

This is the method that the **instrumented search-region code** calls whenever it
needs to branch on a symbolic boolean (e.g. a translated `if`-statement).

```python
def bool_choice(self, b: "Sbool") -> bool:
    return self._choice_point_factory.bool_choice(self, b)
```

`SymbolicChoicePointFactory.bool_choice(se, b)` (Phase 5) does:

1. Check whether `b` is a concrete `ConcSbool`; if so, return its value immediately
   (no new choice).
2. Call `se.transition_to_next_choice_option_and_check_if_on_known_path()`.
   - If still on known path → read the option index from `se.get_current_choice_option()`
     and return `True` if option index == 0, `False` if option index == 1.
   - If on new territory → ask the executor via
     `se._executor.decide_on_next_choice_option_during_execution(options)`.
     The executor checks satisfiability for each candidate, registers the other as a
     future exploration target in the deque, and returns the chosen option.  Returning
     `None` triggers `Backtrack`.

The same pattern applies to all comparison-based choice methods (`lt_choice`,
`gt_choice`, `eq_choice`, etc.); they all delegate to `_choice_point_factory`.

### 5.6 Free variable creation

`SymbolicExecution` exposes convenience factory methods that forward to
`self._value_factory`:

```python
def sym_sint(self) -> "Sint":
    return self._value_factory.sym_sint(self)

def sym_sbool(self) -> "Sbool":
    return self._value_factory.sym_sbool(self)

# … identical for sym_sdouble, sym_sfloat, sym_slong, sym_sshort,
#               sym_sbyte, sym_schar

# Bounded variants
def sym_sint_bounded(self, lb: "Sint", ub: "Sint") -> "Sint":
    return self._value_factory.sym_sint(self, lb, ub)
# … etc.

# Concrete wrappers
def conc_sint(self, i: int) -> "Sint":
    from mulib_python.substitutions.primitives.sint import Sint
    return Sint.conc_sint(i)
# … etc.
```

### 5.7 Named variables and `remember`

```python
def _add_named_variable(self, key: str, value: "Substituted") -> None:
    """Register a named variable so that labeling can retrieve it by name."""
    self._named_variables[key] = value
    self._executor.remember_sprimitive(key, value)  # forwarded to executor

def named_sym_sint(self, identifier: str) -> "Sint":
    result = self.sym_sint()
    self._add_named_variable(identifier, result)
    return result

# … identical helpers for every type: named_sym_sbool, named_sym_sdouble, etc.
# … plus array variants: named_sint_sarray, named_sdouble_sarray, etc.
```

### 5.8 `assume(sbool)` logic

```python
def assume(self, s: "Sbool") -> None:
    """Assert *s* must hold; prune the path if not satisfiable.

    - If *s* is a concrete False → raise Fail immediately.
    - If *s* is a concrete True  → no-op.
    - If symbolic: extract the symbolic part (strip concolic wrapper if present),
      then call add_new_constraint(sym_part).
    """
    from mulib_python.substitutions.primitives.sint import Sbool
    from mulib_python.exceptions import Fail
    from mulib_python.executor.concolic_containers import ConcolicConstraintContainer

    if isinstance(s, Sbool.ConcSbool):
        if not s.is_true():
            raise Fail()
        return   # concrete True — no constraint needed
    # Symbolic path
    sym_part = ConcolicConstraintContainer.try_get_sym_from_concolic(s)
    self.add_new_constraint(sym_part)

def check_assume(self, s: "Sbool") -> "Sbool":
    """Call assume(s) and return s (convenience for in-expression use)."""
    self.assume(s)
    return s
```

### 5.9 `add_new_constraint`

```python
def add_new_constraint(self, c: "Constraint") -> None:
    """Persist a new constraint on the current (non-known-path) choice option."""
    assert not self.next_is_on_known_path()
    self._executor.add_new_constraint(c)
```

The assertion enforces that constraints are only added once we have left the known path
(i.e. during the new execution territory).  If this is violated the executor is in an
illegal state.

### 5.10 Static field accessors

```python
def get_static_field(self, field_name: str) -> Any:
    return self._executor.get_static_field(field_name)

def set_static_field(self, field_name: str, value: Any) -> None:
    self._executor.set_static_field(field_name, value)
```

### 5.11 Thread-local registry

```python
@staticmethod
def get() -> Optional["SymbolicExecution"]:
    return getattr(_se_local, "current", None)

def _set(self) -> None:
    _se_local.current = self

@staticmethod
def remove() -> None:
    try:
        del _se_local.current
    except AttributeError:
        pass

def is_in_search(self) -> "Sbool":
    from mulib_python.substitutions.primitives.sint import Sbool
    return Sbool.conc_sbool(SymbolicExecution.get() is self)
```

### 5.12 Arithmetic/comparison/bitwise proxy methods

`SymbolicExecution` also exposes a large set of methods that simply forward to
`self._calculation_factory`:

```python
# ---- arithmetic (int) --------------------------------------------------
def add(self, lhs: "Sint", rhs: "Sint") -> "Sint":
    return self._calculation_factory.add(self, lhs, rhs)
def sub(self, lhs: "Sint", rhs: "Sint") -> "Sint":
    return self._calculation_factory.sub(self, lhs, rhs)
def mul(self, lhs: "Sint", rhs: "Sint") -> "Sint":
    return self._calculation_factory.mul(self, lhs, rhs)
def div(self, lhs: "Sint", rhs: "Sint") -> "Sint":
    return self._calculation_factory.div(self, lhs, rhs)
def mod(self, lhs: "Sint", rhs: "Sint") -> "Sint":
    return self._calculation_factory.mod(self, lhs, rhs)
def neg_int(self, i: "Sint") -> "Sint":
    return self._calculation_factory.neg(self, i)

# ---- (double, float, long variants follow same pattern) ----------------

# ---- comparison --------------------------------------------------------
def lt(self, lhs: "Sint", rhs: "Sint") -> "Sbool":
    return self._calculation_factory.lt(self, lhs, rhs)
def lte(self, lhs: "Sint", rhs: "Sint") -> "Sbool":
    return self._calculation_factory.lte(self, lhs, rhs)
def gt(self, lhs: "Sint", rhs: "Sint") -> "Sbool":
    return self._calculation_factory.lt(self, rhs, lhs)   # reversed
def gte(self, lhs: "Sint", rhs: "Sint") -> "Sbool":
    return self._calculation_factory.lte(self, rhs, lhs)  # reversed
def eq_int(self, lhs: "Sint", rhs: "Sint") -> "Sbool":
    return self._calculation_factory.eq(self, lhs, rhs)
# … Slong/Sdouble/Sfloat variants …

# ---- boolean logic -----------------------------------------------------
def and_(self, lhs: "Sbool", rhs: "Sbool") -> "Sbool":
    return self._calculation_factory.and_(self, lhs, rhs)
def or_(self, lhs: "Sbool", rhs: "Sbool") -> "Sbool":
    return self._calculation_factory.or_(self, lhs, rhs)
def not_(self, b: "Sbool") -> "Sbool":
    return self._calculation_factory.not_(self, b)
def xor(self, lhs: "Sbool", rhs: "Sbool") -> "Sbool":
    return self._calculation_factory.xor(self, lhs, rhs)
def implies(self, lhs: "Sbool", rhs: "Sbool") -> "Sbool":
    return self._calculation_factory.implies(self, lhs, rhs)

# ---- casts -------------------------------------------------------------
def i2l(self, i: "Sint") -> "Slong":   return self._calculation_factory.i2l(self, i)
def i2f(self, i: "Sint") -> "Sfloat":  return self._calculation_factory.i2f(self, i)
def i2d(self, i: "Sint") -> "Sdouble": return self._calculation_factory.i2d(self, i)
def i2b(self, i: "Sint") -> "Sbyte":   return self._calculation_factory.i2b(self, i)
def i2s(self, i: "Sint") -> "Sshort":  return self._calculation_factory.i2s(self, i)
def i2c(self, i: "Sint") -> "Schar":   return self._calculation_factory.i2c(self, i)
def l2i(self, l: "Slong") -> "Sint":   return self._calculation_factory.l2i(self, l)
def l2f(self, l: "Slong") -> "Sfloat": return self._calculation_factory.l2f(self, l)
def l2d(self, l: "Slong") -> "Sdouble":return self._calculation_factory.l2d(self, l)
def f2i(self, f: "Sfloat") -> "Sint":  return self._calculation_factory.f2i(self, f)
def f2l(self, f: "Sfloat") -> "Slong": return self._calculation_factory.f2l(self, f)
def f2d(self, f: "Sfloat") -> "Sdouble":return self._calculation_factory.f2d(self, f)
def d2i(self, d: "Sdouble") -> "Sint": return self._calculation_factory.d2i(self, d)
def d2l(self, d: "Sdouble") -> "Slong":return self._calculation_factory.d2l(self, d)
def d2f(self, d: "Sdouble") -> "Sfloat":return self._calculation_factory.d2f(self, d)

# ---- bitwise (int) -----------------------------------------------------
def ishl(self, i0: "Sint", i1: "Sint") -> "Sint":  return self._calculation_factory.ishl(self, i0, i1)
def ishr(self, i0: "Sint", i1: "Sint") -> "Sint":  return self._calculation_factory.ishr(self, i0, i1)
def iushr(self, i0: "Sint", i1: "Sint") -> "Sint": return self._calculation_factory.iushr(self, i0, i1)
def ixor(self, i0: "Sint", i1: "Sint") -> "Sint":  return self._calculation_factory.ixor(self, i0, i1)
def ior(self, i0: "Sint", i1: "Sint") -> "Sint":   return self._calculation_factory.ior(self, i0, i1)
def iand(self, i0: "Sint", i1: "Sint") -> "Sint":  return self._calculation_factory.iand(self, i0, i1)
# ---- bitwise (long) ----------------------------------------------------
def lshl(self, l: "Slong", i: "Sint") -> "Slong":  return self._calculation_factory.lshl(self, l, i)
def lshr(self, l: "Slong", i: "Sint") -> "Slong":  return self._calculation_factory.lshr(self, l, i)
def lushr(self, l: "Slong", i: "Sint") -> "Slong": return self._calculation_factory.lushr(self, l, i)
def lxor(self, l0: "Slong", l1: "Slong") -> "Slong": return self._calculation_factory.lxor(self, l0, l1)
def lor(self, l0: "Slong", l1: "Slong") -> "Slong":  return self._calculation_factory.lor(self, l0, l1)
def land(self, l0: "Slong", l1: "Slong") -> "Slong": return self._calculation_factory.land(self, l0, l1)

# ---- cmp (three-way compare) -------------------------------------------
def cmp_long(self, lhs: "Slong", rhs: "Slong") -> "Sint":
    return self._calculation_factory.cmp(self, lhs, rhs)
def cmp_double(self, lhs: "Sdouble", rhs: "Sdouble") -> "Sint":
    return self._calculation_factory.cmp(self, lhs, rhs)
def cmp_float(self, lhs: "Sfloat", rhs: "Sfloat") -> "Sint":
    return self._calculation_factory.cmp(self, lhs, rhs)

# ---- labeling / concretize ---------------------------------------------
def label(self, var: Any) -> Any:
    return self._executor.label(var)
def concretize(self, var: Any) -> Any:
    return self._executor.concretize(var)

# ---- choice-point shortcuts (delegating to choice_point_factory) -------
def lt_choice(self, lhs: "Sint", rhs: "Sint") -> bool:
    return self._choice_point_factory.lt_choice(self, lhs, rhs)
def gt_choice(self, lhs: "Sint", rhs: "Sint") -> bool:
    return self._choice_point_factory.gt_choice(self, lhs, rhs)
def eq_choice(self, lhs: "Sint", rhs: "Sint") -> bool:
    return self._choice_point_factory.eq_choice(self, lhs, rhs)
def not_eq_choice(self, lhs: "Sint", rhs: "Sint") -> bool:
    return self._choice_point_factory.not_eq_choice(self, lhs, rhs)
def gte_choice(self, lhs: "Sint", rhs: "Sint") -> bool:
    return self._choice_point_factory.gte_choice(self, lhs, rhs)
def lte_choice(self, lhs: "Sint", rhs: "Sint") -> bool:
    return self._choice_point_factory.lte_choice(self, lhs, rhs)
# … overloaded for Slong, Sdouble, Sfloat …
def bool_choice(self, b: "Sbool") -> bool:
    return self._choice_point_factory.bool_choice(self, b)
def negated_bool_choice(self, b: "Sbool") -> bool:
    return self._choice_point_factory.negated_bool_choice(self, b)
```

---

## 6. `calculation_factory.py` — `CalculationFactory` ABC

**Purpose:** Define the full interface for all arithmetic, comparison, bitwise,
cast, and boolean operations in the search region.

```python
# mulib_python/executor/calculation_factory.py
from __future__ import annotations
import abc
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from mulib_python.executor.symbolic_execution import SymbolicExecution
    from mulib_python.substitutions.primitives.sint import (
        Sint, Sbool, Sbyte, Schar, Sshort,
    )
    from mulib_python.substitutions.primitives.slong import Slong
    from mulib_python.substitutions.primitives.sdouble import Sdouble
    from mulib_python.substitutions.primitives.sfloat import Sfloat


class CalculationFactory(abc.ABC):
    """Factory for all arithmetic, comparison, bitwise, and cast operations.

    Each method receives the current SymbolicExecution instance (``se``) and one
    or two operands.  The return type matches the Java original.  Both symbolic and
    concrete inputs must be accepted; implementations short-circuit to concrete Python
    arithmetic when all operands are concrete.

    Factory selection:
        CalculationFactory.get_instance(config, value_factory) → SymbolicCalculationFactory
                                                                   or ConcolicCalculationFactory
    """

    @staticmethod
    def get_instance(config: Any, value_factory: Any) -> "CalculationFactory":
        if config.search_concolic:
            from mulib_python.executor.concolic_calculation_factory import ConcolicCalculationFactory
            return ConcolicCalculationFactory(config, value_factory)
        from mulib_python.executor.symbolic_calculation_factory import SymbolicCalculationFactory
        return SymbolicCalculationFactory(config, value_factory)

    # ---- boolean logic --------------------------------------------------
    @abc.abstractmethod
    def implies(self, se: "SymbolicExecution", lhs: "Sbool", rhs: "Sbool") -> "Sbool": ...
    @abc.abstractmethod
    def and_(self, se: "SymbolicExecution", lhs: "Sbool", rhs: "Sbool") -> "Sbool": ...
    @abc.abstractmethod
    def or_(self, se: "SymbolicExecution", lhs: "Sbool", rhs: "Sbool") -> "Sbool": ...
    @abc.abstractmethod
    def xor(self, se: "SymbolicExecution", lhs: "Sbool", rhs: "Sbool") -> "Sbool": ...
    @abc.abstractmethod
    def not_(self, se: "SymbolicExecution", b: "Sbool") -> "Sbool": ...

    # ---- arithmetic (Sint) ----------------------------------------------
    @abc.abstractmethod
    def add(self, se: "SymbolicExecution", lhs: "Sint", rhs: "Sint") -> "Sint": ...
    @abc.abstractmethod
    def sub(self, se: "SymbolicExecution", lhs: "Sint", rhs: "Sint") -> "Sint": ...
    @abc.abstractmethod
    def mul(self, se: "SymbolicExecution", lhs: "Sint", rhs: "Sint") -> "Sint": ...
    @abc.abstractmethod
    def div(self, se: "SymbolicExecution", lhs: "Sint", rhs: "Sint") -> "Sint": ...
    @abc.abstractmethod
    def mod(self, se: "SymbolicExecution", lhs: "Sint", rhs: "Sint") -> "Sint": ...
    @abc.abstractmethod
    def neg_int(self, se: "SymbolicExecution", i: "Sint") -> "Sint": ...

    # ---- arithmetic (Sdouble) -------------------------------------------
    @abc.abstractmethod
    def add_double(self, se, lhs: "Sdouble", rhs: "Sdouble") -> "Sdouble": ...
    @abc.abstractmethod
    def sub_double(self, se, lhs: "Sdouble", rhs: "Sdouble") -> "Sdouble": ...
    @abc.abstractmethod
    def mul_double(self, se, lhs: "Sdouble", rhs: "Sdouble") -> "Sdouble": ...
    @abc.abstractmethod
    def div_double(self, se, lhs: "Sdouble", rhs: "Sdouble") -> "Sdouble": ...
    @abc.abstractmethod
    def mod_double(self, se, lhs: "Sdouble", rhs: "Sdouble") -> "Sdouble": ...
    @abc.abstractmethod
    def neg_double(self, se, d: "Sdouble") -> "Sdouble": ...

    # ---- arithmetic (Sfloat) --------------------------------------------
    # … identical pattern to Sdouble …

    # ---- arithmetic (Slong) ---------------------------------------------
    # … identical pattern to Sint …

    # ---- comparisons (Sint) ---------------------------------------------
    @abc.abstractmethod
    def lt(self, se, lhs: "Sint", rhs: "Sint") -> "Sbool": ...
    @abc.abstractmethod
    def lte(self, se, lhs: "Sint", rhs: "Sint") -> "Sbool": ...
    @abc.abstractmethod
    def eq(self, se, lhs: "Sint", rhs: "Sint") -> "Sbool": ...
    # … Slong/Sdouble/Sfloat overloads …

    # ---- three-way compare ----------------------------------------------
    @abc.abstractmethod
    def cmp_long(self, se, lhs: "Slong", rhs: "Slong") -> "Sint": ...
    @abc.abstractmethod
    def cmp_double(self, se, lhs: "Sdouble", rhs: "Sdouble") -> "Sint": ...
    @abc.abstractmethod
    def cmp_float(self, se, lhs: "Sfloat", rhs: "Sfloat") -> "Sint": ...

    # ---- casts ----------------------------------------------------------
    @abc.abstractmethod
    def i2l(self, se, i: "Sint") -> "Slong": ...
    @abc.abstractmethod
    def i2f(self, se, i: "Sint") -> "Sfloat": ...
    @abc.abstractmethod
    def i2d(self, se, i: "Sint") -> "Sdouble": ...
    @abc.abstractmethod
    def i2b(self, se, i: "Sint") -> "Sbyte": ...
    @abc.abstractmethod
    def i2s(self, se, i: "Sint") -> "Sshort": ...
    @abc.abstractmethod
    def i2c(self, se, i: "Sint") -> "Schar": ...
    @abc.abstractmethod
    def l2i(self, se, l: "Slong") -> "Sint": ...
    @abc.abstractmethod
    def l2f(self, se, l: "Slong") -> "Sfloat": ...
    @abc.abstractmethod
    def l2d(self, se, l: "Slong") -> "Sdouble": ...
    @abc.abstractmethod
    def f2i(self, se, f: "Sfloat") -> "Sint": ...
    @abc.abstractmethod
    def f2l(self, se, f: "Sfloat") -> "Slong": ...
    @abc.abstractmethod
    def f2d(self, se, f: "Sfloat") -> "Sdouble": ...
    @abc.abstractmethod
    def d2i(self, se, d: "Sdouble") -> "Sint": ...
    @abc.abstractmethod
    def d2l(self, se, d: "Sdouble") -> "Slong": ...
    @abc.abstractmethod
    def d2f(self, se, d: "Sdouble") -> "Sfloat": ...

    # ---- bitwise (Sint) -------------------------------------------------
    @abc.abstractmethod
    def ishl(self, se, i0: "Sint", i1: "Sint") -> "Sint": ...
    @abc.abstractmethod
    def ishr(self, se, i0: "Sint", i1: "Sint") -> "Sint": ...
    @abc.abstractmethod
    def iushr(self, se, i0: "Sint", i1: "Sint") -> "Sint": ...
    @abc.abstractmethod
    def ixor(self, se, i0: "Sint", i1: "Sint") -> "Sint": ...
    @abc.abstractmethod
    def ior(self, se, i0: "Sint", i1: "Sint") -> "Sint": ...
    @abc.abstractmethod
    def iand(self, se, i0: "Sint", i1: "Sint") -> "Sint": ...

    # ---- bitwise (Slong) ------------------------------------------------
    @abc.abstractmethod
    def lshl(self, se, l: "Slong", i: "Sint") -> "Slong": ...
    @abc.abstractmethod
    def lshr(self, se, l: "Slong", i: "Sint") -> "Slong": ...
    @abc.abstractmethod
    def lushr(self, se, l: "Slong", i: "Sint") -> "Slong": ...
    @abc.abstractmethod
    def lxor(self, se, l0: "Slong", l1: "Slong") -> "Slong": ...
    @abc.abstractmethod
    def lor(self, se, l0: "Slong", l1: "Slong") -> "Slong": ...
    @abc.abstractmethod
    def land(self, se, l0: "Slong", l1: "Slong") -> "Slong": ...
```

### Overloading strategy in Python

Python has no method overloading by type.  The Java design overloads `add(Sint, Sint)`,
`add(Sdouble, Sdouble)` etc.  In Python we use **separate method names** with a type
suffix (`add`, `add_double`, `add_float`, `add_long`).  The `SymbolicExecution` proxy
methods dispatch correctly.  Alternatively, a single `add(se, lhs, rhs)` method with
`isinstance` dispatch is acceptable and simpler; use whichever approach is easier for
the `ValueFactory` integration.

---

## 7. `symbolic_calculation_factory.py` — `SymbolicCalculationFactory`

**Purpose:** Build symbolic expressions/constraints for all operations.
Short-circuits to concrete Python arithmetic when both operands are `ConcSnumber`
(or `ConcSbool`).

### Internal helpers

```python
from mulib_python.expressions import Sum, Sub, Mul, Div, Mod, Neg
from mulib_python.expressions import BitwiseAnd, BitwiseOr, BitwiseXor
from mulib_python.expressions import ShiftLeft, ShiftRight, LogicalShiftRight
from mulib_python.constraints import Lt, Lte, Eq, And, Or, Not, Xor, Implication

def _sum(lhs, rhs):    return Sum(lhs, rhs)
def _sub(lhs, rhs):    return Sub(lhs, rhs)
def _mul(lhs, rhs):    return Mul(lhs, rhs)
def _div(lhs, rhs):    return Div(lhs, rhs)
def _mod(lhs, rhs):    return Mod(lhs, rhs)
def _neg(e):           return Neg(e)
def _lt(lhs, rhs):     return Lt(lhs, rhs)
def _lte(lhs, rhs):    return Lte(lhs, rhs)
def _eq(lhs, rhs):     return Eq(lhs, rhs)
def _and(l, r):        return And.new_instance(l, r)
def _or(l, r):         return Or.new_instance(l, r)
def _not(c):           return Not(c)
def _xor(l, r):        return Xor(l, r)
def _implication(l,r): return Implication(l, r)
def _bw_and(l, r):     return BitwiseAnd(l, r)
def _bw_or(l, r):      return BitwiseOr(l, r)
def _bw_xor(l, r):     return BitwiseXor(l, r)
def _shl(l, r):        return ShiftLeft(l, r)
def _shr(l, r):        return ShiftRight(l, r)
def _ushr(l, r):       return LogicalShiftRight(l, r)
```

### Concrete short-circuit pattern (for every arithmetic op)

```python
def add(self, se, lhs: Sint, rhs: Sint) -> Sint:
    if isinstance(lhs, ConcSnumber) and isinstance(rhs, ConcSnumber):
        return Sint.conc_sint(lhs.int_val() + rhs.int_val())
    return self._value_factory.wrapping_sym_sint(se, _sum(lhs, rhs))
```

The same pattern repeats for every operator.  The `isinstance(…, ConcSnumber)` check
ensures that whenever both operands are fully concrete the factory returns a cheap
concrete wrapper without touching the solver.

### Comparison methods

```python
def lt(self, se, lhs: Sint, rhs: Sint) -> Sbool:
    if isinstance(lhs, ConcSnumber) and isinstance(rhs, ConcSnumber):
        return Sbool.conc_sbool(lhs.int_val() < rhs.int_val())
    return self._value_factory.wrapping_sym_sbool(se, _lt(lhs, rhs))
```

`_lt(lhs, rhs)` creates a `Lt(lhs, rhs)` constraint node.  The `ValueFactory` wraps
it in a `SymSbool` leaf.

### Boolean ops with eager short-circuit

```python
def and_(self, se, lhs: Sbool, rhs: Sbool) -> Sbool:
    if isinstance(lhs, Sbool.ConcSbool):
        return Sbool.FALSE if lhs.is_false() else rhs
    if isinstance(rhs, Sbool.ConcSbool):
        return Sbool.FALSE if rhs.is_false() else lhs
    return self._value_factory.wrapping_sym_sbool(se, _and(lhs, rhs))

def or_(self, se, lhs: Sbool, rhs: Sbool) -> Sbool:
    if isinstance(lhs, Sbool.ConcSbool):
        return Sbool.TRUE if lhs.is_true() else rhs
    if isinstance(rhs, Sbool.ConcSbool):
        return Sbool.TRUE if rhs.is_true() else lhs
    return self._value_factory.wrapping_sym_sbool(se, _or(lhs, rhs))
```

### Cast methods

```python
def i2l(self, se, i: Sint) -> Slong:
    if isinstance(i, ConcSnumber):
        return Slong.conc_slong(i.long_val())
    # For symbolic, reuse the same expression tree — the bit-width change is
    # captured purely by the type wrapper, not the AST node.
    return self._value_factory.wrapping_sym_slong(se, i.represented_expression)

def i2d(self, se, i: Sint) -> Sdouble:
    if isinstance(i, ConcSnumber):
        return Sdouble.conc_sdouble(float(i.int_val()))
    return self._value_factory.wrapping_sym_sdouble(se, i.represented_expression)
```

The key insight: for symbolic casts, the `Expression` AST node is reused unchanged;
only the *wrapper type* (`Sint` → `Slong`) changes.  The solver back-end (Z3) handles
the bit-width semantics when translating the expression.

### Three-way compare (`cmp`)

```python
def cmp_long(self, se, lhs: Slong, rhs: Slong) -> Sint:
    if isinstance(lhs, ConcSnumber) and isinstance(rhs, ConcSnumber):
        lv, rv = lhs.long_val(), rhs.long_val()
        return Sint.conc_sint(0 if lv == rv else (1 if lv > rv else -1))
    # Symbolic: delegate to value factory's cmp helper which may produce an
    # ExpressionIte node or a dedicated CmpExpression node.
    return self._value_factory.cmp(se, lhs, rhs)
```

---

## 8. `concolic_calculation_factory.py` — `ConcolicCalculationFactory`

**Purpose:** Wrap every operation so that the result carries both a symbolic
expression tree **and** a concrete label from the current execution.

### Structure

```python
class ConcolicCalculationFactory(CalculationFactory):
    def __init__(self, config, value_factory) -> None:
        self._scf = SymbolicCalculationFactory(config, value_factory)
        self._vf  = value_factory
        self._config = config
```

### Concolic arithmetic pattern (for `add Sint`)

```python
def add(self, se, lhs: Sint, rhs: Sint) -> Sint:
    # 1. Strip concolic wrappers to get pure symbolic operands
    sym_lhs = ConcolicMathematicalContainer.try_get_sym_from_concolic(lhs)
    sym_rhs = ConcolicMathematicalContainer.try_get_sym_from_concolic(rhs)

    # 2. Delegate to symbolic factory for the symbolic result
    sym_result = self._scf.add(se, sym_lhs, sym_rhs)

    # 3. If the symbolic result is itself concrete (both inputs were concrete), return it
    if isinstance(sym_result, ConcSnumber):
        return sym_result

    # 4. Compute the concrete label using checked Python arithmetic
    conc_lhs = ConcolicMathematicalContainer.get_conc_numeric_from_concolic(lhs)
    conc_rhs = ConcolicMathematicalContainer.get_conc_numeric_from_concolic(rhs)
    conc_result = Sint.conc_sint(conc_lhs.int_val() + conc_rhs.int_val())

    # 5. Pack into a concolic wrapper Sint
    container = ConcolicMathematicalContainer(sym_result, conc_result)
    result = Sint.new_expression_symbolic_sint(container)
    return result
```

**Every numeric operation** follows this five-step pattern (strip → symbolic → shortcut
if concrete → concrete evaluate → wrap).  Floating-point overflow is handled the same
way as Java (`float('inf')`, `float('nan')` are valid Python float values).

### Concolic boolean pattern (for `lt Sint`)

```python
def lt(self, se, lhs: Sint, rhs: Sint) -> Sbool:
    sym_lhs = ConcolicMathematicalContainer.try_get_sym_from_concolic(lhs)
    sym_rhs = ConcolicMathematicalContainer.try_get_sym_from_concolic(rhs)

    sym_result = self._scf.lt(se, sym_lhs, sym_rhs)
    if isinstance(sym_result, Sbool.ConcSbool):
        return sym_result

    conc_lhs = ConcolicMathematicalContainer.get_conc_numeric_from_concolic(lhs)
    conc_rhs = ConcolicMathematicalContainer.get_conc_numeric_from_concolic(rhs)
    conc_result = Sbool.conc_sbool(conc_lhs.int_val() < conc_rhs.int_val())

    container = ConcolicConstraintContainer(sym_result, conc_result)
    return Sbool.new_constraint_sbool(container)
```

### Concolic cast pattern (for `i2l`)

```python
def i2l(self, se, i: Sint) -> Slong:
    sym_i = ConcolicMathematicalContainer.try_get_sym_from_concolic(i)
    sym_result = self._scf.i2l(se, sym_i)
    if isinstance(sym_result, ConcSnumber):
        return sym_result
    conc_i = ConcolicMathematicalContainer.get_conc_numeric_from_concolic(i)
    conc_result = Slong.conc_slong(conc_i.long_val())
    container = ConcolicMathematicalContainer(sym_result, conc_result)
    return Slong.new_expression_symbolic_slong(container)
```

---

## 9. `abstract_mulib_executor.py` — `AbstractMulibExecutor`

This is the heart of the execution engine.  It implements `MulibExecutor` and provides
the template search loop.

### 9.1 Constructor fields

```python
class AbstractMulibExecutor(MulibExecutor, abc.ABC):
    def __init__(
        self,
        executor_manager: "MulibExecutorManager",
        config: "MulibConfig",
        root_choice_option: "Choice.ChoiceOption",
        search_strategy: "SearchStrategy",
        search_region: Callable,            # replaces Java MethodHandle
        static_variables: "StaticVariables",
        search_region_args: tuple,
    ) -> None:
```

| Python field | Purpose |
|---|---|
| `_executor_manager` | owning manager |
| `_solver_manager` | per-executor `SolverManager` (from Phase 3) |
| `_search_strategy` | `SearchStrategy` enum value |
| `_config` | `MulibConfig` |
| `_current_choice_option` | currently targeted `ChoiceOption` |
| `root_choice_of_search_tree` | fixed root `Choice` |
| `_search_region` | `Callable` representing the search-region function |
| `_static_variables` | per-executor copy of `StaticVariables` |
| `_search_region_args` | tuple of pre-transformed arguments |
| `_remembered_sprimitives` | `dict[str, Sprimitive]` filled during execution |
| `_prototypical_budget_manager` | `ExecutionBudgetManager` prototype |
| `_current_symbolic_execution` | set before each invoke, cleared after |
| `paused` | `bool` — True after terminate() or budget exceeded |
| Statistics counters | `heuristic_sat_evals`, `sat_evals`, `unsat_evals`, `added_after_backtracking_point`, `solver_backtrack` |

The constructor calls
`self._static_variables = static_variables.copy_from_prototype()` so each executor
starts with a fresh copy.

### 9.2 Main search loop — `get_path_solution()`

```python
def get_path_solution(self) -> Optional[PathSolution]:
    """Drive the search until a PathSolution is found or the deque is exhausted."""
    deque = self._get_deque()
    while deque and not self.paused and not self._executor_manager.global_budget_exceeded():
        opt_se = self._create_execution()
        if opt_se is None:
            continue
        se = opt_se
        if self._config.search_concolic and not self._solver_manager.is_satisfiable():
            self._current_choice_option.set_unsatisfiable()
            continue
        self._current_symbolic_execution = se
        try:
            solution_value = self._invoke_search_region()
            if not self._solver_manager.is_satisfiable():
                self._current_choice_option.set_unsatisfiable()
                continue
            path_solution = self._build_path_solution(solution_value, is_exception=False)
            self._executor_manager.add_to_path_solutions(path_solution, self)
            return path_solution
        except Backtrack:
            self._config.callback_backtrack(self, None, self._solver_manager)
        except Fail:
            fail_node = self._current_choice_option.set_explicitly_failed()
            self._executor_manager.add_to_fails(fail_node)
            self._config.callback_fail(self, fail_node, self._solver_manager)
        except ExceededBudgetException as be:
            eb = self._current_choice_option.set_budget_exceeded(be.budget)
            self._executor_manager.add_to_exceeded_budgets(eb)
            self._config.callback_exceeded_budget(self, eb, self._solver_manager)
        except MulibException as e:
            if self._config.search_concolic and not self._solver_manager.is_satisfiable():
                self._current_choice_option.set_unsatisfiable()
                continue
            raise
        except BaseException as e:
            if not self._solver_manager.is_satisfiable():
                self._current_choice_option.set_unsatisfiable()
                continue
            if self._config.search_allow_exceptions:
                path_solution = self._build_path_solution(e, is_exception=True)
                self._executor_manager.add_to_path_solutions(path_solution, self)
                return path_solution
            raise MulibRuntimeException("Unhandled exception in search region") from e
    return None
```

### 9.3 `_create_execution()` — select the next ChoiceOption

```python
def _create_execution(self) -> Optional[SymbolicExecution]:
    opt_co = self._select_next_choice_option(self._get_deque())
    if opt_co is None:
        return None
    co = opt_co
    assert not co.is_unsatisfiable()
    self._adjust_solver_manager_to_new_choice_option(co)
    if not self._check_if_satisfiable_and_set(co):
        return None
    assert self._current_choice_option.depth == self._solver_manager.get_level()
    se = SymbolicExecution(
        executor=self,
        choice_point_factory=self._executor_manager.choice_point_factory,
        value_factory=self._executor_manager.value_factory,
        calculation_factory=self._executor_manager.calculation_factory,
        navigate_to=co,
        execution_budget_manager_prototype=self._prototypical_budget_manager,
        next_partner_class_object_nr=0,   # or from value transformer
        config=self._config,
    )
    return se
```

### 9.4 `_adjust_solver_manager_to_new_choice_option()` — push/pop per depth

This is the critical solver bookkeeping:

```python
def _adjust_solver_manager_to_new_choice_option(
    self,
    option_to_evaluate: Choice.ChoiceOption,
) -> None:
    # Find deepest common ancestor between current and target
    backtrack_to = SearchTree.get_deepest_shared_ancestor(
        option_to_evaluate, self._current_choice_option
    )
    depth_diff = self._current_choice_option.depth - backtrack_to.depth
    self._solver_manager.backtrack(depth_diff)
    self.solver_backtrack += depth_diff

    # Replay the path from backtrack_to down to (but not including) target
    path = SearchTree.get_path_between(backtrack_to, option_to_evaluate)
    for co in path:
        self._solver_manager.add_constraint_after_new_backtracking_point(
            co.option_constraint
        )
        self._solver_manager.add_partner_class_object_constraints(
            co.partner_class_object_constraints
        )
        self.added_after_backtracking_point += 1

    # Update current_choice_option to the parent of the new target
    self._current_choice_option = (
        option_to_evaluate
        if option_to_evaluate.is_evaluated()
        else option_to_evaluate.parent_edge
    )
```

**Invariant:** After this call, `solver_manager.get_level() == option_to_evaluate.depth - 1`
(the parent's level), so when we add the option's own constraint with
`add_constraint_after_backtracking_point` we reach exactly `option_to_evaluate.depth`.

### 9.5 `_check_if_satisfiable_and_set()` — heuristic + solver check

```python
def _check_if_satisfiable_and_set(self, co: Choice.ChoiceOption) -> bool:
    if co.is_satisfiable():
        self._add_after_backtracking_point(co)
        return True
    if co.is_unsatisfiable():
        return False

    # Heuristic: if the sibling is already known to be unsat and the two options
    # are each other's negations, skip the solver call.
    if self._can_declare_satisfiable_by_negation(co):
        co.set_satisfiable()
        co.set_option_constraint(TRUE)
        self._add_after_backtracking_point(co)
        self.heuristic_sat_evals += 1
        return True

    self._add_after_backtracking_point(co)
    return self._check_sat_with_solver(co)

def _check_sat_with_solver(self, co: Choice.ChoiceOption) -> bool:
    if self._solver_manager.is_satisfiable():
        co.set_satisfiable()
        self.sat_evals += 1
        return True
    else:
        co.set_unsatisfiable()
        self.unsat_evals += 1
        self._backtrack_once()
        return False
```

### 9.6 `_invoke_search_region()` — copy args and call

```python
def _invoke_search_region(self) -> Any:
    copier = MulibValueCopier(self._current_symbolic_execution, self._config)
    self._static_variables.set_copier(copier)
    self._remembered_sprimitives.clear()
    try:
        self._solver_manager.reset_labels()
        if self._search_region_args:
            args = self._copy_arguments(self._search_region_args, copier)
            result = self._search_region(*args)
        else:
            result = self._search_region()
        self._reset_execution_specific_state()
        return result
    except BaseException as t:
        self._reset_execution_specific_state()
        raise

def _copy_arguments(self, args: tuple, copier: MulibValueCopier) -> list:
    replaced: dict = {}   # identity map for deduplication
    out = []
    for arg in args:
        obj_id = id(arg)
        if obj_id in replaced:
            out.append(replaced[obj_id])
            continue
        if isinstance(arg, Sprimitive):
            new_arg = copier.copy_sprimitive(arg)
        else:
            new_arg = copier.copy_non_sprimitive(arg)
        replaced[obj_id] = new_arg
        out.append(new_arg)
    return out

def _reset_execution_specific_state(self) -> None:
    self._static_variables.reset()
    SymbolicExecution.remove()
```

### 9.7 `add_new_constraint()` — persist on current ChoiceOption + solver

```python
@final
def add_new_constraint(self, c: Constraint) -> None:
    assert not self._current_symbolic_execution.next_is_on_known_path()
    # Accumulate on the live ChoiceOption so that the full path constraint
    # can later be reconstructed for the PathSolution.
    self._current_choice_option.set_option_constraint(
        And.new_instance(self._current_choice_option.option_constraint, c)
    )
    self._solver_manager.add_constraint(c)
```

### 9.8 `decide_on_next_choice_option_during_execution()`

Called by `ChoicePointFactory` when a new branch is hit during execution:

```python
def decide_on_next_choice_option_during_execution(
    self,
    options: List[Choice.ChoiceOption],
) -> Optional[Choice.ChoiceOption]:
    ebm = self._current_symbolic_execution.execution_budget_manager
    incremental_exceeded = ebm.incremental_actual_choice_point_budget_is_exceeded()
    result = None
    if self._should_continue_execution():
        result = self._take_choice_option_from_next_alternatives(options)
    if self.paused or result is None or incremental_exceeded:
        self._backtrack_once()
        return None   # signals Backtrack to ChoicePointFactory
    return result
```

### 9.9 Abstract hook

```python
@abc.abstractmethod
def _should_continue_execution(self) -> bool:
    """Return True if the executor should continue in the current run
    (DFS: always True; BFS: always False; IDDFS/DSAS: conditional)."""

@abc.abstractmethod
def _select_next_choice_option(
    self, deque: "ChoiceOptionDeque"
) -> Optional["Choice.ChoiceOption"]: ...
```

### 9.10 `remember_sprimitive()` and `_build_path_solution()`

```python
def remember_sprimitive(self, name: str, remembered: Sprimitive) -> None:
    # Strip concolic wrapper so the solver sees the pure symbolic leaf
    from mulib_python.executor.concolic_containers import ConcolicMathematicalContainer
    self._remembered_sprimitives[name] = (
        ConcolicMathematicalContainer.try_get_sym_from_concolic(remembered)
    )

def _build_path_solution(self, value: Any, *, is_exception: bool) -> PathSolution:
    solution = self._solver_manager.label_solution(value, self._remembered_sprimitives)
    acc = SearchTree.get_all_constraints_for_choice_option(self._current_choice_option)
    if is_exception:
        ps = self._current_choice_option.set_exception_solution(
            solution, acc.constraints, acc.partner_class_object_constraints
        )
    else:
        ps = self._current_choice_option.set_solution(
            solution, acc.constraints, acc.partner_class_object_constraints
        )
    self._config.callback_path_solution(self, ps, self._solver_manager)
    return ps
```

---

## 10. `generic_executor.py` — `GenericExecutor`

Extends `AbstractMulibExecutor` and selects the right retrieval strategy
based on `SearchStrategy`.

```python
class GenericExecutor(AbstractMulibExecutor):

    def __init__(self, root_choice_option, executor_manager, config,
                 search_strategy, search_region, static_variables, search_region_args):
        super().__init__(executor_manager, config, root_choice_option,
                         search_strategy, search_region, static_variables, search_region_args)
        self._dsas_missed: int = 0

        if search_strategy == SearchStrategy.DFS:
            self._continue = lambda: True
            self._retriever = GenericExecutor._dfs_retriever
        elif search_strategy == SearchStrategy.BFS:
            self._continue = lambda: False
            self._retriever = GenericExecutor._bfs_retriever
        elif search_strategy == SearchStrategy.IDDFS:
            self._continue = lambda: True
            self._retriever = GenericExecutor._bfs_retriever
        elif search_strategy == SearchStrategy.DSAS:
            self._continue = lambda: True
            self._retriever = self._dsas_retriever
        elif search_strategy == SearchStrategy.IDDSAS:
            self._continue = self._continue_based_on_global_iddfs
            self._retriever = self._dsas_retriever
        else:
            raise NotYetImplementedException(f"Strategy {search_strategy}")

    # ---- abstract implementations ----------------------------------------

    def _should_continue_execution(self) -> bool:
        return self._continue()

    def _select_next_choice_option(self, deque) -> Optional[Choice.ChoiceOption]:
        return self._retriever(deque)

    # ---- static retrievers -----------------------------------------------

    @staticmethod
    def _dfs_retriever(deque: ChoiceOptionDeque) -> Optional[Choice.ChoiceOption]:
        return deque.poll_last()

    @staticmethod
    def _bfs_retriever(deque: ChoiceOptionDeque) -> Optional[Choice.ChoiceOption]:
        return deque.poll_first()

    # ---- DSAS retriever --------------------------------------------------

    def _dsas_retriever(self, deque: ChoiceOptionDeque) -> Optional[Choice.ChoiceOption]:
        current_choice = self._current_choice_option.choice
        while current_choice is not self.root_choice_of_search_tree:
            for co in current_choice.choice_options:
                if co.is_evaluated():
                    continue
                if deque.request(co):
                    return co
            # backtrack up the tree
            current_choice = current_choice.parent_edge.choice
        self._dsas_missed += 1
        return deque.poll_first()

    # ---- IDDSAS continuation check --------------------------------------

    def _continue_based_on_global_iddfs(self) -> bool:
        current_depth = self._current_choice_option.depth
        to_reach = self._executor_manager.global_iddfs_synchronizer.get_to_reach_depth()
        if current_depth <= to_reach:
            return True
        min_d, max_d = self._get_deque().get_min_max_depth()
        if min_d == max_d:
            self._executor_manager.global_iddfs_synchronizer.set_next_depth(current_depth)
            return True
        return False
```

### Search strategy semantics

| Strategy | `_should_continue_execution()` | `_select_next_choice_option()` |
|---|---|---|
| `DFS` | always `True` (keep going deeper) | `poll_last()` — stack |
| `BFS` | always `False` (new run per depth level) | `poll_first()` — queue |
| `IDDFS` | always `True` | `poll_first()` (lowest-depth first) |
| `DSAS` | always `True` | `request()` — deepest ancestor match |
| `IDDSAS` | depth-budget check | `request()` |

**Note on DFS vs DSAS in Python:** DSAS is semantically a DFS variant that preferentially
re-uses solver state by choosing the choice option whose ancestor is deepest in the tree
relative to the previously executed option, saving solver push/pop operations.
In single-threaded mode, pure DFS and DSAS produce equivalent results but DSAS may
incur more deque traversal overhead; for multi-threaded mode DSAS is more important.

---

## 11. `MulibValueCopier`

**Purpose:** Deep-copy symbolic objects at the start of each search-region invocation
so that the search region always starts from a *fresh* symbolic state.

This is a non-trivial operation in the Java version because it must handle reference
cycles and share identity-equivalent sub-objects.  In Python, a minimal correct
implementation is:

```python
class MulibValueCopier:
    """Copies symbolic objects for a new search-region invocation.

    Uses a replacement map (object id → new copy) to preserve reference equality
    (two references to the same object remain the same reference in the copy).
    """

    def __init__(self, se: "SymbolicExecution", config: "MulibConfig") -> None:
        self._se = se
        self._config = config
        self._replaced: dict = {}   # id(original) -> copy

    def copy(self, obj: Any) -> Any:
        if obj is None:
            return None
        oid = id(obj)
        if oid in self._replaced:
            return self._replaced[oid]
        if isinstance(obj, Sprimitive):
            result = self.copy_sprimitive(obj)
        else:
            result = self.copy_non_sprimitive(obj)
        self._replaced[oid] = result
        return result

    def copy_sprimitive(self, sp: Sprimitive) -> Sprimitive:
        # Concrete values are immutable — safe to return as-is.
        from mulib_python.substitutions.markers import Conc
        if isinstance(sp, Conc):
            return sp
        # Symbolic: create a fresh leaf with the same leaf-id so it maps to the
        # same Z3 variable.  The key insight is that the leaf id encodes the
        # variable identity; we do NOT re-allocate a new counter.
        from mulib_python.substitutions.primitives.sprimitive import SymSprimitiveLeaf
        if isinstance(sp, SymSprimitiveLeaf):
            return sp   # leaf vars are immutable and stateless; share safely
        # For expression nodes, deep-copy is not needed because expression ASTs
        # are immutable; share them.
        return sp

    def copy_non_sprimitive(self, obj: Any) -> Any:
        # For PartnerClass objects and Sarrays (Phase 4): perform a structural copy.
        # For plain Python objects (int, str, …): return as-is.
        return obj
```

The `copy_sprimitive` method above is intentionally simple.  A full implementation
with `PartnerClass` support is deferred to Phase 4/7.

---

## 12. `executor_manager.py` — `MulibExecutorManager` ABC

```python
class MulibExecutorManager(abc.ABC):
    """Owns the shared search tree, factories, and list of path solutions."""

    def __init__(
        self,
        config: "MulibConfig",
        observed_tree: "SearchTree",
        choice_point_factory: "ChoicePointFactory",
        value_factory: "ValueFactory",
        calculation_factory: "CalculationFactory",
        search_region: Callable,
        static_variables: "StaticVariables",
        search_region_args: tuple,
    ) -> None:
        self.config = config
        self.observed_tree = observed_tree
        self.choice_point_factory = choice_point_factory
        self.value_factory = value_factory
        self.calculation_factory = calculation_factory
        self.search_region = search_region
        self.static_variables = static_variables
        self.search_region_args = search_region_args
        self._path_solutions: List[PathSolution] = []
        self._fails: List[Any] = []
        self._exceeded_budgets: List[Any] = []
        self._mulib_executors: List[MulibExecutor] = []
        self._start_time: float = time.monotonic()

    # ---- called by executors --------------------------------------------

    def add_to_path_solutions(self, ps: PathSolution, executor: MulibExecutor) -> None:
        self._path_solutions.append(ps)

    def add_to_fails(self, fail: Any) -> None:
        self._fails.append(fail)

    def add_to_exceeded_budgets(self, eb: Any) -> None:
        self._exceeded_budgets.append(eb)

    def notify_new_choice(
        self,
        depth: int,
        choice_options: List[Choice.ChoiceOption],
    ) -> None:
        self.observed_tree.get_choice_option_deque().add_all(choice_options)

    def global_budget_exceeded(self) -> bool:
        return self.config.global_budget_manager.is_exceeded()

    # ---- abstract -------------------------------------------------------

    @abc.abstractmethod
    def _check_for_failure(self) -> None:
        """Raise if any worker thread has failed (used by MultiExecutorsManager)."""

    # ---- main entry point -----------------------------------------------

    def get_all_path_solutions(self) -> List[PathSolution]:
        """Drive all executors to exhaustion and return every PathSolution found."""
        main_executor = self._get_or_create_main_executor()
        while True:
            if self._check_for_pause_and_terminate_if_needed():
                break
            ps = main_executor.get_path_solution()
            # ps may be None if the deque is temporarily empty in multi-threaded mode
        return self._path_solutions

    @abc.abstractmethod
    def _get_or_create_main_executor(self) -> MulibExecutor: ...

    @abc.abstractmethod
    def _check_for_pause_and_terminate_if_needed(self) -> bool: ...
```

---

## 13. `single_executor_manager.py` — `SingleExecutorManager`

```python
class SingleExecutorManager(MulibExecutorManager):
    """Single-threaded manager wrapping a single GenericExecutor."""

    def _check_for_failure(self) -> None:
        pass   # single-threaded; no concurrent failures possible

    def _get_or_create_main_executor(self) -> MulibExecutor:
        if not self._mulib_executors:
            root_co = self.observed_tree.root.get_option(0)
            executor = GenericExecutor(
                root_choice_option=root_co,
                executor_manager=self,
                config=self.config,
                search_strategy=self.config.search_main_strategy,
                search_region=self.search_region,
                static_variables=self.static_variables,
                search_region_args=self.search_region_args,
            )
            executor.add_constraint_after_backtracking_point(root_co.option_constraint)
            self._mulib_executors.append(executor)
        return self._mulib_executors[0]

    def _check_for_pause_and_terminate_if_needed(self) -> bool:
        self._check_for_failure()
        deque = self.observed_tree.get_choice_option_deque()
        if deque.is_empty() or self.global_budget_exceeded():
            for ex in self._mulib_executors:
                ex.terminate()
            return True
        return False
```

---

## 14. `multi_executors_manager.py` — `MultiExecutorsManager`

### Python concurrency model

**The GIL.**  Python threads share the interpreter lock.  For CPU-bound symbolic
execution the GIL means true parallelism is not achievable with `threading.Thread`.
However, **Z3's C extension releases the GIL during its SAT-check**, so the solver
calls in different threads do overlap.  For solver-heavy workloads, threading is
still beneficial.

**multiprocessing alternative.**  For CPU-bound logic **not** in the solver (e.g.
heavy Python-side constraint building) use `multiprocessing`.  The difficulty is
that the shared `SearchTree` / `ChoiceOptionDeque` cannot be cheaply shared across
processes.  The recommended approach is:

- Use `threading` by default (matches Java's thread model most closely, and Z3
  provides real parallelism).
- Offer an opt-in `use_multiprocessing=True` config flag that switches to
  `multiprocessing.Process` with a `multiprocessing.Manager().list()` for the
  shared deque.  This is a Phase 7 enhancement; Phase 6 implements the
  `threading`-based version only.

### Implementation

```python
import threading
from concurrent.futures import ThreadPoolExecutor, Future
from typing import List, Optional

class MultiExecutorsManager(MulibExecutorManager):

    def __init__(self, config, observed_tree, choice_point_factory, value_factory,
                 calculation_factory, search_region, static_variables, search_region_args) -> None:
        super().__init__(config, observed_tree, choice_point_factory, value_factory,
                         calculation_factory, search_region, static_variables, search_region_args)
        self._next_strategies: List[SearchStrategy] = list(config.search_additional_parallel_strategies)
        self._idle: List[MulibExecutor] = []
        self._idle_lock = threading.Lock()
        self._thread_pool = ThreadPoolExecutor(max_workers=len(self._next_strategies) + 1)
        self._activate_parallel_for: int = config.search_activate_parallel_for or 1
        self._failure: Optional[BaseException] = None
        self._failure_lock = threading.Lock()

    def signal_failure(self, exc: BaseException) -> None:
        with self._failure_lock:
            self._failure = exc

    def _check_for_failure(self) -> None:
        with self._failure_lock:
            if self._failure is not None:
                raise MulibRuntimeException("Worker thread failed") from self._failure

    def notify_new_choice(self, depth, choice_options) -> None:
        super().notify_new_choice(depth, choice_options)
        deque = self.observed_tree.get_choice_option_deque()
        # Spin up idle/new executors when enough work is available
        while (not self.global_budget_exceeded()
               and (self._idle or self._next_strategies)
               and deque.size() >= self._activate_parallel_for):
            with self._idle_lock:
                idle_ex = self._idle.pop(0) if self._idle else None
            if idle_ex is not None:
                def _run_idle(ex=idle_ex):
                    try:
                        self._compute_with_non_main_executor(ex)
                    finally:
                        with self._idle_lock:
                            self._idle.append(ex)
                self._thread_pool.submit(_run_idle)
            else:
                strategy = self._next_strategies.pop(0) if self._next_strategies else None
                if strategy is None:
                    return
                if deque.is_empty():
                    return
                root_co = self.observed_tree.root.get_option(0)
                def _start_new(s=strategy):
                    ex = GenericExecutor(root_co, self, self.config, s,
                                        self.search_region, self.static_variables,
                                        self.search_region_args)
                    ex.add_constraint_after_backtracking_point(root_co.option_constraint)
                    ex.add_existing_partner_class_object_constraints(
                        root_co.partner_class_object_constraints)
                    self._mulib_executors.append(ex)
                    try:
                        self._compute_with_non_main_executor(ex)
                    finally:
                        with self._idle_lock:
                            self._idle.append(ex)
                self._thread_pool.submit(_start_new)

    def _compute_with_non_main_executor(self, executor: MulibExecutor) -> None:
        while not self._check_for_pause():
            executor.get_path_solution()

    def _check_for_pause(self) -> bool:
        self._check_for_failure()
        deque = self.observed_tree.get_choice_option_deque()
        return self.global_budget_exceeded() or deque.is_empty()

    def _check_for_pause_and_terminate_if_needed(self) -> bool:
        self._check_for_failure()
        if self._check_for_pause():
            with self._idle_lock:
                all_idle = len(self._idle) == len(self._mulib_executors) - 1
            if all_idle:
                self._thread_pool.shutdown(
                    wait=True,
                    timeout=self.config.shutdown_parallel_timeout_ms / 1000.0
                )
                for ex in self._mulib_executors:
                    ex.terminate()
                return True
        return False

    def _get_or_create_main_executor(self) -> MulibExecutor:
        if not self._mulib_executors:
            root_co = self.observed_tree.root.get_option(0)
            executor = GenericExecutor(
                root_co, self, self.config,
                self.config.search_main_strategy,
                self.search_region, self.static_variables, self.search_region_args
            )
            executor.add_constraint_after_backtracking_point(root_co.option_constraint)
            self._mulib_executors.append(executor)
        return self._mulib_executors[0]
```

### Thread-safety of the shared deque

`ChoiceOptionDeque` implementations from Phase 5 must hold a `threading.Lock` on all
mutating operations (`poll_first`, `poll_last`, `add_all`, `request`, `size`).  The
Phase 5 plan already notes this requirement.  `MultiExecutorsManager` does not need
additional locking beyond what the deque provides.

---

## 15. `StaticVariables` in Python — when to use it

In Java, `StaticVariables` intercepts JVM static-field accesses produced by bytecode
transformation.  Python has no equivalent bytecode transformation layer in Phase 6.

**Recommendation:** Keep `StaticVariables` as a `dict`-backed store that user/instrumented
code can call explicitly via `se.get_static_field(name)` / `se.set_static_field(name, v)`.
Its main purpose in Phase 6 is:

1. Store initial symbolic values that should be the same for every execution run
   (e.g. a symbolic input that was created before the search loop started).
2. Provide per-executor isolation: each executor holds its own copy so that run A
   writing a field does not affect run B.

Static variables are reset at the end of each search-region invocation by
`AbstractMulibExecutor._reset_execution_specific_state()` calling
`static_variables.reset()`.

---

## 16. "Remembered" free variables

The `remember_sprimitive(name, val)` mechanism allows user code to tag a symbolic
variable with a human-readable name.  The name appears in the output `Solution.labels`
so that results can be inspected by name.

### How it flows

1. User calls `se.named_sym_sint("x")` (or any other named-variable method).
2. `se._add_named_variable("x", result)` calls `se._executor.remember_sprimitive("x", result)`.
3. `AbstractMulibExecutor.remember_sprimitive` strips any concolic wrapper and stores
   `{"x": sym_leaf}` in `self._remembered_sprimitives`.
4. Before each new invocation, `_invoke_search_region` clears `_remembered_sprimitives`.
5. After execution, `_build_path_solution` calls
   `solver_manager.label_solution(return_value, self._remembered_sprimitives)`.
6. The solver manager evaluates each symbolic variable in the current model and
   returns a `Solution(return_value=…, labels=Labels({"x": 42, "return": …}))`.

### Cross-run identity for bounded searches

When the user calls `named_sym_sint("x")` **in multiple runs** the intent is that all
runs share the same leaf variable for `"x"` (so that solutions from different runs are
comparable).  In the Java version this is achieved by having `remember_sprimitive`
map the name to the **same** leaf, regardless of run.

In Python: the `ValueFactory` should use the leaf **counter** as the unique name:
leaf variable `"Sint_0"` is always the same Z3 variable across runs because every
run starts the counter from 0 and allocates variables in the same order.
`named_sym_sint("x")` calls `se.get_next_number_sym_sint_leaf()` → 0, and stores
`"Sint_0"` as the Z3 variable name.  If the second run also asks for `"x"` first, it
also gets `"Sint_0"`.  This preserves cross-run identity automatically as long as the
search region always names variables in a deterministic order.

---

## 17. `MulibConfig` integration

Phase 6 references the following `MulibConfig` fields (snake_case Python equivalents):

| Java field | Python field | Type | Default |
|---|---|---|---|
| `SEARCH_MAIN_STRATEGY` | `search_main_strategy` | `SearchStrategy` | `DFS` |
| `SEARCH_ADDITIONAL_PARALLEL_STRATEGIES` | `search_additional_parallel_strategies` | `list[SearchStrategy]` | `[]` |
| `SEARCH_ACTIVATE_PARALLEL_FOR` | `search_activate_parallel_for` | `int` | `2` |
| `SEARCH_CONCOLIC` | `search_concolic` | `bool` | `False` |
| `SEARCH_ALLOW_EXCEPTIONS` | `search_allow_exceptions` | `bool` | `False` |
| `SHUTDOWN_PARALLEL_TIMEOUT_ON_SHUTDOWN_IN_MS` | `shutdown_parallel_timeout_ms` | `int` | `5000` |
| `CALLBACK_PATH_SOLUTION` | `callback_path_solution` | `Callable` | no-op |
| `CALLBACK_FAIL` | `callback_fail` | `Callable` | no-op |
| `CALLBACK_EXCEEDED_BUDGET` | `callback_exceeded_budget` | `Callable` | no-op |
| `CALLBACK_BACKTRACK` | `callback_backtrack` | `Callable` | no-op |

`MulibConfig` should be a `dataclass` (or simple class with defaults).  A builder
pattern mirrors the Java `MulibConfigBuilder`.

---

## 18. File-by-file creation checklist

```
mulib_python/executor/__init__.py
  Re-export: SymbolicExecution, CalculationFactory, SymbolicCalculationFactory,
             ConcolicCalculationFactory, AbstractMulibExecutor, GenericExecutor,
             SingleExecutorManager, MultiExecutorsManager, StaticVariables,
             ConcolicMathematicalContainer, ConcolicConstraintContainer,
             MulibValueCopier

mulib_python/executor/concolic_containers.py        § 2
mulib_python/executor/static_variables.py           § 3
mulib_python/executor/mulib_executor.py             § 4
mulib_python/executor/symbolic_execution.py         § 5
mulib_python/executor/calculation_factory.py        § 6
mulib_python/executor/symbolic_calculation_factory.py § 7
mulib_python/executor/concolic_calculation_factory.py § 8
mulib_python/executor/abstract_mulib_executor.py    § 9
mulib_python/executor/mulib_value_copier.py         § 11  (split from §9 for clarity)
mulib_python/executor/generic_executor.py           § 10
mulib_python/executor/executor_manager.py           § 12
mulib_python/executor/single_executor_manager.py    § 13
mulib_python/executor/multi_executors_manager.py    § 14
```

---

## 19. Python-specific concerns summary

### threading.local() for SymbolicExecution.get()

`_se_local = threading.local()` is declared at module level in `symbolic_execution.py`.
`SymbolicExecution.get()` returns `getattr(_se_local, "current", None)`.
Each OS thread gets its own instance of the local because `threading.local()` manages
per-thread storage automatically.  This is the direct Python equivalent of Java's
`ThreadLocal<SymbolicExecution>`.

### GIL vs multiprocessing for parallel strategies

- **Phase 6 uses `threading`** for `MultiExecutorsManager`.  This is correct and
  sufficient because Z3 (via its C extension) releases the GIL during solver calls,
  giving real parallelism for the most expensive operation.
- For heavily Python-bound paths (e.g. search-region functions that do little solver
  work), `multiprocessing` would be needed.  This is deferred to Phase 7.
- When switching to `multiprocessing`, the `ChoiceOptionDeque` must be replaced with
  a `multiprocessing.Manager().list()`-backed deque, and `PathSolution`/`Fail` objects
  must be picklable.

### MethodHandle → Callable

Java uses `MethodHandle` to invoke the search region.  In Python any `Callable`
(including a plain function, lambda, or `functools.partial`) works.  The search region
is passed as-is; the caller is responsible for wrapping it so that `se = SymbolicExecution.get()`
is accessible inside.

### `@final` methods

Use `typing.final` (Python 3.8+) decorator on methods that must not be overridden,
mirroring Java's `final` keyword.  This is advisory only; Python does not enforce it.

### `assert` statements

Many of the Java invariant checks use `assert`.  Python `assert` is disabled with
`-O`.  For production safety, consider wrapping critical assertions in explicit
`if __debug__:` guards or raising `MulibIllegalStateException` directly.

### Overflow handling for int arithmetic

Java's `Math.addExact` etc. raise `ArithmeticException` on overflow.  Python `int`
is arbitrary precision and never overflows.  For JVM-like 32-bit semantics, use:

```python
def _int32(x: int) -> int:
    x = x & 0xFFFFFFFF
    return x if x < 0x80000000 else x - 0x100000000
```

Apply this to `add`, `sub`, `mul`, `neg` for `Sint`.  Omit for `Slong` (64-bit) by
analogy.  Document that this is opt-in per the `MulibConfig.enforce_jvm_overflow`
flag (default `False` for Python idiom, `True` for JVM compatibility).

---

## 20. Tests to write

1. **`test_symbolic_execution.py`**
   - `test_thread_local_isolation`: two threads each create a `SymbolicExecution`;
     `get()` in each thread returns its own instance.
   - `test_predetermined_path_traversal`: build a 3-deep predetermined path; verify
     `next_is_on_known_path()` transitions correctly.
   - `test_assume_concrete_false_raises_fail`: `se.assume(Sbool.FALSE)` raises `Fail`.
   - `test_assume_concrete_true_noop`: `se.assume(Sbool.TRUE)` does nothing.
   - `test_counter_monotone`: call `get_next_number_sym_sint_leaf()` 5 times; verify 0–4.

2. **`test_symbolic_calculation_factory.py`**
   - `test_add_two_concrete_ints`: `add(conc_sint(2), conc_sint(3))` → `ConcSint(5)`.
   - `test_add_symbolic_int`: result is a `SymSint` wrapping a `Sum` expression.
   - `test_lt_concrete_short_circuit`: `lt(conc_sint(1), conc_sint(2))` → `ConcSbool(True)`.
   - `test_cast_i2l_concrete`: `i2l(conc_sint(-1))` → `ConcSlong(-1)`.
   - `test_and_short_circuit_false`: `and_(Sbool.FALSE, sym_sbool)` → `ConcSbool(False)`.

3. **`test_concolic_calculation_factory.py`**
   - Verify that `add` of two concolic-wrapped integers returns a result whose
     symbolic part is a `Sum` and whose concrete part equals the arithmetic result.
   - Verify that a fully-concrete input short-circuits and returns a `ConcSnumber`.

4. **`test_search_loop.py`**
   - Build a trivial search region (one `bool_choice` → two paths) and drive a
     `SingleExecutorManager` to completion; assert two `PathSolution`s.
   - Test DFS order: the deeper path should be found first.
   - Test BFS order: paths at the same depth should be found before deeper ones.

5. **`test_multi_executor_manager.py`**
   - Smoke test: two strategies (DFS + BFS) run concurrently on a binary-tree
     search region; all leaves are found.
   - Test that a `Fail` raised in a worker thread does not crash the manager but is
     recorded in `_fails`.

---

## 21. Dependency graph (import order)

```
expressions.py
  └── constraints.py
        └── exceptions.py
              └── substitutions/markers.py
                    └── substitutions/primitives/sprimitive.py
                          └── substitutions/primitives/snumber.py
                                └── substitutions/primitives/sint.py (+ sbool)
                                      └── substitutions/value_factory.py
                                            └── solver_manager.py
                                                  └── solution.py
                                                        └── search/strategy.py
                                                              └── search/trees/...
                                                                    └── search/budget/...
                                                                          └── search/choice_points/...
# Phase 6 adds:
executor/concolic_containers.py   (imports sint.py, snumber.py)
executor/static_variables.py      (no mulib_python imports)
executor/mulib_executor.py        (imports constraints.py, search/trees/choice.py,
                                   search/trees/path_solution.py, solution.py,
                                   substitutions/primitives/sprimitive.py,
                                   search/strategy.py)
executor/symbolic_execution.py    (imports mulib_executor.py, concolic_containers.py,
                                   all substitution types, search/trees/search_tree.py)
executor/calculation_factory.py   (imports substitution types, symbolic_execution.py)
executor/symbolic_calculation_factory.py (imports calculation_factory.py, expressions.py,
                                          constraints.py, value_factory.py)
executor/concolic_calculation_factory.py (imports symbolic_calculation_factory.py,
                                           concolic_containers.py)
executor/mulib_value_copier.py    (imports substitutions/markers.py,
                                   substitutions/primitives/sprimitive.py)
executor/abstract_mulib_executor.py (imports mulib_executor.py, symbolic_execution.py,
                                      search/trees/search_tree.py, solver_manager.py,
                                      static_variables.py, mulib_value_copier.py)
executor/generic_executor.py      (imports abstract_mulib_executor.py,
                                   search/strategy.py, search/trees/choice_option_deque.py)
executor/executor_manager.py      (imports calculation_factory.py, value_factory.py,
                                   search/trees/search_tree.py)
executor/single_executor_manager.py (imports executor_manager.py, generic_executor.py)
executor/multi_executors_manager.py (imports executor_manager.py, generic_executor.py,
                                      threading)
```

Circular-import risk: `symbolic_execution.py` ↔ `mulib_executor.py`.  Resolved
by using `TYPE_CHECKING` guards and forward string annotations where needed.

---

*End of Phase 6 Implementation Plan.*
