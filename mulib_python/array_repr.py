"""Array representations for the solver using array history (not z3.Array)."""

from __future__ import annotations

import abc
from dataclasses import dataclass, field
from typing import Any, Dict, List, Optional, TYPE_CHECKING

from mulib_python.substitutions.primitives.sint import ConcSint, ConcSbool
from mulib_python.substitutions.primitives.sdouble import ConcSdouble

if TYPE_CHECKING:
    from mulib_python.constraints import Constraint
    from mulib_python.z3_adapter import Z3MulibAdapter
    import z3


# ---------------------------------------------------------------------------
# Module-level constants
# ---------------------------------------------------------------------------

#: Sentinel value used by partner-class arrays to represent the ``null``
#: reference.  Mirrors Java's ``Sint.ConcSint.MINUS_ONE`` (see
#: ``SimplePartnerClassArraySolverRepresentation.java``).  It is a real
#: module-level constant — *not* a lazily-initialized class attribute — so
#: it is import-time deterministic and thread-safe.
NULL_REFERENCE: ConcSint = ConcSint(-1)

#: Default values used by :class:`PrimitiveValuedArraySolverRepresentation`
#: when the caller does not provide an explicit ``default_value``.  The
#: lookup is intentionally exhaustive: an unsupported element type must
#: raise rather than silently fall back to an integer zero.
_PRIMITIVE_DEFAULTS: Dict[type, Any] = {
    int: ConcSint(0),
    bool: ConcSbool.FALSE,
    float: ConcSdouble(0.0),
}


@dataclass
class ArrayOperation:
    """A single array operation (SELECT or STORE) recorded in history.

    Note
    ----
    ``is_store`` here is a bare boolean field; it is *not* the
    :pyattr:`mulib_python.constraints.ArrayAccessConstraint.is_store`
    property.  The two answer the same question but live in different
    layers (history vs. AST) and must not be conflated.
    """

    index: Any  # Expression for the index
    value: Any  # Expression for the value (for STORE) or result (for SELECT)
    is_store: bool  # True for STORE, False for SELECT
    constraint: Optional["Constraint"] = None  # Path constraint when this op occurred


class ArraySolverRepresentation(abc.ABC):
    """Abstract base class for array solver representations.

    Instead of using z3.Array directly, we track array operations as a history
    and generate constraints that model the array semantics. This is based on
    the Java mulib approach.
    """

    @abc.abstractmethod
    def select(
        self, index: Any, result: Any, adapter: "Z3MulibAdapter"
    ) -> List["z3.ExprRef"]:
        """Generate constraints for an array SELECT operation.
        
        Returns Z3 constraints that must hold for this SELECT to be valid.
        """

    @abc.abstractmethod
    def store(
        self, index: Any, value: Any, adapter: "Z3MulibAdapter"
    ) -> List["z3.ExprRef"]:
        """Generate constraints for an array STORE operation.
        
        Returns Z3 constraints that must hold for this STORE to be valid.
        """

    @abc.abstractmethod
    def get_default_value(self) -> Any:
        """Return the default value for uninitialized elements."""

    @abc.abstractmethod
    def copy(self) -> "ArraySolverRepresentation":
        """Create a copy of this representation for backtracking."""


class ArrayHistorySolverRepresentation(ArraySolverRepresentation):
    """Array representation that tracks SELECT/STORE history.

    For each SELECT, we generate an ITE chain over the recorded STOREs that
    bottoms out at the appropriate "uninitialized" value:

    - If the SELECT index is concrete and matches an entry in
      ``initial_values``, the bottom of the chain is that entry.
    - Otherwise the bottom of the chain is ``default_value``.

    This is based on Java's ``ArrayHistorySolverRepresentation``.
    """

    def __init__(
        self,
        array_id: str,
        element_type: type,
        length: Any,  # Can be symbolic
        default_value: Any = None,
        initial_values: Optional[Dict[int, Any]] = None,
    ) -> None:
        self._array_id = array_id
        self._element_type = element_type
        self._length = length
        # ConcSint(0) is the conservative neutral default for any solver
        # representation that can be translated through the Z3 adapter.
        # Subclasses (primitive / partner-class) override this with a
        # type-appropriate value.
        self._default_value = default_value if default_value is not None else ConcSint(0)
        self._initial_values: Dict[int, Any] = (
            dict(initial_values) if initial_values else {}
        )
        self._history: List[ArrayOperation] = []

    @property
    def array_id(self) -> str:
        return self._array_id

    @property
    def length(self) -> Any:
        return self._length

    def get_default_value(self) -> Any:
        return self._default_value

    def _bottom_value_for(self, index: Any) -> Any:
        """The "uninitialized" value seen at ``index`` before any STORE.

        For a concrete index that has a fixed initial value, that initial
        value is returned; otherwise the array's default value is returned.
        Symbolic indices always fall through to the default value.
        """
        if isinstance(index, ConcSint) and index.value in self._initial_values:
            return self._initial_values[index.value]
        return self._default_value

    def select(
        self, index: Any, result: Any, adapter: "Z3MulibAdapter"
    ) -> List["z3.ExprRef"]:
        """Generate constraints for SELECT[index] = result."""
        import z3

        z3_index = adapter.translate(index)
        z3_result = adapter.translate(result)

        # Bottom of the ITE chain: the value the array would yield if no
        # STORE matched.  This must consult ``_initial_values`` for concrete
        # indices regardless of whether STOREs have been recorded — fixing
        # a previous bug where stored arrays ignored their initial values.
        current_expr = adapter.translate(self._bottom_value_for(index))

        # Build ITE from oldest to newest so the newest store is at the top
        # of the chain (i.e., it is checked first when the constraint is
        # later evaluated).
        for op in self._history:
            if not op.is_store:
                continue
            z3_store_idx = adapter.translate(op.index)
            z3_store_val = adapter.translate(op.value)
            current_expr = z3.If(
                z3_index == z3_store_idx, z3_store_val, current_expr,
                ctx=adapter.ctx,
            )

        # Record this SELECT in history.
        self._history.append(ArrayOperation(
            index=index, value=result, is_store=False,
        ))

        return [z3_result == current_expr]

    def store(
        self, index: Any, value: Any, adapter: "Z3MulibAdapter"
    ) -> List["z3.ExprRef"]:
        """Record a STORE.  STOREs do not emit constraints by themselves;
        they are consumed by the ITE chain built on the next SELECT."""
        self._history.append(ArrayOperation(
            index=index, value=value, is_store=True,
        ))
        return []

    # -- copy/clone --------------------------------------------------------

    def _clone_attrs(self) -> Dict[str, Any]:
        """Return the keyword arguments needed to reconstruct ``self``.

        Subclasses override to forward their own constructor parameters.
        Centralising this here avoids duplicating the (validation-heavy)
        ``__init__`` logic on every backtracking copy.
        """
        return {
            "array_id": self._array_id,
            "element_type": self._element_type,
            "length": self._length,
            "default_value": self._default_value,
            "initial_values": self._initial_values,
        }

    def copy(self) -> "ArrayHistorySolverRepresentation":
        """Create a copy for backtracking.

        Bypasses ``__init__`` (and its validation) so that backtracking
        is cheap and cannot be silently broken by future tightening of
        constructor checks.  Subclasses that introduce additional state
        should override and copy that state explicitly.
        """
        clone = self.__class__.__new__(self.__class__)
        # Copy *all* private attributes from ``self``.  Using the source
        # object's ``__dict__`` (or equivalent) keeps subclasses working
        # without requiring them to override ``copy`` just to forward a new
        # field.
        for name in (
            "_array_id", "_element_type", "_length",
            "_default_value", "_initial_values",
        ):
            setattr(clone, name, getattr(self, name))
        clone._initial_values = dict(self._initial_values)
        clone._history = list(self._history)
        # Forward any subclass-specific attributes too (e.g. _check_bounds).
        for name, val in self.__dict__.items():
            if name not in (
                "_array_id", "_element_type", "_length",
                "_default_value", "_initial_values", "_history",
            ):
                setattr(clone, name, val)
        return clone


class PrimitiveValuedArraySolverRepresentation(ArrayHistorySolverRepresentation):
    """Specialized array representation for primitive-valued arrays.

    Adds bounds checking and a type-appropriate default value lookup.
    The element type *must* be one of the supported primitives
    (``int``/``bool``/``float``); otherwise the constructor raises so that
    misuse is caught immediately rather than silently masked.

    Based on Java's ``PrimitiveValuedArraySolverRepresentation``.
    """

    def __init__(
        self,
        array_id: str,
        element_type: type,
        length: Any,
        default_value: Any = None,
        initial_values: Optional[Dict[int, Any]] = None,
        check_bounds: bool = True,
    ) -> None:
        if default_value is None:
            try:
                default_value = _PRIMITIVE_DEFAULTS[element_type]
            except KeyError as exc:
                raise ValueError(
                    f"PrimitiveValuedArraySolverRepresentation only supports "
                    f"element types {sorted(_PRIMITIVE_DEFAULTS, key=str)}; "
                    f"got {element_type!r}.  Use "
                    f"SimplePartnerClassArraySolverRepresentation for arrays of "
                    f"symbolic objects."
                ) from exc

        super().__init__(array_id, element_type, length, default_value, initial_values)
        self._check_bounds = check_bounds

    def _bounds_constraints(
        self, index: Any, adapter: "Z3MulibAdapter"
    ) -> List["z3.ExprRef"]:
        """Return ``[0 <= index, index < length]`` (or ``[]`` if disabled)."""
        if not self._check_bounds:
            return []
        import z3
        z3_index = adapter.translate(index)
        z3_length = adapter.translate(self._length)
        return [
            z3_index >= z3.IntVal(0, ctx=adapter.ctx),
            z3_index < z3_length,
        ]

    def select(
        self, index: Any, result: Any, adapter: "Z3MulibAdapter"
    ) -> List["z3.ExprRef"]:
        return super().select(index, result, adapter) + self._bounds_constraints(
            index, adapter
        )

    def store(
        self, index: Any, value: Any, adapter: "Z3MulibAdapter"
    ) -> List["z3.ExprRef"]:
        return super().store(index, value, adapter) + self._bounds_constraints(
            index, adapter
        )


class SimplePartnerClassArraySolverRepresentation(PrimitiveValuedArraySolverRepresentation):
    """Solver representation for arrays of symbolic objects (partner classes).

    Mirrors Java's
    ``de.wwu.mulib.solving.object_representations.SimplePartnerClassArraySolverRepresentation``.
    Elements are *identifiers* (``ConcSint``/symbolic ``Sint``) of partner-class
    objects (or nested arrays).  The :data:`NULL_REFERENCE` sentinel is used
    in place of ``null``: a STORE of Python ``None`` is coerced to the
    sentinel, but a SELECT must never attempt to bind the sentinel directly
    to its result variable (matching the Java assertion in ``_select``).

    The element type must be a non-primitive class, which is validated at
    construction time.
    """

    def __init__(
        self,
        array_id: str,
        element_type: type,
        length: Any,
        default_value: Any = None,
        initial_values: Optional[Dict[int, Any]] = None,
        check_bounds: bool = True,
    ) -> None:
        if element_type in _PRIMITIVE_DEFAULTS:
            raise ValueError(
                "SimplePartnerClassArraySolverRepresentation requires a "
                f"non-primitive element type, got {element_type!r}.  "
                "Use PrimitiveValuedArraySolverRepresentation for primitive "
                "arrays."
            )

        if default_value is None:
            default_value = NULL_REFERENCE

        # Coerce any Python ``None`` entries in ``initial_values`` to the
        # null sentinel so we never propagate a value the adapter can't
        # translate.
        if initial_values:
            initial_values = {
                idx: (NULL_REFERENCE if val is None else val)
                for idx, val in initial_values.items()
            }

        # Bypass the parent's primitive-default lookup (which would reject
        # ``element_type``) by passing through the grandparent constructor
        # directly while preserving bounds-check behaviour.
        ArrayHistorySolverRepresentation.__init__(
            self,
            array_id=array_id,
            element_type=element_type,
            length=length,
            default_value=default_value,
            initial_values=initial_values,
        )
        self._check_bounds = check_bounds

    def store(
        self, index: Any, value: Any, adapter: "Z3MulibAdapter"
    ) -> List["z3.ExprRef"]:
        """STORE that maps Python ``None`` to :data:`NULL_REFERENCE` first."""
        if value is None:
            value = NULL_REFERENCE
        return super().store(index, value, adapter)

    def select(
        self, index: Any, result: Any, adapter: "Z3MulibAdapter"
    ) -> List["z3.ExprRef"]:
        """SELECT.

        Mirrors Java's ``SimplePartnerClassArraySolverRepresentation._select``:
        the *result* binding may not be the null sentinel itself (binding
        the null sentinel to the result would conflate "the field genuinely
        holds null" with "we don't know").  Storing ``None`` is fine
        (handled by :meth:`store`); selecting *into* the sentinel is a
        programming error and raises rather than silently being papered
        over.
        """
        if result is NULL_REFERENCE:
            raise AssertionError(
                "SimplePartnerClassArraySolverRepresentation.select called "
                "with the null-reference sentinel as its result; this almost "
                "certainly indicates a bug in the caller."
            )
        return super().select(index, result, adapter)


@dataclass
class SymbolicObjectStates:
    """Tracks all symbolic arrays and objects for the solver."""
    
    arrays: Dict[str, ArraySolverRepresentation] = field(default_factory=dict)
    objects: Dict[str, Dict[str, Any]] = field(default_factory=dict)

    def register_array(self, array_id: str, rep: ArraySolverRepresentation) -> None:
        """Register a new array representation."""
        self.arrays[array_id] = rep

    def get_array(self, array_id: str) -> Optional[ArraySolverRepresentation]:
        """Get an array representation by ID."""
        return self.arrays.get(array_id)

    def copy(self) -> "SymbolicObjectStates":
        """Create a copy for backtracking."""
        return SymbolicObjectStates(
            arrays={k: v.copy() for k, v in self.arrays.items()},
            objects={k: dict(v) for k, v in self.objects.items()},
        )


@dataclass
class IncrementalSolverState:
    """Tracks the incremental state of the solver for backtracking.

    Maintains a stack of (constraint_count, object_states) pairs
    representing the state at each backtracking point.
    """
    
    level: int = 0
    constraint_counts: List[int] = field(default_factory=list)
    object_state_stack: List[SymbolicObjectStates] = field(default_factory=list)
    current_object_states: SymbolicObjectStates = field(default_factory=SymbolicObjectStates)

    def push(self, constraint_count: int) -> None:
        """Push a new backtracking point."""
        self.level += 1
        self.constraint_counts.append(constraint_count)
        self.object_state_stack.append(self.current_object_states.copy())

    def pop(self) -> int:
        """Pop the most recent backtracking point and return the constraint count."""
        if self.level == 0:
            raise RuntimeError("Cannot pop: no backtracking points")
        self.level -= 1
        self.current_object_states = self.object_state_stack.pop()
        return self.constraint_counts.pop()

    def pop_all(self) -> None:
        """Pop all backtracking points."""
        self.level = 0
        self.constraint_counts.clear()
        self.object_state_stack.clear()
        self.current_object_states = SymbolicObjectStates()
