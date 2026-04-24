# Phase 5 Implementation Plan: Search Infrastructure

## Overview

This document is the complete, self-contained specification for the Phase 5 implementation agent.
It covers every file to create, every class and method signature, all Python-specific design
decisions, and precise notes on how the components inter-operate.

### Prerequisites (already implemented)

```
mulib_python/
├── expressions.py      — immutable AST nodes (Expression, Sum, Sub, Mul, Div, …)
├── constraints.py      — immutable constraint nodes (Constraint, And, Or, Not, Lt, Lte, Eq,
│                         TRUE, FALSE, PartnerClassObjectConstraint, …)
└── exceptions.py       — MulibException hierarchy (MulibRuntimeException, Fail, …)
```

### What Phase 5 adds

```
mulib_python/
└── search/
    ├── __init__.py
    ├── strategy.py                         # SearchStrategy enum
    ├── trees/
    │   ├── __init__.py
    │   ├── tree_node.py                    # TreeNode ABC
    │   ├── choice.py                       # Choice + ChoiceOption
    │   ├── path_solution.py                # PathSolution, ThrowablePathSolution
    │   ├── fail.py                         # Fail (leaf node)
    │   ├── exceeded_budget.py              # ExceededBudget (leaf node)
    │   ├── choice_option_deque.py          # ChoiceOptionDeque ABC
    │   ├── simple_choice_option_deque.py   # SimpleChoiceOptionDeque
    │   ├── direct_access_choice_option_deque.py  # DirectAccessChoiceOptionDeque
    │   └── search_tree.py                  # SearchTree
    ├── budget/
    │   ├── __init__.py
    │   ├── budget.py                       # Budget ABC
    │   ├── null_budget.py                  # NullBudget singleton
    │   ├── time_budget.py                  # TimeBudget
    │   ├── counting_budget.py              # CountingBudget (fixed + incremental)
    │   ├── execution_budget_manager.py     # ExecutionBudgetManager (per-run)
    │   └── global_execution_budget_manager.py  # GlobalExecutionBudgetManager (whole search)
    └── choice_points/
        ├── __init__.py
        ├── backtrack.py                    # Backtrack singleton exception
        ├── choice_point_exceeded_budget.py # ChoicePointExceededBudget exception
        ├── choice_point_factory.py         # ChoicePointFactory ABC
        └── symbolic_choice_point_factory.py # SymbolicChoicePointFactory
```

---

## 1. `mulib_python/search/strategy.py`

**Purpose:** Enumerate the supported search strategies, mirroring Java's
`SearchStrategy` enum.  Referenced by `MulibConfig` and the deque factory.

```python
import enum

class SearchStrategy(enum.Enum):
    BFS    = "BFS"     # breadth-first — pollFirst()
    DFS    = "DFS"     # depth-first   — pollLast()
    IDDFS  = "IDDFS"   # iterative-deepening DFS — pollFirst() with incremental budget
    DSAS   = "DSAS"    # deepest-shared-ancestor search — request()
    IDDSAS = "IDDSAS"  # iterative-deepening DSAS — request()
```

---

## 2. `mulib_python/search/trees/tree_node.py`

**Purpose:** Abstract base class for every node in the search tree.
Every node tracks its `parent_edge` (`ChoiceOption | None`) and its `depth` (int).

### Design notes

- The root node has `parent_edge = None` and `depth = 1` (matching Java: the root
  `Choice` is at depth 1 after the sentinel option).
- When a `TreeNode` is constructed with a non-`None` `parent_edge`, it **immediately
  calls `parent_edge.set_child(self)`**, so the tree wires itself up on construction.
  This is identical to the Java constructor.

```python
from __future__ import annotations
import abc
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from mulib_python.search.trees.choice import ChoiceOption


class TreeNode(abc.ABC):
    """Abstract base for every node in the search tree."""

    __slots__ = ("parent_edge", "depth")

    def __init__(self, parent_edge: "ChoiceOption | None") -> None:
        object.__setattr__(self, "parent_edge", parent_edge)
        if parent_edge is not None:
            parent_edge.set_child(self)
            object.__setattr__(self, "depth", parent_edge.depth + 1)
        else:
            object.__setattr__(self, "depth", 1)
```

> **Note:** `__setattr__` is intentionally **not** frozen here because subclass
> `ChoiceOption.set_child` needs to assign to `child`.  Leaf nodes (Fail,
> PathSolution, ExceededBudget) may be frozen after construction if desired, but
> this is not required for correctness.

---

## 3. `mulib_python/search/trees/choice.py`

**Purpose:** The central mutable node in the search tree.  A `Choice` is an
internal node holding ≥ 1 `ChoiceOption` children.  `ChoiceOption` is the
directed *edge* in the tree — it carries the branch constraint and tracks state.

### 3.1 `ChoiceOption` state machine

Use plain `int` module-level constants (not `IntFlag`) to match the Java bitmask
semantics exactly.  Python's `int` never overflows, so the signed-byte special
value `CONSTRAINT_MODIFIED_AFTER_INITIAL_SAT_CHECK = 0x80` (Java's `-128`) must
be stored as `0x80` and masked correctly.

```python
# State bitmask constants
_UNKNOWN          = 0
_SATISFIABLE      = 1
_EVALUATED        = 2
_UNSATISFIABLE    = 4
_BUDGET_EXCEEDED  = 8
_CUT_OFF          = 16
_EXPLICITLY_FAILED = 32
_CONSTRAINT_MODIFIED = 0x80   # Java: (byte) -128
```

**Legal state transitions**

| Current state     | Allowed next state              |
|-------------------|---------------------------------|
| `UNKNOWN`         | `SATISFIABLE`                   |
| `SATISFIABLE`     | `EVALUATED`                     |
| `UNKNOWN` or `SATISFIABLE` | → `UNSATISFIABLE`, `EXPLICITLY_FAILED`, `BUDGET_EXCEEDED` (bypasses EVALUATED) |

The `_CONSTRAINT_MODIFIED` bit is a *modifier* bit, not a primary state.  It is
OR-ed in and preserved across transitions.

### 3.2 `Choice` class

```python
from __future__ import annotations
from typing import List, Sequence, TYPE_CHECKING
from mulib_python.search.trees.tree_node import TreeNode
from mulib_python.constraints import Constraint, PartnerClassObjectConstraint

if TYPE_CHECKING:
    from mulib_python.search.trees.fail import Fail
    from mulib_python.search.trees.exceeded_budget import ExceededBudget
    from mulib_python.search.budget.budget import Budget
    # Solution is defined in Phase 2 / solving layer
    # from mulib_python.solving.solution import Solution


class Choice(TreeNode):
    """Internal node — a fork point with ≥ 1 ChoiceOptions."""

    __slots__ = ("_options",)

    def __init__(
        self,
        parent_edge: "ChoiceOption | None",
        *constraints: Constraint,
    ) -> None:
        if len(constraints) < 1:
            raise IllegalTreeModificationException(
                "There must be at least one choice option for a choice."
            )
        super().__init__(parent_edge)
        self._options: List[ChoiceOption] = [
            ChoiceOption(i, c, self) for i, c in enumerate(constraints)
        ]

    def get_option(self, number: int) -> "ChoiceOption":
        return self._options[number]

    @property
    def choice_options(self) -> List["ChoiceOption"]:
        return self._options

    def __repr__(self) -> str:
        return f"Choice(depth={self.depth}, n_options={len(self._options)})"
```

### 3.3 `ChoiceOption` class

`ChoiceOption` is **not** a Python inner class; instead it lives at module level
in `choice.py` and holds a `_choice` back-reference.  This avoids the
non-idiomatic Java inner-class pattern while preserving all semantics.

```python
class ChoiceOption:
    """A directed edge in the search tree, carrying a branch constraint
    and a state machine tracking exploration progress."""

    __slots__ = (
        "choice_option_number",
        "_option_constraint",
        "_partner_class_object_constraints",
        "_child",
        "_state",
        "_choice",
    )

    def __init__(
        self,
        choice_option_number: int,
        option_constraint: Constraint,
        choice: Choice,
    ) -> None:
        self.choice_option_number = choice_option_number
        self._option_constraint: Constraint = option_constraint
        self._partner_class_object_constraints: List[PartnerClassObjectConstraint] = []
        self._child: "TreeNode | None" = None
        self._state: int = _UNKNOWN
        self._choice: Choice = choice

    # ── Depth and parent navigation ──────────────────────────────────────────

    @property
    def depth(self) -> int:
        """Depth == depth of the parent Choice node."""
        return self._choice.depth

    @property
    def parent_edge(self) -> "ChoiceOption | None":
        """The ChoiceOption that is the parent of this option's Choice."""
        return self._choice.parent_edge

    @property
    def choice(self) -> Choice:
        return self._choice

    # ── Constraint access / mutation ─────────────────────────────────────────

    @property
    def option_constraint(self) -> Constraint:
        return self._option_constraint

    @option_constraint.setter
    def option_constraint(self, value: Constraint) -> None:
        """Replace the option constraint.
        
        Only permitted before the option is evaluated.
        Sets the CONSTRAINT_MODIFIED bit so the solver knows a re-check is needed.
        """
        if self._child is not None or self.is_evaluated:
            raise IllegalTreeModificationException(
                "The constraint of an already-evaluated choice option cannot be changed."
            )
        assert self.is_satisfiable, "Must be satisfiable before modifying constraint"
        self._state |= _CONSTRAINT_MODIFIED
        self._option_constraint = value

    @property
    def partner_class_object_constraints(self) -> List[PartnerClassObjectConstraint]:
        return self._partner_class_object_constraints

    def add_partner_class_constraint(self, c: PartnerClassObjectConstraint) -> None:
        if self._child is not None or self.is_evaluated:
            raise IllegalTreeModificationException(
                "Cannot add partner class constraint to an already-evaluated option."
            )
        self._partner_class_object_constraints.append(c)

    # ── Child management ─────────────────────────────────────────────────────

    def set_child(self, child: "TreeNode") -> None:
        """Called automatically by TreeNode.__init__ when wiring up the tree."""
        self._check_child_unset()
        self._check_state_transition_else_set(_EVALUATED)
        self._child = child

    @property
    def child(self) -> "TreeNode":
        if self._child is None:
            raise IllegalTreeAccessException("Child has not been set yet.")
        return self._child

    # ── State queries ─────────────────────────────────────────────────────────

    @property
    def is_unknown(self) -> bool:
        return self._state == _UNKNOWN or self._state == _CONSTRAINT_MODIFIED

    @property
    def is_satisfiable(self) -> bool:
        return (self._state & _SATISFIABLE) != 0

    @property
    def is_evaluated(self) -> bool:
        return (self._state & _EVALUATED) != 0

    @property
    def is_unsatisfiable(self) -> bool:
        return (self._state & _UNSATISFIABLE) != 0

    @property
    def is_explicitly_failed(self) -> bool:
        return (self._state & _EXPLICITLY_FAILED) != 0

    @property
    def is_budget_exceeded(self) -> bool:
        return (self._state & _BUDGET_EXCEEDED) != 0

    @property
    def is_cut_off(self) -> bool:
        return (self._state & _CUT_OFF) != 0

    @property
    def constraint_was_modified_after_initial_sat_check(self) -> bool:
        return (self._state & _CONSTRAINT_MODIFIED) != 0

    # ── State setters (terminal transitions) ─────────────────────────────────

    def set_satisfiable(self) -> None:
        self._check_state_transition_else_set(_SATISFIABLE)

    def set_unsatisfiable(self) -> "Fail":
        from mulib_python.search.trees.fail import Fail
        self._check_child_unset()
        result = Fail(self, explicitly_failed=False)
        # Fail.__init__ calls set_child → _check_state_transition_else_set(EVALUATED)
        # but we want UNSATISFIABLE, so revert:
        self._state = _UNSATISFIABLE
        return result

    def set_explicitly_failed(self) -> "Fail":
        from mulib_python.search.trees.fail import Fail
        self._check_child_unset()
        result = Fail(self, explicitly_failed=True)
        self._state = _EXPLICITLY_FAILED
        return result

    def set_solution(
        self,
        solution: object,            # Solution type from solving layer
        constraints: tuple,          # tuple[Constraint, ...]
        partner_class_object_constraints: tuple,  # tuple[PartnerClassObjectConstraint, ...]
    ) -> "PathSolution":
        from mulib_python.search.trees.path_solution import PathSolution
        self._check_child_unset()
        return PathSolution(self, solution, constraints, partner_class_object_constraints)

    def set_exception_solution(
        self,
        solution: object,
        constraints: tuple,
        partner_class_object_constraints: tuple,
    ) -> "ThrowablePathSolution":
        from mulib_python.search.trees.path_solution import ThrowablePathSolution
        self._check_child_unset()
        return ThrowablePathSolution(self, solution, constraints, partner_class_object_constraints)

    def set_budget_exceeded(self, budget: "Budget") -> "ExceededBudget":
        from mulib_python.search.trees.exceeded_budget import ExceededBudget
        self._check_child_unset()
        if budget.is_incremental:
            raise MulibRuntimeException(
                "set_budget_exceeded must not be called for incremental budgets."
            )
        result = ExceededBudget(self, budget)
        self._state = _BUDGET_EXCEEDED
        return result

    # ── Helpers ───────────────────────────────────────────────────────────────

    def _check_child_unset(self) -> None:
        if self._child is not None or self.is_evaluated:
            raise IllegalTreeModificationException(
                f"Child is already set to: {self._child}"
            )

    def _check_state_transition_else_set(self, new_state: int) -> None:
        """Validate and apply a state transition.

        Keeps the CONSTRAINT_MODIFIED modifier bit if it was already set.
        """
        valid = (
            self.is_unknown
            or (self.is_satisfiable and new_state == _EVALUATED)
        )
        if not valid:
            raise IllegalTreeModificationException(
                f"Cannot transition to '{_state_name(new_state)}' "
                f"from '{self.state_to_string()}'."
            )
        modifier = self._state & _CONSTRAINT_MODIFIED
        self._state = new_state | modifier

    def state_to_string(self) -> str:
        return _state_name(self._state)

    def __repr__(self) -> str:
        return (
            f"ChoiceOption(depth={self.depth}, number={self.choice_option_number}, "
            f"constraint={self._option_constraint!r}, state={self.state_to_string()})"
        )
```

**Helper at module level:**

```python
def _state_name(state: int) -> str:
    masked = state & ~_CONSTRAINT_MODIFIED   # strip modifier
    if masked == _UNKNOWN:        return "UNKNOWN"
    if masked & _SATISFIABLE:     return "SATISFIABLE"
    if masked & _EVALUATED:       return "EVALUATED"
    if masked & _BUDGET_EXCEEDED: return "BUDGET_EXCEEDED"
    if masked & _CUT_OFF:         return "CUT_OFF"
    if masked & _EXPLICITLY_FAILED: return "EXPLICITLY_FAILED"
    if masked & _UNSATISFIABLE:   return "UNSATISFIABLE"
    return "UNKNOWN_STATE"
```

**Exceptions used in `choice.py`** (import from `mulib_python.exceptions`):

```python
from mulib_python.exceptions import (
    MulibRuntimeException,
    IllegalTreeModificationException,   # add this to exceptions.py
    IllegalTreeAccessException,         # add this to exceptions.py
)
```

> **Add to `exceptions.py`:**
> ```python
> class IllegalTreeModificationException(MulibRuntimeException): ...
> class IllegalTreeAccessException(MulibRuntimeException): ...
> ```

---

## 4. `mulib_python/search/trees/fail.py`

**Purpose:** Leaf node representing a path that was pruned — either because the
constraint system became unsatisfiable (`explicitly_failed=False`) or because the
user explicitly called `Mulib.fail()` (`explicitly_failed=True`).

```python
from __future__ import annotations
from mulib_python.search.trees.tree_node import TreeNode
from mulib_python.search.trees.choice import ChoiceOption


class Fail(TreeNode):
    """Leaf indicating that this path cannot yield a solution."""

    __slots__ = ("explicitly_failed",)

    def __init__(self, parent_edge: ChoiceOption, explicitly_failed: bool) -> None:
        super().__init__(parent_edge)
        self.explicitly_failed = explicitly_failed

    def __repr__(self) -> str:
        return f"Fail(depth={self.depth}, explicitly_failed={self.explicitly_failed})"
```

---

## 5. `mulib_python/search/trees/exceeded_budget.py`

**Purpose:** Leaf node marking that further exploration was aborted due to a
non-incremental budget limit (e.g. maximum choice-point depth reached for the
current path).

```python
from __future__ import annotations
from mulib_python.search.trees.tree_node import TreeNode
from mulib_python.search.trees.choice import ChoiceOption
from mulib_python.search.budget.budget import Budget


class ExceededBudget(TreeNode):
    """Leaf indicating that evaluation was stopped because a budget was exceeded."""

    __slots__ = ("_exceeded_budget",)

    def __init__(self, parent_edge: ChoiceOption, exceeded_budget: Budget) -> None:
        super().__init__(parent_edge)
        self._exceeded_budget = exceeded_budget

    @property
    def exceeded_budget(self) -> Budget:
        return self._exceeded_budget

    def __repr__(self) -> str:
        return f"ExceededBudget(depth={self.depth})"
```

---

## 6. `mulib_python/search/trees/path_solution.py`

**Purpose:** Leaf node for a successfully completed path.  Holds the concrete
`Solution` (mapping of symbolic names → concrete values + return value) and the
full constraint trail from root to this leaf.

### `Solution` stub

The `Solution` type is defined in the solving layer (Phase 6+).  For Phase 5,
treat it as `object`; add a forward-declared type alias so future phases can
narrow it:

```python
# At the top of path_solution.py
from typing import Any, Tuple, TYPE_CHECKING
if TYPE_CHECKING:
    from mulib_python.solving.solution import Solution   # will exist in a later phase
SolutionType = Any   # narrowed once solving layer lands
```

### `PathSolution`

```python
from __future__ import annotations
from mulib_python.search.trees.tree_node import TreeNode
from mulib_python.search.trees.choice import ChoiceOption
from mulib_python.constraints import Constraint, PartnerClassObjectConstraint


class PathSolution(TreeNode):
    """Leaf indicating a successfully explored path.

    Attributes
    ----------
    solution:
        The concrete solution (return value + symbolic variable bindings).
    path_constraints:
        Ordered tuple of all Constraint objects on the path from root to here.
    partner_class_object_constraints:
        Ordered tuple of PartnerClassObjectConstraints accumulated on this path.
    """

    __slots__ = ("solution", "path_constraints", "partner_class_object_constraints")

    def __init__(
        self,
        parent_edge: ChoiceOption,
        solution: object,
        path_constraints: tuple,
        partner_class_object_constraints: tuple,
    ) -> None:
        super().__init__(parent_edge)
        self.solution = solution
        self.path_constraints: Tuple[Constraint, ...] = tuple(path_constraints)
        self.partner_class_object_constraints: Tuple[PartnerClassObjectConstraint, ...] = (
            tuple(partner_class_object_constraints)
        )

    def __repr__(self) -> str:
        return (
            f"{type(self).__name__}(depth={self.depth}, solution={self.solution!r})"
        )
```

### `ThrowablePathSolution`

```python
class ThrowablePathSolution(PathSolution):
    """Leaf indicating a path that ended by throwing an exception.

    The thrown exception object is accessible via ``solution``.
    """
    __slots__ = ()
```

---

## 7. `mulib_python/search/trees/choice_option_deque.py`

**Purpose:** Abstract base class for the priority double-ended queue of
unexplored `ChoiceOption` objects.  The queue orders by depth:
lowest-depth end = "first" (for BFS/IDDFS), highest-depth end = "last" (for DFS).

All public methods of all implementations **must be thread-safe**.

```python
from __future__ import annotations
import abc
from typing import List, Optional
from mulib_python.search.trees.choice import ChoiceOption


class ChoiceOptionDeque(abc.ABC):
    """Thread-safe priority deque of unexplored ChoiceOptions.

    Ordered by depth (ascending).  Consumers call pollFirst() for
    breadth-first strategies and pollLast() for depth-first strategies.
    The special request() method supports deepest-shared-ancestor strategies.
    """

    @abc.abstractmethod
    def poll_first(self) -> Optional[ChoiceOption]:
        """Remove and return an option with the minimum depth, or None if empty."""

    @abc.abstractmethod
    def poll_last(self) -> Optional[ChoiceOption]:
        """Remove and return an option with the maximum depth, or None if empty."""

    @abc.abstractmethod
    def insert(self, depth: int, options: List[ChoiceOption]) -> None:
        """Insert options at the given depth, skipping unsatisfiable ones."""

    @abc.abstractmethod
    def is_empty(self) -> bool: ...

    @abc.abstractmethod
    def request(self, requested: ChoiceOption) -> bool:
        """Try to atomically claim *requested*.
        
        Returns True if the option was present and is now removed; False if
        another consumer already took it.
        Used by DSAS/IDDSAS strategies.
        """

    @abc.abstractmethod
    def set_empty(self) -> None:
        """Clear all remaining options (e.g. when search is aborted)."""

    @abc.abstractmethod
    def size(self) -> int: ...

    @abc.abstractmethod
    def get_min_max_depth(self) -> tuple:
        """Return (min_depth, max_depth).  Returns (0, 0) if empty."""
```

---

## 8. `mulib_python/search/trees/simple_choice_option_deque.py`

**Purpose:** Linked-list–based implementation.  Uses Python's `collections.deque`
(O(1) at both ends) as the backing store.  Items are kept **sorted by depth** so
`poll_first()` and `poll_last()` can return correct answers in O(1) and O(1).
However, `insert()` requires O(n) in the worst case when inserting at the middle.

All methods lock `self._lock` (a `threading.Lock`).

### Insertion strategy (mirrors Java exactly)

```
if deque is empty:
    append all non-unsatisfiable options
elif depth >= max depth in deque:
    append to right (new tail)
elif depth <= min depth in deque:
    appendleft (new head)
else:
    find the insertion point and insert in-order (O(n))
```

```python
from __future__ import annotations
import threading
from collections import deque
from typing import List, Optional
from mulib_python.search.trees.choice import ChoiceOption
from mulib_python.search.trees.choice_option_deque import ChoiceOptionDeque


class SimpleChoiceOptionDeque(ChoiceOptionDeque):
    """Deque backed by collections.deque, kept sorted by ChoiceOption depth."""

    def __init__(self, root_option: ChoiceOption) -> None:
        self._deque: deque[ChoiceOption] = deque([root_option])
        self._lock = threading.Lock()

    def poll_first(self) -> Optional[ChoiceOption]:
        with self._lock:
            return self._deque.popleft() if self._deque else None

    def poll_last(self) -> Optional[ChoiceOption]:
        with self._lock:
            return self._deque.pop() if self._deque else None

    def insert(self, depth: int, options: List[ChoiceOption]) -> None:
        with self._lock:
            filtered = [co for co in options if not co.is_unsatisfiable]
            if not self._deque:
                self._deque.extend(filtered)
            elif depth >= self._deque[-1].depth:
                for co in filtered:
                    self._deque.append(co)
            elif depth <= self._deque[0].depth:
                for co in reversed(filtered):
                    self._deque.appendleft(co)
            else:
                # Find insertion index
                lst = list(self._deque)
                idx = next(
                    (i for i, co in enumerate(lst) if co.depth > depth),
                    len(lst)
                )
                for j, co in enumerate(filtered):
                    lst.insert(idx + j, co)
                self._deque = deque(lst)

    def is_empty(self) -> bool:
        with self._lock:
            return not self._deque

    def request(self, requested: ChoiceOption) -> bool:
        with self._lock:
            try:
                self._deque.remove(requested)
                return True
            except ValueError:
                return False

    def set_empty(self) -> None:
        with self._lock:
            self._deque.clear()

    def size(self) -> int:
        with self._lock:
            return len(self._deque)

    def get_min_max_depth(self) -> tuple:
        with self._lock:
            if self._deque:
                return (self._deque[0].depth, self._deque[-1].depth)
            return (0, 0)
```

---

## 9. `mulib_python/search/trees/direct_access_choice_option_deque.py`

**Purpose:** A more efficient implementation for IDDFS/DSAS strategies.
Instead of one flat sorted list, uses a `list[_LevelContainer]` indexed by
depth.  Each `_LevelContainer` holds a `deque` of options at that depth.
Cached `_head` and `_tail` pointers (depth integers) avoid scanning for the
minimum/maximum occupied level on every operation.

### `_LevelContainer`

```python
class _LevelContainer:
    __slots__ = ("depth", "_queue")

    def __init__(self, depth: int) -> None:
        self.depth = depth
        self._queue: deque[ChoiceOption] = deque()

    def insert(self, co: ChoiceOption) -> None:
        self._queue.append(co)

    def poll(self) -> Optional[ChoiceOption]:
        return self._queue.popleft() if self._queue else None

    def remove(self, co: ChoiceOption) -> bool:
        try:
            self._queue.remove(co)
            return True
        except ValueError:
            return False

    @property
    def is_empty(self) -> bool:
        return not self._queue
```

### `DirectAccessChoiceOptionDeque`

```python
_BATCH_EXPAND = 32   # grow the levels list in increments of 32

class DirectAccessChoiceOptionDeque(ChoiceOptionDeque):
    """Deque using a list-of-level-containers for O(1) depth-indexed access."""

    def __init__(self, root_option: ChoiceOption) -> None:
        self._lock = threading.Lock()
        self._levels: List[_LevelContainer] = []
        self._size: int = 0
        # Ensure capacity
        self._ensure_depth(root_option.depth)
        self._levels[root_option.depth].insert(root_option)
        self._size = 1
        self._cached_head_depth: int = root_option.depth
        self._cached_tail_depth: int = root_option.depth
```

**`_ensure_depth(depth)`** — grows `self._levels` so index `depth` exists,
adding `_BATCH_EXPAND` extra entries beyond `depth`.

**`_find_head_depth()`** — starting from `_cached_head_depth`, scans forward
(increasing depth) until a non-empty level is found.  Updates `_cached_head_depth`.

**`_find_tail_depth()`** — starting from `_cached_tail_depth`, scans backward
(decreasing depth) until a non-empty level is found.  Updates `_cached_tail_depth`.

**`poll_first()`** — acquires lock, calls `_find_head_depth()`, pops from that
level, decrements `_size`.

**`poll_last()`** — acquires lock, calls `_find_tail_depth()`, pops from that
level, decrements `_size`.

**`insert(depth, options)`** — acquires lock, calls `_ensure_depth(depth)`,
inserts each non-unsatisfiable option, updates cached head/tail if needed.

**`request(co)`** — acquires lock, calls `_levels[co.depth].remove(co)`.

**`is_empty()`** — acquires lock, calls `_find_head_depth()`, returns `True` if
no non-empty level is found.

**`size()`** — returns `self._size` (lock not needed if reads are atomic, but
hold the lock for correctness).

**`get_min_max_depth()`** — acquires lock, finds head and tail depths.

---

## 10. `mulib_python/search/trees/search_tree.py`

**Purpose:** The top-level container for one complete search: holds the root
`Choice`, the solutions list, the fails list, the exceeded-budgets list, and the
`ChoiceOptionDeque`.  Provides static utility methods used by the executor to
navigate the tree.

### Constructor

```python
def __init__(self, config: "MulibConfig") -> None:
```

1. Create `self.root = Choice(None, TRUE)` — a `Choice` with no parent and a
   single option whose constraint is the `TRUE` singleton from `constraints.py`.
2. Mark `self.root.get_option(0).set_satisfiable()` — this is the sentinel root
   option; it is always satisfiable.
3. If `config.search_additional_parallel_strategies` is non-empty, use
   thread-safe list wrappers (see §9.1 below); otherwise use plain lists.
4. Create `self._choice_option_deque` via the factory (see §10).

### Thread-safety for the lists

Python's built-in `list.append()` is GIL-protected and effectively atomic for
single appends.  However, to be safe across different Python implementations and
to mirror the Java `Collections.synchronizedList` pattern, wrap the lists:

```python
import threading

class _SynchronizedList:
    """Thin wrapper that serialises all access with a lock."""
    def __init__(self):
        self._lock = threading.Lock()
        self._list = []

    def append(self, item):
        with self._lock:
            self._list.append(item)

    def __iter__(self):
        with self._lock:
            return iter(list(self._list))   # snapshot

    def __len__(self):
        with self._lock:
            return len(self._list)
```

Use `_SynchronizedList` for `solutions_list`, `fails_list`,
`exceeded_budget_list` when `config` specifies parallel strategies; use plain
`list` otherwise.

### Static utility methods

These are pure tree-navigation helpers with no side effects; they do **not**
hold the deque lock.

#### `get_path_to(target: ChoiceOption) -> deque[ChoiceOption]`

Walk up the parent chain from `target`, prepending each option.

```python
@staticmethod
def get_path_to(target: ChoiceOption) -> deque:
    from collections import deque as _deque
    result = _deque()
    current = target
    while current is not None:
        result.appendleft(current)
        current = current.parent_edge
    return result
```

#### `get_deepest_shared_ancestor(co0: ChoiceOption, co1: ChoiceOption) -> ChoiceOption`

Walks both options up to the same depth, then walks both together until equal.
Used by `AbstractMulibExecutor` to determine how far to backtrack the solver.

```python
@staticmethod
def get_deepest_shared_ancestor(
    co0: ChoiceOption,
    co1: ChoiceOption,
) -> ChoiceOption:
    if co0.parent_edge is None:
        return co0
    if co1.parent_edge is None:
        return co1
    while co0 is not co1:
        if co0.depth < co1.depth:
            co1 = co1.parent_edge
        else:
            co0 = co0.parent_edge
    return co0
```

#### `get_path_between(from_co: ChoiceOption, to_co: ChoiceOption) -> deque[ChoiceOption]`

Returns the options strictly between `from_co` (exclusive) and `to_co`
(inclusive), in top-down order.

```python
@staticmethod
def get_path_between(from_co: ChoiceOption, to_co: ChoiceOption) -> deque:
    from collections import deque as _deque
    result = _deque()
    if from_co is to_co:
        return result
    assert to_co.depth > from_co.depth
    current = to_co.parent_edge
    while current is not from_co:
        result.appendleft(current)
        current = current.parent_edge
    return result
```

#### `get_all_constraints_for_choice_option(co: ChoiceOption) -> AccumulatedChoiceOptionConstraints`

```python
@staticmethod
def get_all_constraints_for_choice_option(
    co: ChoiceOption,
) -> "AccumulatedChoiceOptionConstraints":
    path = SearchTree.get_path_to(co)
    constraints = []
    partner_constraints = []
    for option in path:
        constraints.append(option.option_constraint)
        partner_constraints.extend(option.partner_class_object_constraints)
    return AccumulatedChoiceOptionConstraints(
        tuple(constraints),
        tuple(partner_constraints),
    )
```

#### `AccumulatedChoiceOptionConstraints` — inner dataclass

```python
from dataclasses import dataclass
from mulib_python.constraints import Constraint, PartnerClassObjectConstraint

@dataclass(frozen=True)
class AccumulatedChoiceOptionConstraints:
    constraints: tuple           # tuple[Constraint, ...]
    partner_class_object_constraints: tuple   # tuple[PartnerClassObjectConstraint, ...]
```

### `add_to_path_solutions(ps: PathSolution) -> None`

Appends to `solutions_list`.

### `add_to_fails(fail: Fail) -> None`

Appends to `fails_list` **only if** `config.tree_enlist_leaves` is `True`.

### `add_to_exceeded_budgets(eb: ExceededBudget) -> None`

Appends to `exceeded_budget_list` **only if** `config.tree_enlist_leaves` is `True`.

### `string_representation() -> str`

Recursive tree pretty-printer (for debugging).  Does **not** override
`__repr__` (to avoid hanging the debugger on large trees, matching the Java
comment).

---

## 11. `mulib_python/search/trees/__init__.py` — deque factory

The deque factory mirrors `ChoiceOptionDeques.getChoiceOptionDeque()`:

```python
from mulib_python.search.trees.simple_choice_option_deque import SimpleChoiceOptionDeque
from mulib_python.search.trees.direct_access_choice_option_deque import DirectAccessChoiceOptionDeque

class ChoiceOptionDeques:
    SIMPLE = "SIMPLE"
    DIRECT_ACCESS = "DIRECT_ACCESS"

    @staticmethod
    def get_choice_option_deque(
        config: "MulibConfig",
        root_option: "ChoiceOption",
    ) -> "ChoiceOptionDeque":
        deque_type = config.search_choice_option_deque_type
        if deque_type == ChoiceOptionDeques.SIMPLE:
            return SimpleChoiceOptionDeque(root_option)
        elif deque_type == ChoiceOptionDeques.DIRECT_ACCESS:
            return DirectAccessChoiceOptionDeque(root_option)
        else:
            raise NotYetImplementedException(f"Unknown deque type: {deque_type}")
```

---

## 12. Budget subsystem

### 12.1 `mulib_python/search/budget/budget.py`

```python
import abc

class Budget(abc.ABC):
    """Abstract budget: can be incremented and queried for exhaustion."""

    @abc.abstractmethod
    def increment(self) -> None: ...

    @abc.abstractmethod
    def is_exceeded(self) -> bool: ...

    @property
    @abc.abstractmethod
    def is_incremental(self) -> bool:
        """True if this budget drives an incremental search strategy."""

    @abc.abstractmethod
    def copy_from_prototype(self) -> "Budget":
        """Return a fresh copy with the same parameters (but reset counter)."""
```

### 12.2 `mulib_python/search/budget/null_budget.py`

Singleton; never exceeded; `increment()` is a no-op.

```python
class NullBudget(Budget):
    _instance: "NullBudget | None" = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance

    def increment(self) -> None: pass
    def is_exceeded(self) -> bool: return False
    is_incremental = False
    def copy_from_prototype(self) -> "NullBudget": return self

INSTANCE = NullBudget()
```

### 12.3 `mulib_python/search/budget/time_budget.py`

Maps Java's `System.nanoTime()` to `time.monotonic_ns()` (Python ≥ 3.7).

**Key semantics:** `increment()` **resets** the start time (it does NOT add to a
counter).  After calling `increment()`, the countdown starts from zero again.
`is_exceeded()` checks if more nanoseconds than `max_duration` have elapsed
since the last reset.

```python
import time

class TimeBudget(Budget):
    def __init__(self, max_duration_ns: int) -> None:
        self._max_duration = max_duration_ns
        self._start_time = time.monotonic_ns()

    @classmethod
    def get_time_budget(cls, max_duration_ns: int) -> "TimeBudget":
        return cls(max_duration_ns)

    def increment(self) -> None:
        self._start_time = time.monotonic_ns()

    def is_exceeded(self) -> bool:
        return (time.monotonic_ns() - self._max_duration) > self._start_time

    @property
    def is_incremental(self) -> bool:
        return False

    def copy_from_prototype(self) -> "TimeBudget":
        return TimeBudget(self._max_duration)
        # Note: copy starts with a fresh start_time, not the original's start_time
```

### 12.4 `mulib_python/search/budget/counting_budget.py`

```python
class CountingBudget(Budget):
    """Budget based on how many times increment() has been called."""

    def __init__(self, max_number: int, incremental: bool) -> None:
        self._max_number = max_number
        self._incremental = incremental
        self._seen: int = 0

    @classmethod
    def get_fixed_budget(cls, max_number: int) -> "CountingBudget":
        return cls(max_number, incremental=False)

    @classmethod
    def get_incremental_budget(cls, increment_by: int) -> "CountingBudget":
        return cls(increment_by, incremental=True)

    def increment(self) -> None:
        self._seen += 1

    def is_exceeded(self) -> bool:
        return self._seen >= self._max_number

    @property
    def is_incremental(self) -> bool:
        return self._incremental

    def copy_from_prototype(self) -> "CountingBudget":
        return CountingBudget(self._max_number, self._incremental)
        # Fresh copy always starts at 0 — no _seen carried over
```

### 12.5 `mulib_python/search/budget/execution_budget_manager.py`

Manages the two **per-execution** budgets (one fixed, one incremental, both
counting choice points).  Created fresh for each `SymbolicExecution` run by
calling `copy_from_prototype()`.

```python
from mulib_python.search.budget.null_budget import INSTANCE as NULL_BUDGET
from mulib_python.search.budget.counting_budget import CountingBudget

class ExecutionBudgetManager:
    """Tracks per-execution (per-run) budget consumption."""

    def __init__(
        self,
        fixed_cp_budget: Budget,
        incremental_cp_budget: Budget,
    ) -> None:
        self._fixed_cp = fixed_cp_budget
        self._incremental_cp = incremental_cp_budget
        self._should_not_be_copied = (
            fixed_cp_budget is NULL_BUDGET
            and incremental_cp_budget is NULL_BUDGET
        )

    @classmethod
    def new_instance(cls, config: "MulibConfig") -> "ExecutionBudgetManager":
        fixed = (
            CountingBudget.get_fixed_budget(config.budgets_fixed_actual_cp)
            if config.budgets_fixed_actual_cp is not None
            else NULL_BUDGET
        )
        incremental = (
            CountingBudget.get_incremental_budget(config.budgets_incr_actual_cp)
            if config.budgets_incr_actual_cp is not None
            else NULL_BUDGET
        )
        return cls(fixed, incremental)

    def fixed_actual_choice_point_budget_is_exceeded(self) -> bool:
        """Increments then checks the fixed budget.  Returns True if exceeded."""
        self._fixed_cp.increment()
        return self._fixed_cp.is_exceeded()

    def incremental_actual_choice_point_budget_is_exceeded(self) -> bool:
        """Increments then checks the incremental budget.  Returns True if exceeded."""
        self._incremental_cp.increment()
        return self._incremental_cp.is_exceeded()

    @property
    def fixed_actual_choice_point_budget(self) -> Budget:
        return self._fixed_cp

    def copy_from_prototype(self) -> "ExecutionBudgetManager":
        if self._should_not_be_copied:
            return self
        return ExecutionBudgetManager(
            self._fixed_cp.copy_from_prototype(),
            self._incremental_cp.copy_from_prototype(),
        )
```

### 12.6 `mulib_python/search/budget/global_execution_budget_manager.py`

Manages budgets for the **entire** search (not per-run).  These budgets are
checked by the `MulibExecutorManager`/`SearchTree` layer, not by individual
`SymbolicExecution` instances.

```python
class GlobalExecutionBudgetManager:
    """Tracks global limits across all runs."""

    def __init__(self, config: "MulibConfig") -> None:
        self._time = (
            TimeBudget.get_time_budget(config.budgets_global_time_in_nanoseconds)
            if config.budgets_global_time_in_nanoseconds is not None
            else NULL_BUDGET
        )
        self._fails = (
            CountingBudget.get_fixed_budget(config.budgets_max_fails)
            if config.budgets_max_fails is not None
            else NULL_BUDGET
        )
        self._path_solutions = (
            CountingBudget.get_fixed_budget(config.budgets_max_path_solutions)
            if config.budgets_max_path_solutions is not None
            else NULL_BUDGET
        )
        self._exceeded_budgets = (
            CountingBudget.get_fixed_budget(config.budgets_max_exceeded_budget)
            if config.budgets_max_exceeded_budget is not None
            else NULL_BUDGET
        )

    def reset_time_budget(self) -> None:
        """Restart the global time window."""
        self._time.increment()

    def increment_path_solution_budget(self) -> None:
        self._path_solutions.increment()

    def increment_fail_budget(self) -> None:
        self._fails.increment()

    def increment_exceeded_budget_budget(self) -> None:
        self._exceeded_budgets.increment()

    def time_budget_is_exceeded(self) -> bool:
        return self._time.is_exceeded()

    def fixed_path_solution_budget_is_exceeded(self) -> bool:
        return self._path_solutions.is_exceeded()

    def fixed_fail_budget_is_exceeded(self) -> bool:
        return self._fails.is_exceeded()

    def fixed_exceeded_budget_budgets_is_exceeded(self) -> bool:
        return self._exceeded_budgets.is_exceeded()
```

---

## 13. Choice-point subsystem

### 13.1 `mulib_python/search/choice_points/backtrack.py`

`Backtrack` is a singleton *control-flow* exception.  In the Java source it
extends `MulibControlFlowException` (itself extends `RuntimeException`) and
reuses a single pre-allocated instance to avoid allocating stack traces.

In Python:
- Extend `BaseException` (not `Exception`) so that a bare `except Exception:`
  clause in user code or the solver does **not** accidentally suppress it.
- Use `__new__` singleton pattern (identical pre-allocated instance).
- Override `__init__` to **not** capture a traceback for performance.

```python
from __future__ import annotations


class Backtrack(BaseException):
    """Singleton control-flow exception signalling that the current path
    must be abandoned and a new unexplored ChoiceOption selected.

    Raised by ChoicePointFactory when no valid option can be determined.
    Caught at the MulibExecutor level to initiate the next search step.
    """

    _instance: "Backtrack | None" = None

    def __new__(cls) -> "Backtrack":
        if cls._instance is None:
            instance = super().__new__(cls)
            # Suppress traceback to avoid allocation cost on each raise
            instance.__traceback__ = None
            cls._instance = instance
        return cls._instance

    def __init__(self) -> None:
        # Do NOT call super().__init__() — avoids frame capture
        pass

    @classmethod
    def get_instance(cls) -> "Backtrack":
        if cls._instance is None:
            cls._instance = cls()
        return cls._instance
```

> **Usage pattern in executors:**
> ```python
> try:
>     result = choice_point_factory.bool_choice(se, sbool)
> except Backtrack:
>     # select next ChoiceOption from deque, re-run
>     ...
> ```

### 13.2 `mulib_python/search/choice_points/choice_point_exceeded_budget.py`

```python
from mulib_python.search.budget.budget import Budget


class ChoicePointExceededBudget(Exception):
    """Raised when the fixed actual-choice-point budget is exhausted.

    Signals the executor that this path cannot be deepened further; the
    executor should record an ExceededBudget leaf and move on.
    """

    def __init__(self, exceeded_budget: Budget) -> None:
        self.exceeded_budget = exceeded_budget
        super().__init__(f"Choice point budget exceeded: {exceeded_budget!r}")
```

### 13.3 `mulib_python/search/choice_points/choice_point_factory.py`

Abstract base class for both symbolic and concolic factories.  Mirrors the Java
interface but uses Python ABCs.

**All numeric comparison methods** follow the same three-case pattern.  There
are six comparison operators (lt, gt, eq, not_eq, gte, lte) × four numeric types
(Sint, Slong, Sdouble, Sfloat) = 24 methods, plus two bool methods
(`bool_choice`, `negated_bool_choice`) = 26 base methods.  There are also 26
ID-bearing overloads for coverage tracking = 52 total abstract methods.

Rather than listing all 52 individually, the plan describes the pattern and
instructs the implementer to generate them systematically.

```python
import abc
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from mulib_python.search.executors.symbolic_execution import SymbolicExecution
    from mulib_python.substitutions.primitives.sint import Sint, Sbool
    from mulib_python.substitutions.primitives.slong import Slong
    from mulib_python.substitutions.primitives.sdouble import Sdouble
    from mulib_python.substitutions.primitives.sfloat import Sfloat


class ChoicePointFactory(abc.ABC):
    """Factory that creates or follows choice points during symbolic execution.

    Every method returns True if the constraint being tested holds, and False
    if its negation holds.  The three-case logic is:
      1. Concrete value → evaluate directly.
      2. On known path → follow the pre-determined path.
      3. New choice point → ask the executor, potentially throw Backtrack.
    """

    @classmethod
    def get_instance(
        cls,
        config: "MulibConfig",
        coverage_cfg: object = None,
    ) -> "ChoicePointFactory":
        if config.search_concolic:
            from mulib_python.search.choice_points.concolic_choice_point_factory import (
                ConcolicChoicePointFactory,
            )
            return ConcolicChoicePointFactory.get_instance(config, coverage_cfg)
        else:
            from mulib_python.search.choice_points.symbolic_choice_point_factory import (
                SymbolicChoicePointFactory,
            )
            return SymbolicChoicePointFactory.get_instance(config, coverage_cfg)

    # ── Sint comparisons ──────────────────────────────────────────────────────
    @abc.abstractmethod
    def lt_choice(self, se: "SymbolicExecution", lhs: "Sint", rhs: "Sint") -> bool: ...
    @abc.abstractmethod
    def gt_choice(self, se: "SymbolicExecution", lhs: "Sint", rhs: "Sint") -> bool: ...
    @abc.abstractmethod
    def eq_choice(self, se: "SymbolicExecution", lhs: "Sint", rhs: "Sint") -> bool: ...
    @abc.abstractmethod
    def not_eq_choice(self, se: "SymbolicExecution", lhs: "Sint", rhs: "Sint") -> bool: ...
    @abc.abstractmethod
    def gte_choice(self, se: "SymbolicExecution", lhs: "Sint", rhs: "Sint") -> bool: ...
    @abc.abstractmethod
    def lte_choice(self, se: "SymbolicExecution", lhs: "Sint", rhs: "Sint") -> bool: ...

    # ── Slong comparisons (same signatures, different types) ──────────────────
    # ... (repeat pattern for Slong, Sdouble, Sfloat)

    # ── Sbool ─────────────────────────────────────────────────────────────────
    @abc.abstractmethod
    def bool_choice(self, se: "SymbolicExecution", b: "Sbool") -> bool: ...
    @abc.abstractmethod
    def negated_bool_choice(self, se: "SymbolicExecution", b: "Sbool") -> bool: ...

    # ── ID-bearing overloads (coverage tracking) ──────────────────────────────
    @abc.abstractmethod
    def lt_choice_with_id(self, se: "SymbolicExecution", id: int, lhs: "Sint", rhs: "Sint") -> bool: ...
    # ... (same pattern for all 26 methods)
```

**Implementation note:** The ID-bearing overloads each delegate to the non-ID
method and, if coverage tracking is enabled, also update the `CoverageCfg`.  The
default implementation in `SymbolicChoicePointFactory` handles this.

### 13.4 `mulib_python/search/choice_points/symbolic_choice_point_factory.py`

This is the most complex component.  It implements the three-case distinction
for pure symbolic execution.

#### Class structure

```python
from mulib_python.search.choice_points.choice_point_factory import ChoicePointFactory
from mulib_python.search.choice_points.backtrack import Backtrack
from mulib_python.search.choice_points.choice_point_exceeded_budget import ChoicePointExceededBudget
from mulib_python.search.trees.choice import Choice
from mulib_python.constraints import Not, Lt, Lte, Eq


class SymbolicChoicePointFactory(ChoicePointFactory):
    def __init__(self, config: "MulibConfig", coverage_cfg: object = None) -> None:
        self._config = config
        self._cfg = coverage_cfg
        # Validate consistency between config flag and presence of coverage_cfg
        ...

    @classmethod
    def get_instance(cls, config, coverage_cfg=None) -> "SymbolicChoicePointFactory":
        return cls(config, coverage_cfg)
```

#### `lt_choice` and friends

Each numeric comparison delegates to `_three_case_distinction`:

```python
def lt_choice(self, se, lhs, rhs):
    return self._three_case_distinction(se, Lt.new_instance(lhs, rhs))

def gt_choice(self, se, lhs, rhs):
    return self._three_case_distinction(se, Lt.new_instance(rhs, lhs))

def eq_choice(self, se, lhs, rhs):
    return self._three_case_distinction(se, Eq.new_instance(lhs, rhs))

def not_eq_choice(self, se, lhs, rhs):
    return self._three_case_distinction(se, Not(Eq.new_instance(lhs, rhs)))

def gte_choice(self, se, lhs, rhs):
    return self._three_case_distinction(se, Lte.new_instance(rhs, lhs))

def lte_choice(self, se, lhs, rhs):
    return self._three_case_distinction(se, Lte.new_instance(lhs, rhs))

def bool_choice(self, se, b):
    return self._three_case_distinction(se, b)

def negated_bool_choice(self, se, b):
    return self._three_case_distinction(se, Not(b))
```

(All four numeric types are handled identically — the constraint constructors
accept any `Expression`.)

#### ID-bearing overloads

```python
def lt_choice_with_id(self, se, id_, lhs, rhs):
    return self._choice_template_with_id(se, lambda: self.lt_choice(se, lhs, rhs), id_)

def _choice_template_with_id(self, se, supplier, id_):
    if self._config.transf_cfg_generate_choice_points_with_id:
        self._cfg.set_current_cfg_node_if_necessary(id_)
        result = supplier()
        self._cfg.traverse_current_node_with_decision(id_, result)
        self._cfg.add_choice_for_cfg_node(se.get_current_choice_option().choice, id_)
        return result
    else:
        return supplier()
```

#### Core: `_three_case_distinction`

```python
def _three_case_distinction(self, se: "SymbolicExecution", c: "Constraint") -> bool:
    from mulib_python.constraints import _BoolLiteral  # TRUE/FALSE type

    # Case 1: Concrete boolean — no choice needed
    if isinstance(c, _BoolLiteral):
        return c.value

    current_co = se.get_current_choice_option()

    # Case 2: Still on the known path to a pre-selected ChoiceOption
    maybe_result = self._check_if_still_on_known_path(se)
    if maybe_result is not None:
        return maybe_result

    assert not current_co.is_evaluated, "Should not be evaluated yet"

    # Case 3: New choice point
    return self._determine_boolean_with_new_binary_choice(se, c, current_co)
```

#### `_check_if_still_on_known_path`

```python
def _check_if_still_on_known_path(
    self,
    se: "SymbolicExecution",
) -> "bool | None":
    ebm = se.get_execution_budget_manager()
    if ebm.fixed_actual_choice_point_budget_is_exceeded():
        raise ChoicePointExceededBudget(ebm.fixed_actual_choice_point_budget)

    if se.transition_to_next_choice_option_and_check_if_on_known_path():
        current_co = se.get_current_choice_option()
        assert len(current_co.choice.choice_options) == 2, \
            "Boolean choices must always have exactly two options."
        return current_co.choice_option_number == 0
    return None
```

#### `_determine_boolean_with_new_binary_choice`

```python
def _determine_boolean_with_new_binary_choice(
    self,
    se: "SymbolicExecution",
    constraint: "Constraint",
    current_co: "ChoiceOption",
) -> bool:
    from mulib_python.substitutions.primitives.sint import SymSbool

    # Unwrap SymSbool if needed
    if isinstance(constraint, SymSbool):
        constraint = constraint.represented_constraint

    new_choice = Choice(current_co, constraint, Not(constraint))

    if self._config.cfg_use_guidance_during_execution:
        possible_next = self._decision_based_on_cfg(se, new_choice)
    else:
        possible_next = self._decision_not_based_on_cfg(se, new_choice)

    if possible_next is None:
        raise Backtrack.get_instance()

    return possible_next.choice_option_number == 0
```

#### `_decision_not_based_on_cfg`

```python
@staticmethod
def _decision_not_based_on_cfg(
    se: "SymbolicExecution",
    new_choice: Choice,
) -> "ChoiceOption | None":
    all_options = new_choice.choice_options
    chosen = se.decide_on_next_choice_option_during_execution(all_options)

    if chosen is None:
        not_chosen = all_options
    else:
        other_number = 1 - chosen.choice_option_number
        not_chosen = [new_choice.get_option(other_number)]

    se.notify_new_choice(new_choice.depth, not_chosen)
    return chosen
```

#### `_decision_based_on_cfg` (coverage-guided)

```python
@staticmethod
def _decision_based_on_cfg(
    se: "SymbolicExecution",
    new_choice: Choice,
    cfg: object,
) -> "ChoiceOption | None":
    coverage_info = cfg.get_coverage_information_for_current_node()
    ALL_COVERED = ...  # import from coverage_cfg module

    if coverage_info in (ALL_COVERED, BOTH_NOT_COVERED, NO_INFO):
        return SymbolicChoicePointFactory._decision_not_based_on_cfg(se, new_choice)

    all_options = new_choice.choice_options
    if coverage_info == TRUE_BRANCH_NOT_COVERED:
        chosen, other = all_options[0], all_options[1]
    else:
        chosen, other = all_options[1], all_options[0]

    reordered = [chosen, other]
    picked = se.decide_on_next_choice_option_during_execution(reordered)
    not_chosen = [other if picked is chosen else chosen]
    se.notify_new_choice(new_choice.depth, not_chosen if picked else reordered)
    return picked
```

---

## 14. `MulibConfig` integration (stubs for Phase 5)

Phase 5 components reference the following config attributes.  If `MulibConfig`
does not yet exist, create a minimal dataclass stub.  The implementation agent
should **not** build the full config — that is a later phase.

```python
# mulib_python/config.py  (stub — will be expanded in a later phase)
from dataclasses import dataclass, field
from typing import Optional
from mulib_python.search.strategy import SearchStrategy

@dataclass
class MulibConfig:
    # ── Tree / deque ──────────────────────────────────────────────────────────
    tree_indentation: str = "  "
    tree_enlist_leaves: bool = False
    search_choice_option_deque_type: str = "DIRECT_ACCESS"   # "SIMPLE" | "DIRECT_ACCESS"
    search_additional_parallel_strategies: list = field(default_factory=list)

    # ── Search mode ───────────────────────────────────────────────────────────
    search_concolic: bool = False
    cfg_use_guidance_during_execution: bool = False
    transf_cfg_generate_choice_points_with_id: bool = False

    # ── Per-execution budgets ─────────────────────────────────────────────────
    budgets_fixed_actual_cp: Optional[int] = None    # max choice points per path
    budgets_incr_actual_cp: Optional[int] = None     # incremental CP budget (IDDFS)

    # ── Global budgets ────────────────────────────────────────────────────────
    budgets_global_time_in_nanoseconds: Optional[int] = None
    budgets_max_fails: Optional[int] = None
    budgets_max_path_solutions: Optional[int] = None
    budgets_max_exceeded_budget: Optional[int] = None
```

---

## 15. Thread-safety requirements

### What must be thread-safe

| Component | Reason | Mechanism |
|-----------|--------|-----------|
| `ChoiceOptionDeque` (both impls) | Multiple `MulibExecutor` threads read/write concurrently | `threading.Lock` per instance |
| `SearchTree.solutions_list` | Multiple executor threads append solutions | `_SynchronizedList` when parallel config present |
| `SearchTree.fails_list` | Same | `_SynchronizedList` |
| `SearchTree.exceeded_budget_list` | Same | `_SynchronizedList` |

### What is intentionally **not** thread-safe

| Component | Reason |
|-----------|--------|
| `TreeNode` / `Choice` / `ChoiceOption` | Each node is owned by exactly one executor; concurrent modification of the same node is undefined |
| `ExecutionBudgetManager` | Copied per-run; each run uses its own instance |
| `ChoicePointFactory` | Stateless / per-run config references only |

### `_SynchronizedList` usage rule

Only use `_SynchronizedList` if
`config.search_additional_parallel_strategies` is non-empty.  Otherwise use
plain `list` to avoid unnecessary locking overhead in the common single-threaded
case.

---

## 16. The `Backtrack` mechanism — detailed flow

The backtrack exception is the **only** control-flow mechanism for aborting an
execution path.  Here is the full lifecycle:

```
SymbolicExecution.run(target_choice_option)
    │
    ├── replay known path  (follows choices in the deque path to target)
    │       │
    │       └── ChoicePointFactory.xxx_choice()
    │               Case 2: still on known path → returns True/False
    │
    └── new territory: encounters a new fork
            │
            └── ChoicePointFactory.xxx_choice()
                    Case 3: _determine_boolean_with_new_binary_choice()
                        │
                        ├── se.decide_on_next_choice_option() returns chosen option
                        │       (SAT-checks option 0, falls through to option 1 if unsat)
                        │
                        ├── if both options are unsatisfiable → returns None
                        │       → raise Backtrack.get_instance()
                        │
                        └── se.notify_new_choice(depth, not_chosen_options)
                                → inserts not_chosen_options into the deque
```

**At the executor level** (`AbstractMulibExecutor.get_next_path_solution()`):

```python
while True:
    target = deque.poll_last()   # DFS example
    if target is None:
        break  # search complete
    path_to_target = SearchTree.get_path_to(target)
    ebm = execution_budget_manager.copy_from_prototype()
    try:
        result = symbolic_execution.run(path_to_target, ebm)
        search_tree.add_to_path_solutions(result)
    except Backtrack:
        pass  # dead end; try next option
    except ChoicePointExceededBudget as e:
        target.set_budget_exceeded(e.exceeded_budget)
        search_tree.add_to_exceeded_budgets(target._child)
    except MulibUserFail:
        target.set_explicitly_failed()
        search_tree.add_to_fails(target._child)
```

---

## 17. `__init__.py` files

### `mulib_python/search/__init__.py`

```python
from mulib_python.search.strategy import SearchStrategy
```

### `mulib_python/search/trees/__init__.py`

```python
from mulib_python.search.trees.tree_node import TreeNode
from mulib_python.search.trees.choice import Choice, ChoiceOption
from mulib_python.search.trees.path_solution import PathSolution, ThrowablePathSolution
from mulib_python.search.trees.fail import Fail
from mulib_python.search.trees.exceeded_budget import ExceededBudget
from mulib_python.search.trees.choice_option_deque import ChoiceOptionDeque
from mulib_python.search.trees.simple_choice_option_deque import SimpleChoiceOptionDeque
from mulib_python.search.trees.direct_access_choice_option_deque import DirectAccessChoiceOptionDeque
from mulib_python.search.trees.search_tree import SearchTree, AccumulatedChoiceOptionConstraints
```

### `mulib_python/search/budget/__init__.py`

```python
from mulib_python.search.budget.budget import Budget
from mulib_python.search.budget.null_budget import NullBudget, INSTANCE as NULL_BUDGET
from mulib_python.search.budget.time_budget import TimeBudget
from mulib_python.search.budget.counting_budget import CountingBudget
from mulib_python.search.budget.execution_budget_manager import ExecutionBudgetManager
from mulib_python.search.budget.global_execution_budget_manager import GlobalExecutionBudgetManager
```

### `mulib_python/search/choice_points/__init__.py`

```python
from mulib_python.search.choice_points.backtrack import Backtrack
from mulib_python.search.choice_points.choice_point_exceeded_budget import ChoicePointExceededBudget
from mulib_python.search.choice_points.choice_point_factory import ChoicePointFactory
from mulib_python.search.choice_points.symbolic_choice_point_factory import SymbolicChoicePointFactory
```

---

## 18. Updates to existing files

### `mulib_python/exceptions.py`

Add:

```python
class IllegalTreeModificationException(MulibRuntimeException):
    """Raised when an illegal modification to the search tree is attempted."""

class IllegalTreeAccessException(MulibRuntimeException):
    """Raised when accessing a tree node in an invalid state."""

class MulibControlFlowException(MulibException):
    """Base for control-flow exceptions used internally (e.g. Backtrack)."""
```

---

## 19. Testing requirements

Create `mulib_python/search/tests/` with:

### `test_choice.py`

- Test `Choice` construction with 1, 2, and N constraints.
- Test `ChoiceOption` state machine: legal and illegal transitions.
- Test `set_satisfiable()` → `set_child()` (EVALUATED).
- Test `set_unsatisfiable()` reverts to `UNSATISFIABLE` state (not `EVALUATED`).
- Test `set_explicitly_failed()` similarly.
- Test `set_budget_exceeded()` rejects incremental budget.
- Test `option_constraint.setter` raises after evaluation.
- Test `add_partner_class_constraint()` raises after evaluation.
- Test `_check_child_unset()` raises on double-set.
- Test `constraint_was_modified_after_initial_sat_check` flag survives state transitions.

### `test_search_tree.py`

- Test `get_path_to()` returns correct ordered path.
- Test `get_deepest_shared_ancestor()` for root, sibling, and deep options.
- Test `get_path_between()` returns correct intermediate nodes.
- Test `get_all_constraints_for_choice_option()` accumulates constraints and
  partner constraints in order.
- Test `add_to_fails()` respects `enlist_leaves` flag.

### `test_deques.py`

- Test `SimpleChoiceOptionDeque` insert/poll semantics for DFS (poll_last),
  BFS (poll_first), and in-order insert.
- Test `DirectAccessChoiceOptionDeque` same scenarios.
- Test both deques' `request()` method (concurrent racing not tested here — see
  threading tests).
- Test both deques' `set_empty()`.
- Test `get_min_max_depth()` on empty and populated deques.

### `test_budget.py`

- Test `NullBudget` is never exceeded.
- Test `TimeBudget.increment()` resets the clock.
- Test `TimeBudget.is_exceeded()` returns `True` after max duration passes.
- Test `CountingBudget` fixed: exceeded after `max_number` increments.
- Test `CountingBudget.copy_from_prototype()` resets the counter.
- Test `ExecutionBudgetManager.copy_from_prototype()` returns self when
  `should_not_be_copied` is `True`.

### `test_backtrack.py`

- Verify `Backtrack.get_instance()` returns the same object on repeated calls.
- Verify `Backtrack` is a subclass of `BaseException`, not `Exception`.
- Verify `raise Backtrack.get_instance()` can be caught with
  `except Backtrack:`.

### `test_choice_point_factory.py`

- Test `SymbolicChoicePointFactory._three_case_distinction()` for:
  - Case 1: concrete `TRUE`/`FALSE` constraints.
  - Case 2: mock `se.transition_to_next_choice_option_and_check_if_on_known_path()`
    returning `True` with a mock ChoiceOption of number 0 or 1.
  - Case 3: new choice; verify `Choice` node is created with 2 options;
    verify `Backtrack` is raised when `se.decide_on_next_choice_option_during_execution()`
    returns `None`.
- Test budget exceeded raises `ChoicePointExceededBudget`.

---

## 20. Summary of key Python–Java mapping decisions

| Java pattern | Python equivalent | Rationale |
|---|---|---|
| `synchronized` method | `threading.Lock` + `with self._lock:` | Explicit locking; no implicit monitor |
| `Optional<T>` return | `T \| None` return | Pythonic; avoids Optional wrapper overhead |
| `ArrayDeque<T>` | `collections.deque` | O(1) at both ends, same semantics |
| `LinkedList<T>` | `collections.deque` | Same; deque is the right Python tool |
| `System.nanoTime()` | `time.monotonic_ns()` | Monotonic, nanosecond precision (Python 3.7+) |
| Inner class `Choice.ChoiceOption` | Top-level `ChoiceOption` in `choice.py` with `_choice` back-ref | Avoids non-idiomatic Java inner-class pattern |
| `byte` state field | `int` constant | Python has no byte; `int` with bitmask ops is equivalent |
| `(byte) -128` | `0x80` | Correct signed→unsigned reinterpretation |
| `Collections.synchronizedList` | `_SynchronizedList` wrapper | Explicit; thread-safe append + iteration |
| Singleton `Backtrack` | `__new__` pattern + `get_instance()` | Same pre-allocated singleton, no traceback allocation |
| `throw Backtrack.getInstance()` | `raise Backtrack.get_instance()` | Direct equivalent |
| `List.of(...)` (immutable) | `tuple(...)` | Python's canonical immutable sequence |
| `instanceof Sbool.ConcSbool` | `isinstance(c, _BoolLiteral)` | Uses Phase 1 concrete constraint node |
| Java `assert` | `assert` in Python (non-production) | Keep; strip with `-O` in production |
