"""Array representations for the solver using array history (not z3.Array)."""

from __future__ import annotations

import abc
from dataclasses import dataclass, field
from typing import Any, Dict, List, Optional, Tuple, TYPE_CHECKING

if TYPE_CHECKING:
    from mulib_python.constraints import Constraint
    from mulib_python.z3_adapter import Z3MulibAdapter
    import z3


@dataclass
class ArrayOperation:
    """A single array operation (SELECT or STORE)."""
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

    For each SELECT, we generate constraints that:
    - If the index matches a previous STORE index, the result equals that STORE's value
    - If the index doesn't match any STORE, the result equals the default/initial value

    This is based on Java's ArrayHistorySolverRepresentation.
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
        self._default_value = default_value if default_value is not None else 0
        self._initial_values = dict(initial_values) if initial_values else {}
        self._history: List[ArrayOperation] = []

    @property
    def array_id(self) -> str:
        return self._array_id

    @property
    def length(self) -> Any:
        return self._length

    def get_default_value(self) -> Any:
        return self._default_value

    def select(
        self, index: Any, result: Any, adapter: "Z3MulibAdapter"
    ) -> List["z3.ExprRef"]:
        """Generate constraints for SELECT[index] = result."""
        import z3

        constraints: List[z3.ExprRef] = []
        z3_index = adapter.translate(index)
        z3_result = adapter.translate(result)

        # Find all STOREs that could affect this index
        stores_at_index: List[Tuple[Any, Any]] = []
        for op in self._history:
            if op.is_store:
                stores_at_index.append((op.index, op.value))

        if not stores_at_index:
            # No stores yet - result must equal default or initial value
            # Check if index is concrete
            from mulib_python.substitutions.primitives.sint import ConcSint
            if isinstance(index, ConcSint):
                idx_val = index._value
                if idx_val in self._initial_values:
                    init_z3 = adapter.translate(self._initial_values[idx_val])
                    constraints.append(z3_result == init_z3)
                else:
                    default_z3 = adapter.translate(self._default_value)
                    constraints.append(z3_result == default_z3)
            else:
                # Symbolic index - result equals default
                default_z3 = adapter.translate(self._default_value)
                constraints.append(z3_result == default_z3)
        else:
            # Build constraint: result equals value from most recent matching store,
            # or default if no store matches
            # This is: (idx == s_n.idx => result == s_n.val) AND
            #          (idx != s_n.idx AND idx == s_{n-1}.idx => result == s_{n-1}.val) AND ...
            #          (idx != all stores => result == default)
            
            # Simpler encoding: ITE chain from newest to oldest
            default_z3 = adapter.translate(self._default_value)
            current_expr = default_z3
            
            for store_idx, store_val in reversed(stores_at_index):
                z3_store_idx = adapter.translate(store_idx)
                z3_store_val = adapter.translate(store_val)
                current_expr = z3.If(z3_index == z3_store_idx, z3_store_val, current_expr)
            
            constraints.append(z3_result == current_expr)

        # Record this SELECT in history
        self._history.append(ArrayOperation(
            index=index,
            value=result,
            is_store=False
        ))

        return constraints

    def store(
        self, index: Any, value: Any, adapter: "Z3MulibAdapter"
    ) -> List["z3.ExprRef"]:
        """Generate constraints for STORE[index] = value."""
        # STOREs don't generate constraints themselves; they just update history
        # The constraints are generated when a subsequent SELECT occurs
        self._history.append(ArrayOperation(
            index=index,
            value=value,
            is_store=True
        ))
        return []

    def copy(self) -> "ArrayHistorySolverRepresentation":
        """Create a copy for backtracking."""
        rep = ArrayHistorySolverRepresentation(
            array_id=self._array_id,
            element_type=self._element_type,
            length=self._length,
            default_value=self._default_value,
            initial_values=self._initial_values,
        )
        rep._history = list(self._history)
        return rep


class PrimitiveValuedArraySolverRepresentation(ArrayHistorySolverRepresentation):
    """Specialized array representation for primitive-valued arrays.

    This adds bounds checking and type-specific default values.
    Based on Java's PrimitiveValuedArraySolverRepresentation.
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
        # Determine appropriate default value for primitive type
        if default_value is None:
            from mulib_python.substitutions.primitives.sint import ConcSint, ConcSbool
            from mulib_python.substitutions.primitives.slong import ConcSlong
            from mulib_python.substitutions.primitives.sdouble import ConcSdouble
            from mulib_python.substitutions.primitives.sfloat import ConcSfloat
            
            # Map element_type to default
            type_defaults = {
                int: ConcSint(0),
                bool: ConcSbool.FALSE,
                float: ConcSdouble(0.0),
            }
            default_value = type_defaults.get(element_type, ConcSint(0))

        super().__init__(array_id, element_type, length, default_value, initial_values)
        self._check_bounds = check_bounds

    def select(
        self, index: Any, result: Any, adapter: "Z3MulibAdapter"
    ) -> List["z3.ExprRef"]:
        """Generate SELECT constraints with optional bounds checking."""
        import z3

        constraints = super().select(index, result, adapter)
        
        if self._check_bounds:
            z3_index = adapter.translate(index)
            z3_length = adapter.translate(self._length)
            # 0 <= index < length
            constraints.append(z3_index >= z3.IntVal(0))
            constraints.append(z3_index < z3_length)

        return constraints

    def store(
        self, index: Any, value: Any, adapter: "Z3MulibAdapter"
    ) -> List["z3.ExprRef"]:
        """Generate STORE constraints with optional bounds checking."""
        import z3

        constraints = super().store(index, value, adapter)
        
        if self._check_bounds:
            z3_index = adapter.translate(index)
            z3_length = adapter.translate(self._length)
            # 0 <= index < length
            constraints.append(z3_index >= z3.IntVal(0))
            constraints.append(z3_index < z3_length)

        return constraints

    def copy(self) -> "PrimitiveValuedArraySolverRepresentation":
        """Create a copy for backtracking."""
        rep = PrimitiveValuedArraySolverRepresentation(
            array_id=self._array_id,
            element_type=self._element_type,
            length=self._length,
            default_value=self._default_value,
            initial_values=self._initial_values,
            check_bounds=self._check_bounds,
        )
        rep._history = list(self._history)
        return rep


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
