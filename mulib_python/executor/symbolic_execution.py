"""SymbolicExecution - the core execution context."""

from __future__ import annotations

import threading
from typing import Any, Callable, Dict, List, Optional, TYPE_CHECKING

from mulib_python.exceptions import Backtrack
from mulib_python.constraints import Constraint, Not, Eq
from mulib_python.solution import Solution

if TYPE_CHECKING:
    from mulib_python.solver_manager import SolverManager
    from mulib_python.search.trees.search_tree import SearchTree
    from mulib_python.search.budget.budget_manager import BudgetManager
    from mulib_python.search.choice_points.choice_point_factory import ChoicePointFactory
    from mulib_python.executor.calculation_factory import CalculationFactory


class SymbolicExecution:
    """The core symbolic execution context.

    Manages the state of a single symbolic execution run, including:
    - The solver for constraint solving
    - The search tree for tracking exploration
    - The calculation factory for creating symbolic values
    - Remembered variables for solution labeling
    - Budget management
    
    This class is **NOT thread-safe**. Each thread should have its own
    SymbolicExecution instance (see _se_context.py for thread-local access).
    """

    def __init__(
        self,
        solver: "SolverManager",
        tree: "SearchTree",
        calculation_factory: "CalculationFactory",
        choice_point_factory: "ChoicePointFactory",
        budget_manager: Optional["BudgetManager"] = None,
    ) -> None:
        self._solver = solver
        self._tree = tree
        self._calc_factory = calculation_factory
        self._choice_factory = choice_point_factory
        self._budget_manager = budget_manager
        
        self._remembered: Dict[str, Any] = {}
        self._path_constraints: List[Constraint] = []
        self._depth = 0
        self._active = False

    # ---- Properties ----

    @property
    def solver(self) -> "SolverManager":
        return self._solver

    @property
    def tree(self) -> "SearchTree":
        return self._tree

    @property
    def calculation_factory(self) -> "CalculationFactory":
        return self._calc_factory

    @property
    def choice_point_factory(self) -> "ChoicePointFactory":
        return self._choice_factory

    @property
    def budget_manager(self) -> Optional["BudgetManager"]:
        return self._budget_manager

    @property
    def depth(self) -> int:
        return self._depth

    @property
    def is_active(self) -> bool:
        return self._active

    # ---- Symbolic value creation ----

    def sym_int(self, name: Optional[str] = None) -> Any:
        """Create a symbolic integer."""
        return self._calc_factory.create_symbolic_int(name)

    def sym_long(self, name: Optional[str] = None) -> Any:
        """Create a symbolic long."""
        return self._calc_factory.create_symbolic_long(name)

    def sym_double(self, name: Optional[str] = None) -> Any:
        """Create a symbolic double."""
        return self._calc_factory.create_symbolic_double(name)

    def sym_float(self, name: Optional[str] = None) -> Any:
        """Create a symbolic float."""
        return self._calc_factory.create_symbolic_float(name)

    def sym_bool(self, name: Optional[str] = None) -> Any:
        """Create a symbolic boolean."""
        return self._calc_factory.create_symbolic_bool(name)

    def sym_byte(self, name: Optional[str] = None) -> Any:
        """Create a symbolic byte."""
        return self._calc_factory.create_symbolic_byte(name)

    def sym_char(self, name: Optional[str] = None) -> Any:
        """Create a symbolic character."""
        return self._calc_factory.create_symbolic_char(name)

    def sym_short(self, name: Optional[str] = None) -> Any:
        """Create a symbolic short."""
        return self._calc_factory.create_symbolic_short(name)

    # ---- Constraint management ----

    def add_constraint(self, constraint: Constraint) -> None:
        """Add a constraint to the current path."""
        self._solver.add_constraint(constraint)
        self._path_constraints.append(constraint)

    def assume(self, constraint: Constraint) -> None:
        """Assume a constraint holds (synonym for add_constraint)."""
        self.add_constraint(constraint)

    def check(self, constraint: Constraint) -> bool:
        """Check if adding a constraint keeps the path satisfiable."""
        return self._solver.check_with_new_constraint(constraint)

    def is_satisfiable(self) -> bool:
        """Check if the current path is satisfiable."""
        return self._solver.is_satisfiable()

    # ---- Choice points ----

    def bool_choice(self, condition: Any) -> bool:
        """Make a boolean choice based on a symbolic condition.

        If condition is concrete, returns its boolean value.
        If symbolic, creates a choice point and returns True or False
        based on feasibility.

        This is the method called by SymSbool.__bool__.
        """
        from mulib_python.substitutions.primitives.sint import Sbool, ConcSbool, SymSbool
        
        # Concrete case: no choice needed
        if isinstance(condition, ConcSbool):
            return condition._value
        if isinstance(condition, bool):
            return condition
        
        # Get the constraint
        if isinstance(condition, SymSbool):
            constraint = condition._represented_constraint
        elif isinstance(condition, Constraint):
            constraint = condition
        else:
            # Try to convert to bool constraint
            constraint = Eq(condition, True)
        
        # Create choice point
        constraint_for_true = constraint
        constraint_for_false = Not(constraint)
        
        # Check budget
        if self._budget_manager and self._budget_manager.is_any_exceeded():
            exceeded = self._budget_manager.get_first_exceeded()
            self._tree.mark_exceeded_budget(exceeded.budget_type() if exceeded else "unknown")
            raise Backtrack(f"Budget exceeded: {exceeded.budget_type() if exceeded else 'unknown'}")
        
        # Delegate to choice point factory
        return self._choice_factory.bool_choice(
            condition=constraint,
            constraint_for_true=constraint_for_true,
            constraint_for_false=constraint_for_false,
        )

    def int_choice(self, low: int, high: int) -> int:
        """Make an integer choice in [low, high]."""
        def constraint_fn(i: int) -> Constraint:
            return Eq(low, i)  # placeholder; raw ints coerced inside Eq

        return self._choice_factory.int_choice(low, high, constraint_fn)

    def choice(self, options: List[Any]) -> Any:
        """Make a choice among arbitrary options."""
        def constraint_fn(opt: Any) -> Constraint:
            return Eq(opt, opt)  # tautology placeholder
        
        return self._choice_factory.choice(options, constraint_fn)

    # ---- Variable remembering ----

    def remember(self, name: str, value: Any) -> None:
        """Remember a variable for later labeling."""
        self._remembered[name] = value

    def get_remembered(self) -> Dict[str, Any]:
        """Get all remembered variables."""
        return dict(self._remembered)

    def forget(self, name: str) -> None:
        """Forget a remembered variable."""
        self._remembered.pop(name, None)

    def forget_all(self) -> None:
        """Forget all remembered variables."""
        self._remembered.clear()

    # ---- Solution labeling ----

    def label_solution(self, return_value: Any) -> Solution:
        """Create a labeled solution from the current path."""
        return self._solver.label_solution(return_value, self._remembered)

    def get_label(self, var: Any) -> Any:
        """Get the concrete label for a symbolic value."""
        return self._solver.get_label(var)

    # ---- Path management ----

    def backtrack(self, levels: int = 1) -> None:
        """Backtrack the solver by n levels."""
        self._solver.backtrack(levels)
        self._depth = max(0, self._depth - levels)
        # Trim path constraints
        self._path_constraints = self._path_constraints[:-levels] if levels <= len(self._path_constraints) else []

    def get_path_constraints(self) -> tuple:
        """Get current path constraints as a tuple."""
        return tuple(self._path_constraints)

    # ---- Lifecycle ----

    def activate(self) -> None:
        """Activate this execution context."""
        self._active = True
        from mulib_python.substitutions._se_context import _set_se
        _set_se(self)

    def deactivate(self) -> None:
        """Deactivate this execution context."""
        self._active = False
        from mulib_python.substitutions._se_context import _clear_se
        _clear_se()

    def shutdown(self) -> None:
        """Shut down this execution context."""
        self.deactivate()
        self._solver.shutdown()
        self._remembered.clear()
        self._path_constraints.clear()

    # ---- Context manager ----

    def __enter__(self) -> "SymbolicExecution":
        self.activate()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb) -> None:
        self.deactivate()
