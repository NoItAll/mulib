"""Z3 incremental solver manager implementation."""

from __future__ import annotations

import functools
import threading
from typing import Any, Callable, Dict, List, Optional, TYPE_CHECKING

import z3

from mulib_python.solver_manager import SolverManager
from mulib_python.solution import Labels, Solution
from mulib_python.z3_adapter import Z3MulibAdapter
from mulib_python.array_repr import (
    ArraySolverRepresentation, IncrementalSolverState, SymbolicObjectStates
)
from mulib_python.constraints import (
    Constraint, ArrayAccessConstraint, ArrayInitializationConstraint
)

if TYPE_CHECKING:
    pass


# Z3's Python bindings share global C state; a process-wide reentrant lock
# serializes all solver/adapter access so multiple threads can each drive
# their own SymbolicExecution without corrupting Z3.  This mirrors the
# ``syncObject`` used in the Java implementation
# (``de.wwu.mulib.solving.solvers.Z3SolverManager``).
_Z3_LOCK: threading.RLock = threading.RLock()


def _z3_locked(method: Callable[..., Any]) -> Callable[..., Any]:
    """Decorator that serializes a method behind :data:`_Z3_LOCK`."""

    @functools.wraps(method)
    def wrapper(self: Any, *args: Any, **kwargs: Any) -> Any:
        with _Z3_LOCK:
            return method(self, *args, **kwargs)

    return wrapper


class Z3IncrementalSolverManager(SolverManager):
    """Z3-based incremental solver manager.

    Uses Z3's push/pop mechanism for efficient backtracking.
    Translates mulib AST to Z3 via Z3MulibAdapter.
    """

    def __init__(
        self,
        timeout_ms: Optional[int] = None,
        treat_bools_as_ints: bool = False,
    ) -> None:
        # Each manager owns its own Z3 context so that solver instances in
        # different threads do not share C-level state.  This is the same
        # design used by the Java mulib (one Context per SolverManager).
        self._ctx: z3.Context = z3.Context()
        self._solver = z3.Solver(ctx=self._ctx)
        if timeout_ms is not None:
            self._solver.set("timeout", timeout_ms)

        self._adapter = Z3MulibAdapter(
            treat_bools_as_ints=treat_bools_as_ints, ctx=self._ctx
        )
        self._level = 0
        self._state = IncrementalSolverState()
        self._model: Optional[z3.ModelRef] = None
        self._label_cache: Dict[int, Any] = {}

    @property
    def ctx(self) -> z3.Context:
        """The Z3 context owned by this manager."""
        return self._ctx

    @property
    def solver(self) -> z3.Solver:
        """Access the underlying Z3 solver."""
        return self._solver

    @property
    def adapter(self) -> Z3MulibAdapter:
        """Access the Z3 adapter."""
        return self._adapter

    @_z3_locked
    def add_constraint(self, constraint: Constraint) -> None:
        """Add constraint to current scope."""
        z3_constraint = self._adapter.translate(constraint)
        self._solver.add(z3_constraint)
        self._model = None  # Invalidate cached model

    @_z3_locked
    def add_constraint_after_new_backtracking_point(self, constraint: Constraint) -> None:
        """Open a new backtracking scope and add constraint."""
        self._solver.push()
        self._level += 1
        self._state.push(len(self._solver.assertions()))
        z3_constraint = self._adapter.translate(constraint)
        self._solver.add(z3_constraint)
        self._model = None

    @_z3_locked
    def add_array_constraint(
        self, ac: ArrayAccessConstraint | ArrayInitializationConstraint
    ) -> None:
        """Add an array-related constraint."""
        if isinstance(ac, ArrayInitializationConstraint):
            # Register the array with our state.  The element type decides
            # whether this is a primitive-valued array or an array of
            # symbolic objects (partner classes).
            from mulib_python.array_repr import (
                PrimitiveValuedArraySolverRepresentation,
                PartnerClassArraySolverRepresentation,
            )

            if ac.element_type in (int, bool, float):
                rep = PrimitiveValuedArraySolverRepresentation(
                    array_id=ac.array_id,
                    element_type=ac.element_type,
                    length=ac.length,
                    default_value=ac.default_value,
                    initial_values=ac.initial_values,
                )
            else:
                rep = PartnerClassArraySolverRepresentation(
                    array_id=ac.array_id,
                    element_type=ac.element_type,
                    length=ac.length,
                    default_value=ac.default_value,
                    initial_values=ac.initial_values,
                )
            self._state.current_object_states.register_array(ac.array_id, rep)
        
        elif isinstance(ac, ArrayAccessConstraint):
            # Generate SELECT/STORE constraints
            rep = self._state.current_object_states.get_array(ac.array_id)
            if rep is None:
                raise ValueError(f"Array {ac.array_id} not initialized")
            
            if ac.is_store:
                constraints = rep.store(ac.index, ac.value, self._adapter)
            else:
                constraints = rep.select(ac.index, ac.value, self._adapter)
            
            for c in constraints:
                self._solver.add(c)
            self._model = None

    @_z3_locked
    def check_with_new_constraint(self, constraint: Constraint) -> bool:
        """Check if adding constraint keeps the system satisfiable."""
        z3_constraint = self._adapter.translate(constraint)
        self._solver.push()
        self._solver.add(z3_constraint)
        result = self._solver.check()
        self._solver.pop()
        return result == z3.sat

    @_z3_locked
    def is_satisfiable(self) -> bool:
        """Check if current constraint stack is satisfiable."""
        result = self._solver.check()
        if result == z3.sat:
            self._model = self._solver.model()
            return True
        return False

    @_z3_locked
    def backtrack_once(self) -> None:
        """Remove most recent backtracking scope."""
        if self._level > 0:
            self._solver.pop()
            self._level -= 1
            self._state.pop()
            self._model = None

    @_z3_locked
    def backtrack(self, n: int) -> None:
        """Remove n most recent backtracking scopes."""
        for _ in range(n):
            self.backtrack_once()

    @_z3_locked
    def backtrack_all(self) -> None:
        """Remove all backtracking scopes."""
        while self._level > 0:
            self.backtrack_once()

    @_z3_locked
    def get_label(self, var: Any) -> Any:
        """Evaluate var in current model."""
        var_id = id(var)
        if var_id in self._label_cache:
            return self._label_cache[var_id]
        
        if self._model is None:
            if not self.is_satisfiable():
                raise RuntimeError("Cannot get label: system unsatisfiable")
        
        label = self._adapter.extract_value(self._model, var)
        self._label_cache[var_id] = label
        return label

    @_z3_locked
    def label_solution(
        self,
        return_value: Any,
        remembered: Dict[str, Any],
    ) -> Solution:
        """Construct a Solution with labeled values."""
        if self._model is None:
            if not self.is_satisfiable():
                raise RuntimeError("Cannot label solution: system unsatisfiable")
        
        id_to_var: Dict[str, Any] = {"return": return_value}
        id_to_label: Dict[str, Any] = {}
        
        # Label return value
        id_to_label["return"] = self._concretize(return_value)
        
        # Label remembered variables
        for name, var in remembered.items():
            id_to_var[name] = var
            id_to_label[name] = self._concretize(var)
        
        labels = Labels(id_to_var, id_to_label)
        return Solution(return_value=id_to_label["return"], labels=labels)

    @_z3_locked
    def _concretize(self, value: Any) -> Any:
        """Convert a potentially symbolic value to a concrete one."""
        from mulib_python.substitutions.primitives.sint import (
            ConcSint, ConcSbool, Sint, Sbool
        )
        from mulib_python.substitutions.primitives.slong import ConcSlong, Slong
        from mulib_python.substitutions.primitives.sdouble import ConcSdouble, Sdouble
        from mulib_python.substitutions.primitives.sfloat import ConcSfloat, Sfloat

        # Already concrete primitive?
        if isinstance(value, (ConcSint, ConcSbool, ConcSlong, ConcSdouble, ConcSfloat)):
            return value._value
        
        # Python primitive?
        if isinstance(value, (int, float, bool, str, type(None))):
            return value
        
        # Symbolic value - need to get label from model
        if isinstance(value, (Sint, Sbool, Slong, Sdouble, Sfloat)):
            return self.get_label(value)
        
        # Unknown type - try to get label
        try:
            return self.get_label(value)
        except TypeError:
            return value

    @_z3_locked
    def reset_labels(self) -> None:
        """Clear the label cache."""
        self._label_cache.clear()

    @_z3_locked
    def register_label_pair(self, search_repr: Any, label: Any) -> None:
        """Cache a label pair."""
        self._label_cache[id(search_repr)] = label

    @_z3_locked
    def get_level(self) -> int:
        """Return current backtracking depth."""
        return self._level

    @_z3_locked
    def get_up_to_n_solutions(
        self, initial_solution: Solution, n: int
    ) -> List[Solution]:
        """Generate up to n additional distinct solutions."""
        solutions: List[Solution] = [initial_solution]
        
        if n <= 0:
            return solutions
        
        # Get initial model values
        initial_labels = initial_solution.labels.id_to_label
        
        for _ in range(n):
            # Add constraint to exclude current solution
            exclusion_clauses = []
            for name, label in initial_labels.items():
                if name == "return":
                    continue
                var = initial_solution.labels.get_named_var(name)
                if var is not None:
                    try:
                        z3_var = self._adapter.translate(var)
                        z3_label = self._to_z3_value(label)
                        exclusion_clauses.append(z3_var != z3_label)
                    except TypeError:
                        continue
            
            if not exclusion_clauses:
                break
            
            self._solver.push()
            self._solver.add(z3.Or(*exclusion_clauses))
            if self._solver.check() == z3.sat:
                self._model = self._solver.model()
                self.reset_labels()
                
                # Build new solution
                id_to_var = initial_solution.labels.id_to_named_var
                id_to_label = {}
                for name, var in id_to_var.items():
                    id_to_label[name] = self._concretize(var)
                
                new_solution = Solution(
                    return_value=id_to_label.get("return"),
                    labels=Labels(id_to_var, id_to_label)
                )
                solutions.append(new_solution)
                initial_labels = id_to_label
            else:
                self._solver.pop()
                break
            
            self._solver.pop()
        
        return solutions

    def _to_z3_value(self, value: Any) -> z3.ExprRef:
        """Convert a Python value to a Z3 value in this manager's context."""
        ctx = self._ctx
        if isinstance(value, bool):
            return z3.BoolVal(value, ctx=ctx)
        if isinstance(value, int):
            return z3.IntVal(value, ctx=ctx)
        if isinstance(value, float):
            return z3.RealVal(value, ctx=ctx)
        return z3.IntVal(0, ctx=ctx)

    @_z3_locked
    def shutdown(self) -> None:
        """Release solver resources."""
        self._solver = None
        self._model = None
        self._label_cache.clear()
        self._adapter.clear_cache()


class Z3GlobalLearningSolverManager(Z3IncrementalSolverManager):
    """Z3 solver manager with global constraint learning.

    Remembers constraints that are universally true across the search,
    improving performance by avoiding redundant work.
    """

    def __init__(
        self,
        timeout_ms: Optional[int] = None,
        treat_bools_as_ints: bool = False,
    ) -> None:
        super().__init__(timeout_ms, treat_bools_as_ints)
        self._global_constraints: List[Constraint] = []
        self._learned_facts: List[z3.ExprRef] = []

    @_z3_locked
    def add_global_constraint(self, constraint: Constraint) -> None:
        """Add a constraint that applies globally (survives backtracking)."""
        z3_constraint = self._adapter.translate(constraint)
        self._global_constraints.append(constraint)
        self._learned_facts.append(z3_constraint)
        self._solver.add(z3_constraint)
        self._model = None

    @_z3_locked
    def learn_fact(self, fact: z3.ExprRef) -> None:
        """Add a learned Z3 fact that applies globally."""
        self._learned_facts.append(fact)
        self._solver.add(fact)
        self._model = None

    @_z3_locked
    def backtrack_all(self) -> None:
        """Remove all backtracking scopes but keep global constraints."""
        super().backtrack_all()
        # Re-add global constraints
        for fact in self._learned_facts:
            self._solver.add(fact)

    @property
    def global_constraints(self) -> List[Constraint]:
        """Get all global constraints."""
        return list(self._global_constraints)
