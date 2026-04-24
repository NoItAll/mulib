"""Budget manager for coordinating multiple budgets."""

from __future__ import annotations

from typing import List, Optional

from mulib_python.search.budget.budget import (
    Budget, NullBudget, TimeBudget, CountingBudget,
    PathBudget, ChoicePointBudget, DepthBudget,
    SolverCallBudget, CompositeBudget
)


class BudgetManager:
    """Manages budgets for a symbolic execution run.

    Provides a convenient interface for checking and updating multiple budgets.
    """

    def __init__(self) -> None:
        self._global_time_budget: Budget = NullBudget()
        self._path_budget: Budget = NullBudget()
        self._choice_point_budget: Budget = NullBudget()
        self._depth_budget: Budget = NullBudget()
        self._solver_call_budget: Budget = NullBudget()
        self._custom_budgets: List[Budget] = []

    # ---- Factory methods ----

    def set_global_time_budget(self, seconds: float) -> "BudgetManager":
        """Set a global time budget."""
        self._global_time_budget = TimeBudget(seconds)
        return self

    def set_path_budget(self, max_paths: int) -> "BudgetManager":
        """Set a path budget."""
        self._path_budget = PathBudget(max_paths)
        return self

    def set_choice_point_budget(self, max_choices: int) -> "BudgetManager":
        """Set a choice point budget."""
        self._choice_point_budget = ChoicePointBudget(max_choices)
        return self

    def set_depth_budget(self, max_depth: int) -> "BudgetManager":
        """Set a depth budget."""
        self._depth_budget = DepthBudget(max_depth)
        return self

    def set_solver_call_budget(self, max_calls: int) -> "BudgetManager":
        """Set a solver call budget."""
        self._solver_call_budget = SolverCallBudget(max_calls)
        return self

    def add_custom_budget(self, budget: Budget) -> "BudgetManager":
        """Add a custom budget."""
        self._custom_budgets.append(budget)
        return self

    # ---- Status queries ----

    def is_any_exceeded(self) -> bool:
        """Return True if any budget is exceeded."""
        if self._global_time_budget.is_exceeded():
            return True
        if self._path_budget.is_exceeded():
            return True
        if self._choice_point_budget.is_exceeded():
            return True
        if self._depth_budget.is_exceeded():
            return True
        if self._solver_call_budget.is_exceeded():
            return True
        return any(b.is_exceeded() for b in self._custom_budgets)

    def get_first_exceeded(self) -> Optional[Budget]:
        """Return the first exceeded budget, or None."""
        for budget in self._all_budgets():
            if budget.is_exceeded():
                return budget
        return None

    def _all_budgets(self) -> List[Budget]:
        """Return all configured budgets."""
        return [
            self._global_time_budget,
            self._path_budget,
            self._choice_point_budget,
            self._depth_budget,
            self._solver_call_budget,
            *self._custom_budgets,
        ]

    # ---- Increment methods ----

    def increment_path_count(self) -> None:
        """Increment the path counter."""
        if isinstance(self._path_budget, CountingBudget):
            self._path_budget.increment()

    def increment_choice_point_count(self) -> None:
        """Increment the choice point counter."""
        if isinstance(self._choice_point_budget, CountingBudget):
            self._choice_point_budget.increment()

    def increment_solver_calls(self, amount: int = 1) -> None:
        """Increment the solver call counter."""
        if isinstance(self._solver_call_budget, CountingBudget):
            self._solver_call_budget.increment(amount)

    def set_current_depth(self, depth: int) -> None:
        """Set the current depth for depth budget checking."""
        if isinstance(self._depth_budget, DepthBudget):
            self._depth_budget.set_depth(depth)

    # ---- Property access ----

    @property
    def global_time_budget(self) -> Budget:
        return self._global_time_budget

    @property
    def path_budget(self) -> Budget:
        return self._path_budget

    @property
    def choice_point_budget(self) -> Budget:
        return self._choice_point_budget

    @property
    def depth_budget(self) -> Budget:
        return self._depth_budget

    @property
    def solver_call_budget(self) -> Budget:
        return self._solver_call_budget

    # ---- Management ----

    def reset_all(self) -> None:
        """Reset all budgets."""
        for budget in self._all_budgets():
            budget.reset()

    def as_composite(self) -> CompositeBudget:
        """Return all budgets as a single CompositeBudget."""
        return CompositeBudget(*self._all_budgets())

    def copy(self) -> "BudgetManager":
        """Create a copy of this budget manager."""
        manager = BudgetManager()
        manager._global_time_budget = self._global_time_budget.copy()
        manager._path_budget = self._path_budget.copy()
        manager._choice_point_budget = self._choice_point_budget.copy()
        manager._depth_budget = self._depth_budget.copy()
        manager._solver_call_budget = self._solver_call_budget.copy()
        manager._custom_budgets = [b.copy() for b in self._custom_budgets]
        return manager
