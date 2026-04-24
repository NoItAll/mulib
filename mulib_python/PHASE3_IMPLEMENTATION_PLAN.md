# Phase 3 Implementation Plan: Z3 Solver Integration

## Overview

Phase 3 adds a full Z3-backed constraint solver to `mulib_python/`. It builds on the
immutable expression and constraint AST nodes from Phase 1
(`expressions.py` / `constraints.py`) without modifying them.

The Java reference implementation lives in:
- `AbstractZ3SolverManager` – core translation logic and model labeling
- `Z3IncrementalSolverManager` – incremental (push/pop) variant
- `Z3GlobalLearningSolverManager` – global-scope, implication-guarded variant
- `AbstractIncrementalEnabledSolverManager` – backtracking bookkeeping template
- `IncrementalSolverState` – per-level constraint + object-representation tracking
- `ArrayHistorySolverRepresentation` – nested store-chain for array theory

---

## 1. Files to Create

```
mulib_python/
├── expressions.py          (Phase 1, unchanged)
├── constraints.py          (Phase 1, unchanged)
├── exceptions.py           (Phase 1, unchanged)
├── substitutions.py        (NEW) — symbolic primitive leaf nodes (Sint, Sbool, Sfloat, …)
├── solution.py             (NEW) — Labels + Solution data classes
├── solver_manager.py       (NEW) — SolverManager ABC
├── incremental_state.py    (NEW) — IncrementalSolverState + per-level bookkeeping
├── array_repr.py           (NEW) — ArrayHistorySolverRepresentation + ArraySolverRepresentation
├── z3_adapter.py           (NEW) — Z3MulibAdapter: AST → z3 expression translation
├── z3_solver_manager.py    (NEW) — AbstractZ3SolverManager + two concrete subclasses
└── __init__.py             (updated to export public API)
```

### File purposes

| File | Java equivalent | Purpose |
|---|---|---|
| `substitutions.py` | `Sint`, `Sbool`, `Sfloat`, `Slong`, `Sdouble`, `Sfloat`, `Sshort`, `Sbyte` | Leaf symbolic variables and concrete wrappers used by solver |
| `solution.py` | `Labels`, `StdLabels`, `Solution` | Immutable result containers |
| `solver_manager.py` | `SolverManager` interface | Abstract base class defining the public API |
| `incremental_state.py` | `IncrementalSolverState` | Backtracking-level bookkeeping; maps object IDs to representations |
| `array_repr.py` | `ArrayHistorySolverRepresentation`, `ArraySolverRepresentation`, `PrimitiveValuedArraySolverRepresentation` | High-level array theory (store-chain, select-constraint generation) |
| `z3_adapter.py` | `Z3MulibAdapter` | Translates every expression/constraint node into a `z3.ExprRef` |
| `z3_solver_manager.py` | `AbstractZ3SolverManager`, `Z3IncrementalSolverManager`, `Z3GlobalLearningSolverManager` | Z3-backed solver with incremental and global-learning modes |

---

## 2. `substitutions.py` — Symbolic Primitive Leaves

The adapter needs typed leaf nodes (concrete + symbolic) to decide Z3 sort and to
extract model values. These correspond to Java's `Sint`, `Sbool`, `Slong`, etc.

```python
# mulib_python/substitutions.py
from __future__ import annotations
from dataclasses import dataclass, field
from typing import Optional, Union
import abc

# ---- numeric types --------------------------------------------------------

class SymbolicVar(abc.ABC):
    """Marker base for all symbolic/concrete leaf variables."""

# ---- Concrete wrappers ----

@dataclass(frozen=True)
class ConcInt(SymbolicVar):
    value: int          # Python int; covers Java Sint, Sshort, Sbyte, Schar, Slong

@dataclass(frozen=True)
class ConcFloat(SymbolicVar):
    value: float        # Python float; covers Java Sfloat, Sdouble

@dataclass(frozen=True)
class ConcBool(SymbolicVar):
    value: bool

# ---- Symbolic leaves ----

@dataclass(frozen=True)
class SymInt(SymbolicVar):
    """Symbolic integer leaf (covers Sint, Slong).
    
    Attributes
    ----------
    id : str
        Unique identifier used as Z3 const name.
    is_long : bool
        True when this represents a Java `long` (64-bit); False for `int` (32-bit).
    """
    id: str
    is_long: bool = False

@dataclass(frozen=True)
class SymFloat(SymbolicVar):
    """Symbolic real leaf (covers Sfloat, Sdouble).
    
    Attributes
    ----------
    id : str
        Unique identifier.
    is_double : bool
        True for double precision.
    """
    id: str
    is_double: bool = True

@dataclass(frozen=True)
class SymBool(SymbolicVar):
    """Symbolic boolean leaf.
    
    Attributes
    ----------
    id : str
        Unique identifier.
    """
    id: str

# Type alias for any leaf
Primitive = Union[ConcInt, ConcFloat, ConcBool, SymInt, SymFloat, SymBool]
```

**Design notes:**
- `is_long` drives the BitVec width (64 vs 32) for bitwise operations.
- These dataclasses are used as keys in the adapter's expression cache (they are
  hashable and frozen).
- The adapter treats `SymBool` specially when `treat_bools_as_ints=True` (creates an
  Int Z3 const rather than a Bool const).

---

## 3. `solution.py` — Labels and Solution

Direct Python translation of Java's `Labels` / `StdLabels` / `Solution`.

```python
# mulib_python/solution.py
from __future__ import annotations
from typing import Any, Dict, Collection
from mulib_python.substitutions import Primitive


class Labels:
    """Immutable container mapping symbolic-variable names to their concrete labels.

    The key ``"return"`` always holds the return value of the search-region
    execution.

    Parameters
    ----------
    id_to_var : dict[str, Primitive]
        Maps each remembered name to its search-region representation
        (a :class:`~mulib_python.substitutions.Primitive` or other symbolic object).
    id_to_label : dict[str, Any]
        Maps each remembered name to the concrete Python value assigned by
        the solver (``int``, ``float``, ``bool``, ``list``, etc.).
    """

    __slots__ = ("_id_to_var", "_id_to_label")

    def __init__(
        self,
        id_to_var: Dict[str, Any],
        id_to_label: Dict[str, Any],
    ) -> None:
        self._id_to_var = dict(id_to_var)
        self._id_to_label = dict(id_to_label)

    def get_label_for_id(self, name: str) -> Any:
        """Return the concrete label for *name*.  Returns ``None`` if absent."""
        return self._id_to_label.get(name)

    def get_named_var(self, name: str) -> Any:
        """Return the search-region representation for *name*."""
        return self._id_to_var.get(name)

    @property
    def id_to_named_var(self) -> Dict[str, Any]:
        return dict(self._id_to_var)

    @property
    def id_to_label(self) -> Dict[str, Any]:
        return dict(self._id_to_label)

    @property
    def names(self) -> Collection[str]:
        return self._id_to_label.keys()

    def __repr__(self) -> str:
        return f"Labels({self._id_to_label!r})"


class Solution:
    """A single solution to the search region.

    Attributes
    ----------
    return_value : Any
        The concrete return value produced by the search region.
    labels : Labels
        All remembered variables and their concrete values, including
        ``"return"``.
    """

    __slots__ = ("return_value", "labels")

    def __init__(self, return_value: Any, labels: Labels) -> None:
        self.return_value = return_value
        self.labels = labels

    def __repr__(self) -> str:
        return f"Solution(return_value={self.return_value!r}, labels={self.labels!r})"
```

---

## 4. `solver_manager.py` — Abstract SolverManager

```python
# mulib_python/solver_manager.py
from __future__ import annotations
import abc
from typing import Any, List
from mulib_python.constraints import Constraint, ArrayAccessConstraint, ArrayInitializationConstraint
from mulib_python.solution import Solution


class SolverManager(abc.ABC):
    """Abstract base class mirroring Java's ``SolverManager`` interface.

    Each concrete subclass must implement all abstract methods.
    Backtracking points form a stack; ``add_constraint_after_new_backtracking_point``
    opens a new scope and ``backtrack_once`` / ``backtrack`` close scope(s).
    """

    # ---- constraint insertion -----------------------------------------------

    @abc.abstractmethod
    def add_constraint(self, constraint: Constraint) -> None:
        """Add *constraint* to the current scope without creating a new level."""

    @abc.abstractmethod
    def add_constraint_after_new_backtracking_point(self, constraint: Constraint) -> None:
        """Open a new backtracking scope and add *constraint* inside it."""

    @abc.abstractmethod
    def add_array_constraint(self, ac: "ArrayAccessConstraint | ArrayInitializationConstraint") -> None:
        """Add an array-related constraint (SELECT, STORE, or initialization)."""

    # ---- satisfiability -----------------------------------------------------

    @abc.abstractmethod
    def check_with_new_constraint(self, constraint: Constraint) -> bool:
        """Return ``True`` iff adding *constraint* keeps the stack satisfiable.

        The constraint is **not** persistently added.
        """

    @abc.abstractmethod
    def is_satisfiable(self) -> bool:
        """Return ``True`` iff the current constraint stack is satisfiable."""

    # ---- backtracking -------------------------------------------------------

    @abc.abstractmethod
    def backtrack_once(self) -> None:
        """Remove the most-recent backtracking scope."""

    @abc.abstractmethod
    def backtrack(self, n: int) -> None:
        """Remove the *n* most-recent backtracking scopes."""

    @abc.abstractmethod
    def backtrack_all(self) -> None:
        """Remove all backtracking scopes."""

    # ---- labeling -----------------------------------------------------------

    @abc.abstractmethod
    def get_label(self, var: Any) -> Any:
        """Evaluate *var* in the current model and return a concrete Python value."""

    @abc.abstractmethod
    def label_solution(
        self,
        return_value: Any,
        remembered: "dict[str, Any]",
    ) -> Solution:
        """Construct a :class:`~mulib_python.solution.Solution`.

        Parameters
        ----------
        return_value:
            The symbolic (or concrete) return value of the search-region.
        remembered:
            ``{name: symbolic_var}`` pairs for variables the user remembered
            during the search.
        """

    @abc.abstractmethod
    def reset_labels(self) -> None:
        """Clear the label cache so subsequent calls re-evaluate from the model."""

    @abc.abstractmethod
    def register_label_pair(self, search_repr: Any, label: Any) -> None:
        """Cache a ``(search-region representation, label)`` pair."""

    # ---- misc ---------------------------------------------------------------

    @abc.abstractmethod
    def get_level(self) -> int:
        """Return the current depth (number of open backtracking scopes)."""

    @abc.abstractmethod
    def get_up_to_n_solutions(self, initial_solution: Solution, n: int) -> List[Solution]:
        """Generate up to *n* additional distinct solutions on the current path."""

    @abc.abstractmethod
    def shutdown(self) -> None:
        """Release all solver resources."""
```

---

## 5. `incremental_state.py` — Backtracking Bookkeeping

This file mirrors `IncrementalSolverState` and its inner class
`SymbolicPartnerClassObjectStates`. It does **not** hold any Z3-specific objects;
it only tracks Python-level bookkeeping.

```python
# mulib_python/incremental_state.py
from __future__ import annotations
from collections import deque
from typing import Any, Deque, Dict, List, Optional, TYPE_CHECKING

if TYPE_CHECKING:
    from mulib_python.constraints import Constraint, ArrayAccessConstraint, ArrayInitializationConstraint
    from mulib_python.substitutions import ConcInt, SymInt   # used as array IDs


class PartnerClassObjectRepresentation:
    """Maintains a versioned stack of representations for a single object ID.

    Each entry is ``(level, representation)``.  The newest representation is at
    the top of the internal stack.  On backtracking, representations whose level
    exceeds the new level are discarded.

    Mirrors Java ``IncrementalSolverState.PartnerClassObjectRepresentation<R>``.
    """

    __slots__ = ("_id", "_stack")

    def __init__(self, obj_id: Any) -> None:
        self._id = obj_id
        self._stack: List[tuple[int, Any]] = []

    def add_new_representation(self, representation: Any, level: int) -> None:
        self._stack.append((level, representation))

    def get_newest_representation(self) -> Optional[Any]:
        return self._stack[-1][1] if self._stack else None

    def get_representation_for_depth(self, depth: int) -> Optional[Any]:
        """Return the most-recent representation whose level is ≤ *depth*."""
        for lvl, rep in reversed(self._stack):
            if lvl <= depth:
                return rep
        return None

    def pop_until_level(self, level: int) -> None:
        """Remove representations added at levels > *level*."""
        while self._stack and self._stack[-1][0] > level:
            self._stack.pop()


class SymbolicObjectStates:
    """Maps object IDs to their ``PartnerClassObjectRepresentation`` wrappers.

    Mirrors Java ``IncrementalSolverState.SymbolicPartnerClassObjectStates``.
    Holds a back-reference to the solver manager so that it can call
    ``add_constraint`` when adding metadata constraints during array/object
    initialization.

    Parameters
    ----------
    solver_manager:
        The owning :class:`~mulib_python.solver_manager.SolverManager`; used
        for ``add_metadata_constraint``.
    """

    def __init__(self, solver_manager: Any) -> None:
        self._solver_manager = solver_manager
        self._id_to_repr: Dict[Any, PartnerClassObjectRepresentation] = {}

    def add_representation_for_id(self, obj_id: Any, representation: Any, level: int) -> None:
        pcor = PartnerClassObjectRepresentation(obj_id)
        pcor.add_new_representation(representation, level)
        self._id_to_repr[obj_id] = pcor

    def get_representation_for_id(self, obj_id: Any) -> Optional[PartnerClassObjectRepresentation]:
        return self._id_to_repr.get(obj_id)

    def add_metadata_constraint(self, constraint: "Constraint") -> None:
        """Delegate directly to the solver manager."""
        self._solver_manager.add_constraint(constraint)

    def get_current_level(self) -> int:
        return self._solver_manager.get_level()

    def pop_all_representations_above_level(self, level: int) -> None:
        for pcor in self._id_to_repr.values():
            pcor.pop_until_level(level)


class IncrementalSolverState:
    """Tracks per-level constraints and object representations.

    This is a pure-Python bookkeeping layer; no Z3 objects are stored here.

    Mirrors Java ``IncrementalSolverState<AR, PR>``.

    Attributes
    ----------
    level : int
        Current depth (number of open backtracking scopes).
    """

    def __init__(self, solver_manager: Any) -> None:
        # Stack of constraints mirroring each backtracking scope.
        # Each element represents one scope's conjunction of constraints.
        self._constraints: Deque["Constraint"] = deque()
        self._level: int = 0

        # Per-level list-of-lists for array and partner-class constraints.
        # Index 0 holds level-0 constraints, index 1 holds level-1, etc.
        self._partner_class_constraints: List[List[Any]] = []

        # Invalidation cache (mirrors Java's allPartnerClassObjectConstraints field)
        self._all_pco_constraints_cache: Optional[List[Any]] = None

        # Both array states and object states use the same SymbolicObjectStates
        # class; the Java side uses the same class parameterized differently.
        self.symbolic_array_states = SymbolicObjectStates(solver_manager)
        self.symbolic_object_states = SymbolicObjectStates(solver_manager)

    # ---- level accessors ----------------------------------------------------

    @property
    def level(self) -> int:
        return self._level

    # ---- constraint tracking ------------------------------------------------

    def add_constraint(self, c: "Constraint") -> None:
        """Conjoin *c* with the top-scope constraint (does NOT push to solver)."""
        from mulib_python.constraints import And
        if self._constraints:
            prev = self._constraints[0]
            self._constraints[0] = And.new_instance(prev, c)
        # If no scope is open yet, nothing to track; the solver itself holds it.

    def push_constraint(self, c: "Constraint") -> None:
        """Open a new scope whose root constraint is *c*."""
        self._constraints.appendleft(c)
        self._level += 1
        while len(self._partner_class_constraints) <= self._level:
            self._partner_class_constraints.append([])

    def pop_constraint(self) -> None:
        """Close the most-recent scope; decrements level."""
        self._pop_partner_class_constraints_for_level()
        if self._constraints:
            self._constraints.popleft()
        self._level -= 1

    def get_constraints(self) -> Deque["Constraint"]:
        return self._constraints

    # ---- partner-class / array constraint tracking --------------------------

    def add_array_constraint(self, ac: Any) -> None:
        self._all_pco_constraints_cache = None
        self._ensure_level_list(self._level)
        self._partner_class_constraints[self._level].append(ac)

    def add_partner_class_object_constraint(self, pc: Any) -> None:
        self._all_pco_constraints_cache = None
        self._ensure_level_list(self._level)
        self._partner_class_constraints[self._level].append(pc)

    def _ensure_level_list(self, level: int) -> None:
        while len(self._partner_class_constraints) <= level:
            self._partner_class_constraints.append([])

    def _pop_partner_class_constraints_for_level(self) -> None:
        """Remove all object constraints recorded at the current level."""
        self._all_pco_constraints_cache = None
        if self._level < len(self._partner_class_constraints):
            self._partner_class_constraints[self._level].clear()
        # Also roll back representation stacks.
        self.symbolic_array_states.pop_all_representations_above_level(self._level - 1)
        self.symbolic_object_states.pop_all_representations_above_level(self._level - 1)

    def get_all_partner_class_object_constraints(self) -> List[Any]:
        if self._all_pco_constraints_cache is not None:
            return self._all_pco_constraints_cache
        result: List[Any] = []
        for lst in self._partner_class_constraints:
            result.extend(lst)
        self._all_pco_constraints_cache = result
        return result

    # ---- array / object representations -------------------------------------

    def initialize_array_representation(
        self, constraint: "ArrayInitializationConstraint", representation: Any
    ) -> None:
        obj_id = constraint.partner_class_object_id
        self.symbolic_array_states.add_representation_for_id(obj_id, representation, self._level)

    def initialize_partner_class_object_representation(
        self, constraint: Any, representation: Any
    ) -> None:
        obj_id = constraint.partner_class_object_id
        self.symbolic_object_states.add_representation_for_id(obj_id, representation, self._level)

    def get_current_array_representation(self, array_id: Any) -> Optional[Any]:
        pcor = self.symbolic_array_states.get_representation_for_id(array_id)
        return pcor.get_newest_representation() if pcor else None

    def get_current_partner_class_object_representation(self, obj_id: Any) -> Optional[Any]:
        pcor = self.symbolic_object_states.get_representation_for_id(obj_id)
        return pcor.get_newest_representation() if pcor else None

    def add_new_representation_for_array(
        self, constraint: "ArrayAccessConstraint", new_repr: Any
    ) -> None:
        pcor = self.symbolic_array_states.get_representation_for_id(
            constraint.partner_class_object_id
        )
        assert pcor is not None, "Array must be initialized before adding new representations"
        pcor.add_new_representation(new_repr, self._level)

    def add_new_representation_for_partner_class_object(
        self, constraint: Any, new_repr: Any
    ) -> None:
        pcor = self.symbolic_object_states.get_representation_for_id(
            constraint.partner_class_object_id
        )
        assert pcor is not None
        pcor.add_new_representation(new_repr, self._level)

    # ---- clear --------------------------------------------------------------

    def clear(self) -> None:
        self._constraints.clear()
        self._partner_class_constraints.clear()
        self._all_pco_constraints_cache = None
        self._level = 0
```

---

## 6. `array_repr.py` — High-Level Array Theory

This mirrors `ArrayHistorySolverRepresentation` and
`PrimitiveValuedArraySolverRepresentation`. The key design decision is that the
array theory is expressed entirely in mulib's constraint AST; the resulting
`Constraint` objects are then fed back through `SolverManager.add_constraint`,
which translates them to Z3 normally.

```python
# mulib_python/array_repr.py
from __future__ import annotations
from dataclasses import dataclass, field
from typing import Any, Callable, List, Optional, Set, TYPE_CHECKING

from mulib_python.constraints import (
    And, Or, Not, Eq, Implication, Equivalence, BoolIte, Constraint, TRUE, FALSE,
)
from mulib_python.substitutions import ConcInt, ConcBool, ConcFloat

if TYPE_CHECKING:
    from mulib_python.substitutions import Primitive


# -----------------------------------------------------------------------
# ArrayAccessRecord — a single guarded (index, value) pair
# -----------------------------------------------------------------------

@dataclass
class ArrayAccessRecord:
    """Mirrors Java's private ``ArrayAccessSolverRepresentation``.

    Attributes
    ----------
    guard : Constraint
        Condition under which this access is valid (typically TRUE for simple
        arrays, or an equality of IDs for aliasing arrays).
    index : Any
        The symbolic or concrete index expression (an ``Expression`` node).
    value : Any
        The symbolic or concrete value expression (a ``Primitive`` leaf or
        ``Expression`` node).
    """
    guard: Constraint
    index: Any
    value: Any

    @property
    def index_is_valid(self) -> Callable[[Any], Constraint]:
        """Returns a function ``f(i) -> guard ∧ (index == i)``."""
        def check(i: Any) -> Constraint:
            return And.new_instance(self.guard, Eq(self.index, i))
        return check


def _default_for_type(value_type: type) -> Any:
    """Return the default element for arrays of *value_type*.

    Mirrors the switch-case in ``ArrayHistorySolverRepresentation.__init__``.
    """
    if value_type is bool:
        return ConcBool(False)
    if value_type is float:
        return ConcFloat(0.0)
    if value_type is int:
        return ConcInt(0)
    # Reference types (object arrays, nested arrays): -1 signals null
    return ConcInt(-1)


# -----------------------------------------------------------------------
# ArrayHistorySolverRepresentation
# -----------------------------------------------------------------------

class ArrayHistorySolverRepresentation:
    """Nested store-chain representation of an array's history.

    Mirrors Java ``ArrayHistorySolverRepresentation``.

    A new STORE creates a new wrapper node referencing the previous
    representation; SELECT constraints are generated by walking the chain
    and building implications.

    Parameters
    ----------
    initial_selects : list[ArrayAccessRecord]
        Records for values that are known at initialization time.
    value_type : type
        Python element type (``int``, ``float``, ``bool``).
    _store_record : ArrayAccessRecord | None
        Internal; set when this node represents a store on top of *_before_store*.
    _before_store : ArrayHistorySolverRepresentation | None
        Internal; the representation prior to the store.
    """

    def __init__(
        self,
        initial_selects: List[ArrayAccessRecord],
        value_type: type,
        *,
        _store_record: Optional[ArrayAccessRecord] = None,
        _before_store: Optional["ArrayHistorySolverRepresentation"] = None,
    ) -> None:
        self._selects: List[ArrayAccessRecord] = list(initial_selects)
        self._store: Optional[ArrayAccessRecord] = _store_record
        self._before_store: Optional["ArrayHistorySolverRepresentation"] = _before_store
        self._default_value: Any = _default_for_type(value_type)
        self._value_type = value_type

    def copy(self) -> "ArrayHistorySolverRepresentation":
        """Shallow copy (selects list is copied, store chain is shared)."""
        new = ArrayHistorySolverRepresentation.__new__(ArrayHistorySolverRepresentation)
        new._selects = list(self._selects)
        new._store = self._store
        new._before_store = self._before_store
        new._default_value = self._default_value
        new._value_type = self._value_type
        return new

    @property
    def is_empty(self) -> bool:
        return self._store is None and not self._selects

    # ---- store ---------------------------------------------------------------

    def store(
        self,
        guard: Constraint,
        index: Any,
        value: Any,
    ) -> "ArrayHistorySolverRepresentation":
        """Return a new history node representing a store on top of this one.

        The new node wraps ``self`` as ``_before_store``.
        """
        if guard is FALSE:
            return self
        new = ArrayHistorySolverRepresentation.__new__(ArrayHistorySolverRepresentation)
        new._selects = []
        new._store = ArrayAccessRecord(guard=guard, index=index, value=value)
        new._before_store = self
        new._default_value = self._default_value
        new._value_type = self._value_type
        return new

    # ---- select --------------------------------------------------------------

    def select(
        self,
        guard: Constraint,
        index: Any,
        value: Any,
        *,
        array_is_completely_initialized: bool,
        allow_aliasing: bool,
        value_type: type,
        enforce_default_for_unknowns: bool,
    ) -> Constraint:
        """Generate the constraint that ``array[index] == value`` (under *guard*).

        Mirrors ``ArrayHistorySolverRepresentation.select(...)``.

        Parameters
        ----------
        guard:
            Must hold for this select to be relevant.
        index:
            The index expression.
        value:
            The expected value expression.
        array_is_completely_initialized:
            When ``True`` the (guard, index, value) triple is NOT pushed into
            ``_selects`` (it is already fully covered).
        allow_aliasing:
            When ``False`` we add inequalities to ensure lazily-initialized
            reference-typed values are distinct.
        value_type:
            Element type; used to determine whether to enforce distinct values.
        enforce_default_for_unknowns:
            When ``True`` an unseen index implies the default value.
        """
        if guard is FALSE:
            return TRUE

        enforce_distinct = (
            not array_is_completely_initialized
            and (value_type in (list, object) or issubclass(value_type, object))
            and not allow_aliasing
        )

        select_constraint = self._select_inner(index, value, enforce_distinct)

        if enforce_default_for_unknowns:
            # Gather all guarded index-equals constraints from history
            index_seen = self._collect_index_seen_constraints(index)
            default_eq = _elements_equal(value, self._default_value)
            # ¬(any prior access with same index) → value == default
            select_constraint = And.new_instance(
                select_constraint,
                Implication.new_instance(
                    Not(index_seen),
                    default_eq,
                ),
            )

        if not array_is_completely_initialized:
            self._selects.append(ArrayAccessRecord(guard=guard, index=index, value=value))

        return Implication.new_instance(guard, select_constraint)

    def _select_inner(
        self,
        index: Any,
        value: Any,
        enforce_distinct: bool,
    ) -> Constraint:
        """Recursive inner select matching Java ``_select(...)``."""
        if self._store is not None:
            assert self._before_store is not None
            store_index_valid = self._store.index_is_valid(index)
            store_value_eq = _elements_equal(self._store.value, value)
            before_result = self._before_store._select_inner(index, value, enforce_distinct)
        else:
            store_index_valid = FALSE
            store_value_eq = TRUE
            before_result = TRUE

        for rec in self._selects:
            idx_eq = rec.index_is_valid(index)
            val_eq = _elements_equal(rec.value, value)
            if idx_eq is FALSE:
                if enforce_distinct:
                    before_result = And.new_instance(before_result, Not(val_eq))
                continue
            if idx_eq is TRUE:
                before_result = val_eq
                break

            if enforce_distinct:
                implication = Equivalence.new_instance(idx_eq, val_eq)
            else:
                implication = Implication.new_instance(idx_eq, val_eq)
            before_result = And.new_instance(implication, before_result)

        return BoolIte.new_instance(store_index_valid, store_value_eq, before_result)

    def _collect_index_seen_constraints(self, index: Any) -> Constraint:
        """Return the disjunction of all guarded (guard ∧ index==i) constraints."""
        parts: List[Constraint] = []
        node: Optional["ArrayHistorySolverRepresentation"] = self
        while node is not None:
            if node._store is not None:
                parts.append(node._store.index_is_valid(index))
            for rec in node._selects:
                parts.append(rec.index_is_valid(index))
            node = node._before_store

        if not parts:
            return FALSE
        result = parts[0]
        for p in parts[1:]:
            result = Or.new_instance(result, p)
        return result

    def get_values_known_to_possibly_be_in_array(
        self, array_has_fixed_length: bool
    ) -> Set[Any]:
        """Return an over-approximation of element values (for aliasing)."""
        result: Set[Any] = set()
        node: Optional["ArrayHistorySolverRepresentation"] = self
        while node is not None:
            if node._store is not None:
                result.add(node._store.value)
            if array_has_fixed_length:
                # Only concrete initial selects and stores
                for rec in node._selects:
                    if node._store is None:  # root node
                        result.add(rec.value)
            else:
                for rec in node._selects:
                    result.add(rec.value)
            node = node._before_store
        return result


# -----------------------------------------------------------------------
# Helpers
# -----------------------------------------------------------------------

def _elements_equal(a: Any, b: Any) -> Constraint:
    """Build an ``Eq`` or ``Equivalence`` constraint for a == b."""
    from mulib_python.substitutions import ConcBool, SymBool
    from mulib_python.constraints import Equivalence, Eq

    a_bool = isinstance(a, (ConcBool, SymBool))
    b_bool = isinstance(b, (ConcBool, SymBool))
    if a_bool and b_bool:
        # Both booleans: use Equivalence
        return Equivalence.new_instance(a, b)  # type: ignore[arg-type]
    return Eq(a, b)


# -----------------------------------------------------------------------
# PrimitiveValuedArraySolverRepresentation
# -----------------------------------------------------------------------

class PrimitiveValuedArraySolverRepresentation:
    """High-level array theory for arrays whose elements are primitive values.

    Mirrors Java ``PrimitiveValuedArraySolverRepresentation``.

    Parameters
    ----------
    array_id : Any
        The symbolic Sint identifier of the array.
    length : Any
        Symbolic or concrete length expression.
    is_null : Any
        Symbolic or concrete boolean indicating nullability.
    value_type : type
        Element type (``int``, ``float``, ``bool``).
    default_is_symbolic : bool
        When ``True`` the default value for unseen indices is symbolic (free).
    is_completely_initialized : bool
        When ``True`` every index is already constrained.
    can_contain_non_symbolic_default : bool
        When ``True`` unseen indices may hold the concrete default value.
    level : int
        Depth at which this representation was created.
    initial_selects : list[ArrayAccessRecord]
        Pre-populated (guard, index, value) triples from initialization.
    """

    def __init__(
        self,
        array_id: Any,
        length: Any,
        is_null: Any,
        value_type: type,
        default_is_symbolic: bool,
        is_completely_initialized: bool,
        can_contain_non_symbolic_default: bool,
        level: int,
        initial_selects: Optional[List[ArrayAccessRecord]] = None,
        *,
        allow_aliasing: bool = False,
    ) -> None:
        self.array_id = array_id
        self.length = length
        self.is_null = is_null
        self.value_type = value_type
        self.default_is_symbolic = default_is_symbolic
        self.is_completely_initialized = is_completely_initialized
        self.can_contain_non_symbolic_default = can_contain_non_symbolic_default
        self.level = level
        self._allow_aliasing = allow_aliasing
        self._current_repr = ArrayHistorySolverRepresentation(
            initial_selects or [], value_type
        )

    def select(self, guard: Constraint, index: Any, selected_value: Any) -> Constraint:
        """Generate the select constraint.

        Mirrors ``PrimitiveValuedArraySolverRepresentation._select``.
        """
        return self._current_repr.select(
            guard,
            index,
            selected_value,
            array_is_completely_initialized=self.is_completely_initialized,
            allow_aliasing=self._allow_aliasing,
            value_type=self.value_type,
            enforce_default_for_unknowns=(
                not self.default_is_symbolic
                and self.can_contain_non_symbolic_default
            ),
        )

    def store(self, guard: Constraint, index: Any, stored_value: Any) -> None:
        """Record a store; the history chain is updated in-place."""
        self._current_repr = self._current_repr.store(guard, index, stored_value)

    def copy_for_new_level(self, level: int) -> "PrimitiveValuedArraySolverRepresentation":
        new = PrimitiveValuedArraySolverRepresentation.__new__(
            PrimitiveValuedArraySolverRepresentation
        )
        new.array_id = self.array_id
        new.length = self.length
        new.is_null = self.is_null
        new.value_type = self.value_type
        new.default_is_symbolic = self.default_is_symbolic
        new.is_completely_initialized = self.is_completely_initialized
        new.can_contain_non_symbolic_default = self.can_contain_non_symbolic_default
        new.level = level
        new._allow_aliasing = self._allow_aliasing
        new._current_repr = self._current_repr.copy()
        return new
```

---

## 7. `z3_adapter.py` — Expression/Constraint → Z3 Translation

This is the most critical file; it mirrors `Z3MulibAdapter` completely.

### 7.1 Module structure

```python
# mulib_python/z3_adapter.py
"""Translates mulib_python expression/constraint AST nodes into z3 ExprRef objects.

This module depends on the ``z3-solver`` PyPI package (``import z3``).
Each expression is memoised by Python ``id`` (identity, not value) to
avoid re-creating Z3 constants for the same Python object.  This mirrors
Java's ``numericExpressionsStore`` and ``boolExprStore`` maps.
"""
from __future__ import annotations

from fractions import Fraction
from typing import Any, Dict, Optional, Union

import z3

from mulib_python.expressions import (
    Expression, AbstractOperatorExpression,
    Sum, Sub, Mul, Div, Mod,
    BitwiseAnd, BitwiseOr, BitwiseXor,
    ShiftLeft, ShiftRight, LogicalShiftRight,
    Neg, ExpressionIte,
)
from mulib_python.constraints import (
    Constraint, _BoolLiteral, TRUE, FALSE,
    AbstractTwoSidedConstraint, AbstractTwoSidedMathematicalConstraint,
    And, Or, Xor, Implication, Equivalence,
    Not, BoolIte, In,
    Lt, Lte, Eq,
)
from mulib_python.substitutions import (
    ConcInt, ConcFloat, ConcBool, SymInt, SymFloat, SymBool, Primitive,
)
```

### 7.2 Cache design

```python
class Z3MulibAdapter:
    """Stateful translator from mulib AST nodes to z3 expressions.

    All created Z3 expressions are memoised using Python object identity
    (``id(node)``), mirroring Java's ``IdentityHashMap``-based stores.

    Parameters
    ----------
    treat_bools_as_ints : bool
        When ``True`` symbolic booleans are represented as Z3 integers
        restricted to {0, 1}, enabling them to appear in arithmetic
        contexts.  Mirrors Java ``MulibConfig.VALS_TREAT_BOOLEANS_AS_INTS``.
    """

    def __init__(self, *, treat_bools_as_ints: bool = False) -> None:
        self.treat_bools_as_ints = treat_bools_as_ints
        # id(Python object) → z3.ExprRef
        self._num_cache: Dict[int, z3.ExprRef] = {}
        self._bool_cache: Dict[int, z3.BoolRef] = {}
```

### 7.3 Complete expression translation table

```python
    def transform_numeric_expr(self, n: Union[Expression, Primitive]) -> z3.ArithRef:
        """Translate a numeric expression node into a Z3 arithmetic expression.

        Mirrors ``Z3MulibAdapter.transformNumericExpr``.
        """
        cached = self._num_cache.get(id(n))
        if cached is not None:
            return cached  # type: ignore[return-value]

        if isinstance(n, AbstractOperatorExpression):
            elhs = self.transform_numeric_expr(n.lhs)
            erhs = self.transform_numeric_expr(n.rhs)
            result = self._translate_binary_numeric(n, elhs, erhs)

        elif isinstance(n, Neg):
            result = z3.ArithRef.__neg__(self.transform_numeric_expr(n.expr))
            # NOTE: z3.ArithRef does not expose __neg__ in all builds.
            # Use:  result = 0 - self.transform_numeric_expr(n.expr)

        elif isinstance(n, ExpressionIte):
            result = z3.If(
                self.transform_constraint(n.condition),
                self.transform_numeric_expr(n.if_expr),
                self.transform_numeric_expr(n.else_expr),
            )

        elif isinstance(n, (ConcInt, SymInt, ConcFloat, SymFloat)):
            result = self._translate_leaf(n)
        else:
            raise NotImplementedError(f"Cannot translate numeric expression: {n!r}")

        self._num_cache[id(n)] = result
        return result  # type: ignore[return-value]
```

#### Binary numeric operators

| mulib class | Z3 call | Notes |
|---|---|---|
| `Sum` | `z3.ArithRef.__add__(elhs, erhs)` | real-safe |
| `Sub` | `z3.ArithRef.__sub__(elhs, erhs)` | real-safe |
| `Mul` | `z3.ArithRef.__mul__(elhs, erhs)` | real-safe |
| `Div` | `z3.ArithRef.__div__(elhs, erhs)` or `elhs / erhs` | integer vs real handled by Z3 automatically |
| `Mod` | `z3.ArithRef.__mod__(elhs, erhs)` | integer only; raise if fp |
| `BitwiseAnd` | `z3.BV2Int(z3.BVAND(int2bv(elhs, w), int2bv(erhs, w)), True)` | |
| `BitwiseOr` | `z3.BV2Int(z3.BVOR(…), True)` | |
| `BitwiseXor` | `z3.BV2Int(z3.BVXOR(…), True)` | |
| `ShiftLeft` | `z3.BV2Int(z3.BVSHL(…), True)` | |
| `ShiftRight` | `z3.BV2Int(z3.BVASHR(…), True)` | arithmetic |
| `LogicalShiftRight` | `z3.BV2Int(z3.BVLSHR(…), True)` | logical |

For bitwise operations the helper:

```python
    def _int2bv(self, expr: z3.ExprRef, width: int) -> z3.BitVecRef:
        return z3.Int2BV(expr, width)  # type: ignore[arg-type]
```

Width is 64 for `SymInt(is_long=True)` and 32 otherwise. Both operands must have
the same width (validated/asserted).

Full implementation of `_translate_binary_numeric`:

```python
    def _translate_binary_numeric(
        self,
        n: AbstractOperatorExpression,
        elhs: z3.ExprRef,
        erhs: z3.ExprRef,
    ) -> z3.ExprRef:
        if isinstance(n, Sum):
            return elhs + erhs
        if isinstance(n, Sub):
            return elhs - erhs
        if isinstance(n, Mul):
            return elhs * erhs
        if isinstance(n, Div):
            return elhs / erhs
        if isinstance(n, Mod):
            return elhs % erhs

        # Bitwise / shift — must convert through BitVec
        width = 64 if getattr(n.lhs, "is_long", False) else 32
        bvlhs = self._int2bv(elhs, width)
        bvrhs = self._int2bv(erhs, width)
        if isinstance(n, BitwiseAnd):
            bv_result = bvlhs & bvrhs
        elif isinstance(n, BitwiseOr):
            bv_result = bvlhs | bvrhs
        elif isinstance(n, BitwiseXor):
            bv_result = bvlhs ^ bvrhs
        elif isinstance(n, ShiftLeft):
            bv_result = z3.BVSHL(bvlhs, bvrhs)
        elif isinstance(n, ShiftRight):
            bv_result = z3.BVASHR(bvlhs, bvrhs)  # arithmetic
        elif isinstance(n, LogicalShiftRight):
            bv_result = z3.BVLSHR(bvlhs, bvrhs)  # logical
        else:
            raise NotImplementedError(f"Unknown bitwise op: {n!r}")
        return z3.BV2Int(bv_result, is_signed=True)
```

### 7.4 Leaf translation

```python
    def _translate_leaf(self, n: Primitive) -> z3.ExprRef:
        """Translate a concrete or symbolic leaf into a Z3 constant or numeral."""
        if isinstance(n, ConcInt):
            return z3.IntVal(n.value)
        if isinstance(n, ConcFloat):
            # Use exact rational representation to avoid floating-point noise
            frac = Fraction(n.value).limit_denominator(10**9)
            return z3.RealVal(f"{frac.numerator}/{frac.denominator}")
        if isinstance(n, ConcBool):
            return z3.BoolVal(n.value)

        if isinstance(n, SymInt):
            return z3.Int(n.id)

        if isinstance(n, SymFloat):
            return z3.Real(n.id)

        if isinstance(n, SymBool):
            if self.treat_bools_as_ints:
                return z3.Int(f"{n.id}_int")
            return z3.Bool(n.id)

        raise NotImplementedError(f"Unknown leaf type: {n!r}")
```

### 7.5 Constraint translation table

```python
    def transform_constraint(self, c: Constraint) -> z3.BoolRef:
        """Translate a constraint node into a Z3 boolean expression.

        Mirrors ``Z3MulibAdapter.transformConstraint``.
        """
        cached = self._bool_cache.get(id(c))
        if cached is not None:
            return cached

        result = self._translate_constraint_inner(c)
        self._bool_cache[id(c)] = result
        return result
```

Full translation table:

| mulib class | Z3 call |
|---|---|
| `_BoolLiteral(True)` | `z3.BoolVal(True)` |
| `_BoolLiteral(False)` | `z3.BoolVal(False)` |
| `SymBool` leaf | `z3.Bool(sym.id)` |
| `ConcBool` leaf | `z3.BoolVal(b.value)` |
| `Not(c)` | `z3.Not(transform_constraint(c.constraint))` |
| `And(l, r)` | `z3.And(transform_constraint(l), transform_constraint(r))` |
| `Or(l, r)` | `z3.Or(transform_constraint(l), transform_constraint(r))` |
| `Xor(l, r)` | `z3.Xor(transform_constraint(l), transform_constraint(r))` |
| `Implication(l, r)` | `z3.Implies(transform_constraint(l), transform_constraint(r))` |
| `Equivalence(l, r)` | `transform_constraint(l) == transform_constraint(r)` (Z3 overloads `==` for booleans to produce `Iff`) |
| `BoolIte(c, t, f)` | `z3.If(transform_constraint(c), transform_constraint(t), transform_constraint(f))` |
| `Eq(l, r)` | `transform_numeric_expr(l) == transform_numeric_expr(r)` |
| `Lt(l, r)` | `transform_numeric_expr(l) < transform_numeric_expr(r)` |
| `Lte(l, r)` | `transform_numeric_expr(l) <= transform_numeric_expr(r)` |
| `In(el, {s0, s1, ...})` | `z3.Or([transform(el)==transform(si) for si in set])` |

```python
    def _translate_constraint_inner(self, c: Constraint) -> z3.BoolRef:
        if isinstance(c, _BoolLiteral):
            return z3.BoolVal(c.value)

        if isinstance(c, SymBool):
            return z3.Bool(c.id)

        if isinstance(c, ConcBool):
            return z3.BoolVal(c.value)

        if isinstance(c, Not):
            return z3.Not(self.transform_constraint(c.constraint))

        if isinstance(c, BoolIte):
            return z3.If(
                self.transform_constraint(c.condition),
                self.transform_constraint(c.if_case),
                self.transform_constraint(c.else_case),
            )

        if isinstance(c, And):
            return z3.And(
                self.transform_constraint(c.lhs),
                self.transform_constraint(c.rhs),
            )
        if isinstance(c, Or):
            return z3.Or(
                self.transform_constraint(c.lhs),
                self.transform_constraint(c.rhs),
            )
        if isinstance(c, Xor):
            return z3.Xor(
                self.transform_constraint(c.lhs),
                self.transform_constraint(c.rhs),
            )
        if isinstance(c, Implication):
            return z3.Implies(
                self.transform_constraint(c.lhs),
                self.transform_constraint(c.rhs),
            )
        if isinstance(c, Equivalence):
            lz = self.transform_constraint(c.lhs)
            rz = self.transform_constraint(c.rhs)
            return lz == rz  # Z3 overloads == for BoolRef → BoolRef

        if isinstance(c, Eq):
            return self.transform_numeric_expr(c.lhs) == self.transform_numeric_expr(c.rhs)
        if isinstance(c, Lt):
            return self.transform_numeric_expr(c.lhs) < self.transform_numeric_expr(c.rhs)
        if isinstance(c, Lte):
            return self.transform_numeric_expr(c.lhs) <= self.transform_numeric_expr(c.rhs)

        if isinstance(c, In):
            el_z = self.transform_numeric_expr(c.element)
            return z3.Or([el_z == self.transform_numeric_expr(s) for s in c.set])

        raise NotImplementedError(f"Cannot translate constraint: {c!r}")
```

### 7.6 Array theory (Z3 native, non-high-level path)

When `high_level_symbolic_object_approach=False` the adapter uses Z3's native
`z3.Array` theory.

```python
    def new_array_expr_from_type(self, array_id: Any, value_type: type) -> z3.ArrayRef:
        """Create a named Z3 array constant.

        Mirrors ``Z3MulibAdapter.newArrayExprFromType``.

        Parameters
        ----------
        array_id : Any
            Symbolic Sint id; used to build the Z3 array name.
        value_type : type
            Element type: ``int`` → IntSort, ``bool`` → BoolSort, ``float`` → RealSort.
        """
        if value_type is bool:
            elem_sort = z3.BoolSort()
        elif value_type is float:
            elem_sort = z3.RealSort()
        elif value_type is int:
            elem_sort = z3.IntSort()
        else:
            raise NotImplementedError(f"Unsupported element type: {value_type}")

        name = f"Sarray{array_id}"
        return z3.Array(name, z3.IntSort(), elem_sort)

    def new_array_expr_from_store(
        self,
        old_array: z3.ArrayRef,
        index: Any,
        value: Any,
    ) -> z3.ArrayRef:
        """Return ``z3.Store(old_array, translate(index), translate(value))``.

        Mirrors ``Z3MulibAdapter.newArrayExprFromStore``.
        """
        i_z = self.transform_numeric_expr(index)
        v_z = self._translate_substituted(value)
        return z3.Store(old_array, i_z, v_z)

    def transform_select_constraint(
        self,
        array_expr: z3.ArrayRef,
        index: Any,
        value: Any,
    ) -> z3.BoolRef:
        """Return ``z3.Select(array, index) == translate(value)``.

        Mirrors ``Z3MulibAdapter.transformSelectConstraint``.
        """
        select = z3.Select(array_expr, self.transform_numeric_expr(index))
        return select == self._translate_substituted(value)

    def _translate_substituted(self, sv: Any) -> z3.ExprRef:
        """Route to bool or numeric translation based on runtime type."""
        if isinstance(sv, (ConcBool, SymBool, _BoolLiteral)):
            return self.transform_constraint(sv)  # type: ignore[arg-type]
        return self.transform_numeric_expr(sv)  # type: ignore[arg-type]
```

### 7.7 Model labeling helpers

```python
    def label_from_model(
        self,
        var: Primitive,
        model: z3.ModelRef,
    ) -> Union[int, float, bool]:
        """Evaluate *var* in *model* and return a Python primitive.

        Mirrors ``AbstractZ3SolverManager.toPrimitiveOrString``.

        The returned type depends on the variable's declared type:
        - ``SymInt(is_long=False)`` → ``int``
        - ``SymInt(is_long=True)`` → ``int``  (Python has arbitrary ints)
        - ``SymFloat(is_double=True)`` → ``float``
        - ``SymFloat(is_double=False)`` → ``float``
        - ``SymBool`` → ``bool``
        - ``ConcInt`` → ``int``  (short-circuit, no model lookup)
        - ``ConcFloat`` → ``float``
        - ``ConcBool`` → ``bool``
        """
        if isinstance(var, ConcInt):
            return var.value
        if isinstance(var, ConcFloat):
            return var.value
        if isinstance(var, ConcBool):
            return var.value

        if isinstance(var, SymInt):
            z3_expr = self.transform_numeric_expr(var)
        elif isinstance(var, SymFloat):
            z3_expr = self.transform_numeric_expr(var)
        elif isinstance(var, SymBool):
            if self.treat_bools_as_ints:
                # The int-encoded bool lives in num_cache
                z3_expr = z3.Int(f"{var.id}_int")
            else:
                z3_expr = z3.Bool(var.id)
        else:
            raise NotImplementedError(f"Cannot label: {var!r}")

        evaluated = model.eval(z3_expr, model_completion=True)
        return _z3_value_to_python(evaluated, var)
```

```python
def _z3_value_to_python(
    z3_val: z3.ExprRef,
    hint: Primitive,
) -> Union[int, float, bool]:
    """Convert a Z3 model value to a Python primitive.

    Mirrors ``AbstractZ3SolverManager.toPrimitiveOrString``.

    Parameters
    ----------
    z3_val : z3.ExprRef
        The result of ``model.eval(expr, model_completion=True)``.
    hint : Primitive
        The original variable; used to decide the Python return type.
    """
    if z3.is_int_value(z3_val):
        py_int = z3_val.as_long()
        # Sub-type coercion: for short/byte we rely on the caller knowing the
        # declared Java type.  In Phase 3 we only need int/long.
        return py_int

    if z3.is_rational_value(z3_val):
        # RatNum: numerator / denominator
        num = z3_val.numerator_as_long()
        den = z3_val.denominator_as_long()
        if den == 0:
            return 0.0
        result = num / den
        if isinstance(hint, SymFloat) and not hint.is_double:
            return float(result)  # truncate to 32-bit range if needed
        return float(result)

    if z3.is_true(z3_val):
        return True
    if z3.is_false(z3_val):
        return False

    # Fallback: try numeric conversion
    try:
        return int(str(z3_val))
    except ValueError:
        pass

    raise ValueError(f"Cannot convert Z3 value {z3_val!r} (hint={hint!r}) to Python")
```

---

## 8. `z3_solver_manager.py` — Solver Manager Implementations

### 8.1 `AbstractZ3SolverManager`

```python
# mulib_python/z3_solver_manager.py
from __future__ import annotations

import threading
from collections import deque
from typing import Any, Deque, Dict, List, Optional, Union

import z3

from mulib_python.constraints import (
    Constraint, _BoolLiteral, TRUE, FALSE,
    ArrayAccessConstraint, ArrayInitializationConstraint,
    PartnerClassObjectConstraint,
)
from mulib_python.expressions import Expression
from mulib_python.substitutions import ConcBool, Primitive
from mulib_python.solution import Labels, Solution
from mulib_python.solver_manager import SolverManager
from mulib_python.incremental_state import IncrementalSolverState
from mulib_python.array_repr import PrimitiveValuedArraySolverRepresentation, ArrayAccessRecord
from mulib_python.z3_adapter import Z3MulibAdapter
from mulib_python.exceptions import MulibRuntimeException, UnknownSolutionException


# One global lock for Z3 context creation (mirrors Java's syncObject)
_Z3_INIT_LOCK = threading.Lock()


class AbstractZ3SolverManager(SolverManager):
    """Base class for Z3-backed solver managers.

    Handles:
    - Z3 solver + context initialisation (thread-safe)
    - Expression/constraint → Z3 translation via :class:`~mulib_python.z3_adapter.Z3MulibAdapter`
    - Incremental state bookkeeping via :class:`~mulib_python.incremental_state.IncrementalSolverState`
    - Satisfiability caching
    - Model labeling
    - High-level array theory integration

    Parameters
    ----------
    treat_bools_as_ints : bool
        Passed through to :class:`~mulib_python.z3_adapter.Z3MulibAdapter`.
    high_level_symbolic_object_approach : bool
        When ``True`` uses the Python-level store-chain array theory
        (``array_repr.py``); when ``False`` uses Z3's native ``z3.Array``
        theory directly.
    """

    def __init__(
        self,
        *,
        treat_bools_as_ints: bool = False,
        high_level_symbolic_object_approach: bool = True,
    ) -> None:
        with _Z3_INIT_LOCK:
            self._solver = z3.Solver()
            self._adapter = Z3MulibAdapter(treat_bools_as_ints=treat_bools_as_ints)

        self._high_level = high_level_symbolic_object_approach
        self._state = IncrementalSolverState(self)

        # Satisfiability cache
        self._is_satisfiable: bool = False
        self._sat_was_calculated: bool = False
        self._current_model: Optional[z3.ModelRef] = None

        # Label cache (identity-keyed)
        self._repr_to_label: Dict[int, Any] = {}  # id(search_repr) → label
```

### 8.2 Satisfiability caching

```python
    def _reset_sat_and_model(self) -> None:
        self._sat_was_calculated = False
        self._current_model = None

    def is_satisfiable(self) -> bool:
        assert self._state.level != 0, "At least the initial choice scope must be present"
        if not self._sat_was_calculated:
            self._is_satisfiable = self._calculate_is_satisfiable()
            self._sat_was_calculated = True
        return self._is_satisfiable

    def _get_current_model(self) -> z3.ModelRef:
        if self._current_model is None:
            self._current_model = self._solver.model()
        return self._current_model
```

### 8.3 Constraint insertion

```python
    def add_constraint(self, constraint: Constraint) -> None:
        if isinstance(constraint, _BoolLiteral) and constraint.value is True:
            return  # TRUE is always satisfied; skip
        self._reset_sat_and_model()
        z3_bool = self._adapter.transform_constraint(constraint)
        self._add_solver_constraint(z3_bool)

    def add_constraint_after_new_backtracking_point(self, constraint: Constraint) -> None:
        self._reset_sat_and_model()
        self._state.push_constraint(constraint)
        self._solver_specific_backtracking_point()
        z3_bool = self._adapter.transform_constraint(constraint)
        self._add_solver_constraint(z3_bool)
```

### 8.4 Array constraint handling

```python
    def add_array_constraint(
        self,
        ac: Union[ArrayAccessConstraint, ArrayInitializationConstraint],
    ) -> None:
        if isinstance(ac, ArrayInitializationConstraint):
            self._handle_array_initialization(ac)
        elif isinstance(ac, ArrayAccessConstraint):
            self._handle_array_access(ac)
        else:
            raise NotImplementedError(f"Unknown array constraint type: {type(ac)!r}")
        self._state.add_array_constraint(ac)

    def _handle_array_initialization(self, aic: ArrayInitializationConstraint) -> None:
        if self._high_level:
            # Build a PrimitiveValuedArraySolverRepresentation
            repr_ = PrimitiveValuedArraySolverRepresentation(
                array_id=aic.partner_class_object_id,
                length=aic.length,
                is_null=FALSE,  # TODO: expose nullability in constraint
                value_type=aic.value_type,
                default_is_symbolic=False,
                is_completely_initialized=False,
                can_contain_non_symbolic_default=True,
                level=self._state.level,
            )
            self._state.initialize_array_representation(aic, repr_)
        else:
            # Native Z3 Array theory path
            z3_array = self._adapter.new_array_expr_from_type(
                aic.partner_class_object_id, aic.value_type
            )
            self._state.initialize_array_representation(aic, z3_array)

    def _handle_array_access(self, ac: ArrayAccessConstraint) -> None:
        if self._high_level:
            repr_ = self._state.get_current_array_representation(ac.partner_class_object_id)
            assert repr_ is not None, "Array must be initialized before access"
            if repr_.level != self._state.level:
                repr_ = repr_.copy_for_new_level(self._state.level)
                self._state.add_new_representation_for_array(ac, repr_)

            if ac.type == ArrayAccessConstraint.Type.SELECT:
                select_constraint = repr_.select(TRUE, ac.index, ac.value)
                self.add_constraint(select_constraint)
            else:
                repr_.store(TRUE, ac.index, ac.value)
        else:
            # Native Z3 Array theory path
            z3_array = self._state.get_current_array_representation(ac.partner_class_object_id)
            assert z3_array is not None
            if ac.type == ArrayAccessConstraint.Type.SELECT:
                bool_expr = self._adapter.transform_select_constraint(
                    z3_array, ac.index, ac.value
                )
                self._add_solver_constraint(bool_expr)
                self._reset_sat_and_model()
            else:
                new_array = self._adapter.new_array_expr_from_store(
                    z3_array, ac.index, ac.value
                )
                self._state.add_new_representation_for_array(ac, new_array)
```

### 8.5 Backtracking

```python
    def backtrack_once(self) -> None:
        self._solver_specific_backtrack_once()
        self._state.pop_constraint()
        self._reset_sat_and_model()

    def backtrack(self, n: int) -> None:
        self._solver_specific_backtrack(n)
        for _ in range(n):
            self._state.pop_constraint()
        if n > 0:
            self._reset_sat_and_model()

    def backtrack_all(self) -> None:
        self.backtrack(self._state.level)

    def get_level(self) -> int:
        return self._state.level
```

### 8.6 Labeling

```python
    def reset_labels(self) -> None:
        self._repr_to_label.clear()

    def register_label_pair(self, search_repr: Any, label: Any) -> None:
        self._repr_to_label[id(search_repr)] = label

    def get_label(self, var: Any) -> Any:
        if not self.is_satisfiable():
            raise MulibRuntimeException("Cannot label: constraint system is unsatisfiable")
        return self._label_var(var)

    def _label_var(self, var: Any) -> Any:
        if var is None:
            return None
        cached = self._repr_to_label.get(id(var))
        if cached is not None:
            return cached
        if isinstance(var, (ConcBool, *_PRIMITIVE_TYPES)):
            # ConcInt, ConcFloat, ConcBool → just unwrap
            return var.value
        # SymInt, SymFloat, SymBool
        return self._adapter.label_from_model(var, self._get_current_model())

    def label_solution(
        self,
        return_value: Any,
        remembered: Dict[str, Any],
    ) -> Solution:
        """Build a :class:`~mulib_python.solution.Solution`.

        Mirrors ``AbstractIncrementalEnabledSolverManager.labelSolution``.

        Parameters
        ----------
        return_value:
            Symbolic or concrete return value.
        remembered:
            ``{name: symbolic_var}`` for variables the user remembered.
        """
        id_to_var: Dict[str, Any] = {}
        id_to_label: Dict[str, Any] = {}

        for name, sym_var in remembered.items():
            if name in id_to_var:
                raise MulibRuntimeException(f"Duplicate remembered name: {name!r}")
            id_to_var[name] = sym_var
            id_to_label[name] = self._label_var(sym_var)

        labeled_return = self._label_var(return_value)
        id_to_label["return"] = labeled_return
        if return_value is not None:
            id_to_var["return"] = return_value

        labels = Labels(id_to_var, id_to_label)
        return Solution(labeled_return, labels)

_PRIMITIVE_TYPES = (
    type(None),  # placeholder
)

# Refined in z3_adapter to import actual Primitive subtypes at call time.
```

### 8.7 `get_up_to_n_solutions`

```python
    def get_up_to_n_solutions(
        self,
        initial_solution: Solution,
        n: int,
    ) -> List[Solution]:
        """Generate up to *n* additional distinct solutions.

        Mirrors ``AbstractIncrementalEnabledSolverManager.getUpToNSolutions``.

        Each iteration adds a disjunction of inequalities forbidding the
        previously found solution, then checks satisfiability and labels again.
        """
        from mulib_python.constraints import Or, Not, Eq

        latest_solution = initial_solution
        solutions: List[Solution] = []

        # Collect the symbolic return variable and remembered primitives
        unlabeled_return = initial_solution.labels.get_named_var("return")

        while n > 0:
            # Build negation: at least one variable must differ
            neq_parts: List[Constraint] = []
            for name, sym_var in latest_solution.labels.id_to_named_var.items():
                label = latest_solution.labels.get_label_for_id(name)
                neq = self._build_neq(sym_var, label)
                if neq is not None:
                    neq_parts.append(neq)

            if not neq_parts:
                break  # nothing to negate

            disjunction: Constraint = neq_parts[0]
            for part in neq_parts[1:]:
                disjunction = Or.new_instance(disjunction, part)

            self.add_constraint(disjunction)
            if self.is_satisfiable():
                self.reset_labels()
                new_solution = self.label_solution(
                    unlabeled_return,
                    {
                        k: v
                        for k, v in latest_solution.labels.id_to_named_var.items()
                        if k != "return"
                    },
                )
                solutions.append(new_solution)
                latest_solution = new_solution
                n -= 1
            else:
                break

        return solutions

    def _build_neq(self, sym_var: Any, label: Any) -> Optional[Constraint]:
        """Build ``Not(Eq(sym_var, label_as_conc))`` or ``None`` for concretized vars."""
        from mulib_python.substitutions import ConcInt, ConcFloat, ConcBool
        from mulib_python.constraints import Eq, Not as CnNot
        if isinstance(sym_var, (ConcInt, ConcFloat, ConcBool)):
            return None  # concrete vars cannot be further constrained
        if sym_var is None:
            return None
        if isinstance(label, bool):
            return CnNot(Eq(sym_var, ConcBool(label)))
        if isinstance(label, float):
            return CnNot(Eq(sym_var, ConcFloat(label)))
        if isinstance(label, int):
            return CnNot(Eq(sym_var, ConcInt(label)))
        return None
```

### 8.8 Abstract solver-specific hooks

```python
    # ---- abstract methods (implemented by subclasses) ----------------------

    @abc.abstractmethod
    def _calculate_is_satisfiable(self) -> bool: ...

    @abc.abstractmethod
    def _calculate_satisfiability_with_assumption(self, z3_bool: z3.BoolRef) -> bool: ...

    @abc.abstractmethod
    def _solver_specific_backtracking_point(self) -> None: ...

    @abc.abstractmethod
    def _solver_specific_backtrack_once(self) -> None: ...

    @abc.abstractmethod
    def _solver_specific_backtrack(self, n: int) -> None: ...

    @abc.abstractmethod
    def _add_solver_constraint(self, z3_bool: z3.BoolRef) -> None: ...

    def check_with_new_constraint(self, constraint: Constraint) -> bool:
        if isinstance(constraint, _BoolLiteral):
            return constraint.value
        z3_bool = self._adapter.transform_constraint(constraint)
        result = self._calculate_satisfiability_with_assumption(z3_bool)
        self._reset_sat_and_model()
        return result

    def shutdown(self) -> None:
        self._adapter._num_cache.clear()
        self._adapter._bool_cache.clear()
        self._state.clear()
        self._repr_to_label.clear()
```

---

## 9. `Z3IncrementalSolverManager`

```python
class Z3IncrementalSolverManager(AbstractZ3SolverManager):
    """Incremental Z3 solver using native push/pop scopes.

    Mirrors ``Z3IncrementalSolverManager``.

    Best for depth-first search strategies.  Backtracking is O(1) since Z3
    manages the scope stack internally.

    Parameters
    ----------
    treat_bools_as_ints : bool
        See :class:`AbstractZ3SolverManager`.
    high_level_symbolic_object_approach : bool
        See :class:`AbstractZ3SolverManager`.
    """

    def _calculate_is_satisfiable(self) -> bool:
        status = self._solver.check()
        if status == z3.unknown:
            raise UnknownSolutionException(
                f"Z3 cannot determine satisfiability: {self._solver.reason_unknown()}"
            )
        return status == z3.sat

    def _calculate_satisfiability_with_assumption(self, z3_bool: z3.BoolRef) -> bool:
        status = self._solver.check(z3_bool)
        if status == z3.unknown:
            raise UnknownSolutionException(
                f"Z3 cannot determine satisfiability: {self._solver.reason_unknown()}"
            )
        return status == z3.sat

    def _solver_specific_backtracking_point(self) -> None:
        self._solver.push()

    def _solver_specific_backtrack_once(self) -> None:
        self._solver.pop(1)

    def _solver_specific_backtrack(self, n: int) -> None:
        self._solver.pop(n)

    def _add_solver_constraint(self, z3_bool: z3.BoolRef) -> None:
        self._solver.add(z3_bool)
```

---

## 10. `Z3GlobalLearningSolverManager`

```python
class Z3GlobalLearningSolverManager(AbstractZ3SolverManager):
    """Non-incremental Z3 solver using assumption-literal implication guards.

    Mirrors ``Z3GlobalLearningSolverManager``.

    Instead of pushing/popping scopes, every constraint is added globally
    behind a fresh implication literal:  ``implier_k → constraint``.
    Backtracking simply removes the current-level implier from the assumption
    list.  This allows Z3 to retain learned lemmas across backtracks, which is
    beneficial for BFS / IDDFS strategies at the cost of higher memory use.

    Attributes
    ----------
    _impliers : deque[z3.BoolRef]
        Stack of per-level assumption literals.  The most-recent is at the left.
    _bool_to_implier : dict
        Caches the implier for an already-seen constraint (avoids re-registering
        the same implication).
    _implier_counter : int
        Monotonically increasing counter used for unique implier names.
    """

    def __init__(self, **kwargs: Any) -> None:
        super().__init__(**kwargs)
        self._impliers: Deque[z3.BoolRef] = deque()
        self._bool_to_implier: Dict[int, z3.BoolRef] = {}  # id(z3_bool) → implier
        self._implier_counter: int = 0

    def _add_solver_constraint(self, z3_bool: z3.BoolRef) -> None:
        implier = self._bool_to_implier.get(id(z3_bool))
        if implier is None:
            implier = z3.Bool(f"implier_{self._implier_counter}")
            self._implier_counter += 1
            self._solver.add(z3.Implies(implier, z3_bool))
            self._bool_to_implier[id(z3_bool)] = implier

        level = self.get_level()
        if len(self._impliers) == level:
            # Conjoin to the current level's implier
            existing = self._impliers.popleft()
            implier = z3.And(existing, implier)
        self._impliers.appendleft(implier)
        assert len(self._impliers) == level

    def _calculate_is_satisfiable(self) -> bool:
        assumptions = list(self._impliers)
        status = self._solver.check(*assumptions)
        if status == z3.unknown:
            raise UnknownSolutionException(
                f"Z3 cannot determine satisfiability: {self._solver.reason_unknown()}"
            )
        return status == z3.sat

    def _calculate_satisfiability_with_assumption(self, z3_bool: z3.BoolRef) -> bool:
        assumptions = list(self._impliers) + [z3_bool]
        status = self._solver.check(*assumptions)
        if status == z3.unknown:
            raise UnknownSolutionException(
                f"Z3 cannot determine satisfiability: {self._solver.reason_unknown()}"
            )
        return status == z3.sat

    def _solver_specific_backtracking_point(self) -> None:
        pass  # No-op: we stay on the global scope

    def _solver_specific_backtrack_once(self) -> None:
        if self._impliers:
            self._impliers.popleft()

    def _solver_specific_backtrack(self, n: int) -> None:
        for _ in range(n):
            if self._impliers:
                self._impliers.popleft()
```

---

## 11. Thread Safety

### 11.1 Z3 context isolation (most important)

The z3-solver Python package (backed by `libz3`) is **not thread-safe** for
shared `z3.Solver` objects.  The Java code uses a single global `syncObject` only
during *construction* of the context.  The key design decision for Python is:

**One `z3.Solver` (and adapter) per `SolverManager` instance.**

The lock `_Z3_INIT_LOCK` at module level only protects the Z3 context creation
path.  Once created, each solver instance is used exclusively by a single thread.
This mirrors Java's intent; `AbstractZ3SolverManager` is not designed for
multi-threaded access after construction.

### 11.2 Per-instance locking (optional)

If the mulib search engine spawns multiple search threads that share a single
solver (not the default Java design but possible), add a per-instance
`threading.RLock` around all public methods of `AbstractZ3SolverManager`:

```python
self._lock = threading.RLock()

def add_constraint(self, constraint: Constraint) -> None:
    with self._lock:
        ...
```

### 11.3 Cache keying

Python caches are keyed by `id(obj)` (memory address).  This is safe under
single-threaded use.  Under multi-threading, if the same Python object can be
garbage collected and a new object created at the same address, the cache could
return a stale entry.  Mitigate by holding strong references to all AST nodes in
the constraint system (the incremental state already does this).

### 11.4 Z3 global state

`z3.set_option` calls are global in the Z3 library; avoid them in library code or
use the `z3.Params` / per-solver parameter API instead.

---

## 12. `Solution` and `Labels` Equivalents (Summary)

| Java | Python | Location |
|---|---|---|
| `Labels` (interface) | `Labels` (class with same API) | `solution.py` |
| `StdLabels` | built into `Labels.__init__` | `solution.py` |
| `Solution` | `Solution` | `solution.py` |

Key mapping of method names:

| Java | Python |
|---|---|
| `getLabelForId(id)` | `get_label_for_id(name)` |
| `getNamedVar(id)` | `get_named_var(name)` |
| `getIdToNamedVar()` | property `id_to_named_var` |
| `getIdToLabel()` | property `id_to_label` |
| `getNames()` | property `names` |
| `Solution.returnValue` | `Solution.return_value` |
| `Solution.labels` | `Solution.labels` |

---

## 13. `__init__.py` Updates

```python
# mulib_python/__init__.py  (additions for Phase 3)

from mulib_python.substitutions import (
    ConcInt, ConcFloat, ConcBool,
    SymInt, SymFloat, SymBool,
)
from mulib_python.solution import Labels, Solution
from mulib_python.solver_manager import SolverManager
from mulib_python.z3_solver_manager import (
    Z3IncrementalSolverManager,
    Z3GlobalLearningSolverManager,
)

__all__ = [
    # Phase 1
    "expressions",
    "constraints",
    "exceptions",
    # Phase 3
    "ConcInt", "ConcFloat", "ConcBool",
    "SymInt", "SymFloat", "SymBool",
    "Labels", "Solution",
    "SolverManager",
    "Z3IncrementalSolverManager",
    "Z3GlobalLearningSolverManager",
]
```

---

## 14. Dependencies

Add to the project's dependency list (e.g., `requirements.txt` or `build.gradle`
Python section):

```
z3-solver>=4.12.0
```

This is the official Z3 Python binding on PyPI.  No other solver libraries are
needed for Phase 3.

---

## 15. Testing Guidance

The implementation agent should validate these scenarios:

1. **Basic satisfiability** — create a `Z3IncrementalSolverManager`, push a
   `Lt(SymInt("x"), ConcInt(5))` constraint, assert `is_satisfiable()` is `True`;
   label `SymInt("x")` and verify result < 5.

2. **Backtracking** — push level with `x < 5`, then a second level with `x < 3`.
   Backtrack once; verify that `x < 5` is still satisfiable and `x` can be 4.

3. **Global learning** — same test with `Z3GlobalLearningSolverManager`; verify
   that `get_level()` tracking matches.

4. **Bitwise ops** — verify `BitwiseAnd(SymInt("a"), ConcInt(0xFF))` produces the
   expected Z3 expression.

5. **Floating point** — `Eq(SymFloat("f"), ConcFloat(3.14))` should yield a
   model where `f ≈ 3.14`.

6. **In constraint** — `In(SymInt("n"), (ConcInt(1), ConcInt(2), ConcInt(3)))`
   should be satisfiable and label `n` in {1, 2, 3}.

7. **Array theory (high-level)** — initialize an array repr, store an element,
   select it, verify the generated `Implication`/`Eq` constraint is satisfiable.

8. **`get_up_to_n_solutions`** — verify that 3 calls return 3 distinct solutions.

9. **`label_solution`** — verify `Labels` contains the `"return"` key.

10. **Thread safety** — create two `Z3IncrementalSolverManager` instances in
    parallel threads and verify no cross-contamination of models.

---

## 16. Design Decisions (Rationale)

| Decision | Java | Python | Reason |
|---|---|---|---|
| Cache by `id()` | `IdentityHashMap` | `dict` keyed by `id(obj)` | Exact semantic match; avoids expensive structural equality checks on AST nodes |
| No shared Z3 context | `new Context()` per manager | `z3.Solver()` per instance | Thread safety and Z3's own recommendation |
| `Fraction` for float → Z3 Real | `ctx.mkReal(String.valueOf(v))` | `Fraction(v).limit_denominator(…)` | Avoids floating-point representation errors in the solver |
| `treat_bools_as_ints` flag | `MulibConfig.VALS_TREAT_BOOLEANS_AS_INTS` | constructor kwarg | Keeps config injectable without a heavy config object |
| High-level vs native array theory | `config.SOLVER_HIGH_LEVEL_SYMBOLIC_OBJECT_APPROACH` | `high_level_symbolic_object_approach` ctor kwarg | Preserves both code paths; the high-level path is the validated one |
| Python `int` for Sint/Slong | Java `BigInteger` | Python `int` is unbounded | Python's native integer is already arbitrary precision |
| `deque` for implier stack | `ArrayDeque<BoolExpr>` | `collections.deque` | O(1) appendleft/popleft, matches Java's addFirst/removeFirst |
