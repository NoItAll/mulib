"""Abstract SolverManager base class."""

from __future__ import annotations

import abc
from typing import Any, List, TYPE_CHECKING

if TYPE_CHECKING:
    from mulib_python.constraints import (
        Constraint, ArrayAccessConstraint, ArrayInitializationConstraint
    )
    from mulib_python.solution import Solution


class SolverManager(abc.ABC):
    """Abstract base class mirroring Java's ``SolverManager`` interface.

    Each concrete subclass must implement all abstract methods.
    Backtracking points form a stack; ``add_constraint_after_new_backtracking_point``
    opens a new scope and ``backtrack_once`` / ``backtrack`` close scope(s).
    """

    # ---- constraint insertion -----------------------------------------------

    @abc.abstractmethod
    def add_constraint(self, constraint: "Constraint") -> None:
        """Add *constraint* to the current scope without creating a new level."""

    @abc.abstractmethod
    def add_constraint_after_new_backtracking_point(self, constraint: "Constraint") -> None:
        """Open a new backtracking scope and add *constraint* inside it."""

    @abc.abstractmethod
    def add_array_constraint(
        self, ac: "ArrayAccessConstraint | ArrayInitializationConstraint"
    ) -> None:
        """Add an array-related constraint (SELECT, STORE, or initialization)."""

    # ---- satisfiability -----------------------------------------------------

    @abc.abstractmethod
    def check_with_new_constraint(self, constraint: "Constraint") -> bool:
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
    ) -> "Solution":
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
    def get_up_to_n_solutions(
        self, initial_solution: "Solution", n: int
    ) -> List["Solution"]:
        """Generate up to *n* additional distinct solutions on the current path."""

    @abc.abstractmethod
    def shutdown(self) -> None:
        """Release all solver resources."""
