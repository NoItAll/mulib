"""Budget management for search."""

from __future__ import annotations

import abc
import time
from typing import Optional


class Budget(abc.ABC):
    """Abstract base class for search budgets.

    A budget limits the resources that can be consumed during symbolic execution.
    When the budget is exceeded, the search stops exploring new paths.
    """

    @abc.abstractmethod
    def is_exceeded(self) -> bool:
        """Return True if the budget has been exceeded."""

    @abc.abstractmethod
    def is_incurred(self) -> bool:
        """Return True if any resources have been consumed."""

    @abc.abstractmethod
    def reset(self) -> None:
        """Reset the budget to its initial state."""

    @abc.abstractmethod
    def budget_type(self) -> str:
        """Return a string identifying this budget type."""

    @abc.abstractmethod
    def copy(self) -> "Budget":
        """Create a copy of this budget."""


class NullBudget(Budget):
    """A budget that is never exceeded (unlimited resources)."""

    def is_exceeded(self) -> bool:
        return False

    def is_incurred(self) -> bool:
        return False

    def reset(self) -> None:
        pass

    def budget_type(self) -> str:
        return "null"

    def copy(self) -> "NullBudget":
        return NullBudget()


class TimeBudget(Budget):
    """A time-based budget in seconds.

    The budget is exceeded when the elapsed time since creation
    (or last reset) exceeds the limit.
    """

    def __init__(self, limit_seconds: float) -> None:
        self._limit = limit_seconds
        self._start_time = time.time()

    @property
    def limit_seconds(self) -> float:
        return self._limit

    @property
    def elapsed(self) -> float:
        return time.time() - self._start_time

    @property
    def remaining(self) -> float:
        return max(0.0, self._limit - self.elapsed)

    def is_exceeded(self) -> bool:
        return self.elapsed >= self._limit

    def is_incurred(self) -> bool:
        return self.elapsed > 0

    def reset(self) -> None:
        self._start_time = time.time()

    def budget_type(self) -> str:
        return "time"

    def copy(self) -> "TimeBudget":
        budget = TimeBudget(self._limit)
        budget._start_time = self._start_time
        return budget


class CountingBudget(Budget):
    """A counting-based budget.

    Can be used for paths, solver calls, choice points, etc.
    """

    def __init__(self, limit: int) -> None:
        self._limit = limit
        self._count = 0

    @property
    def limit(self) -> int:
        return self._limit

    @property
    def count(self) -> int:
        return self._count

    @property
    def remaining(self) -> int:
        return max(0, self._limit - self._count)

    def increment(self, amount: int = 1) -> None:
        """Increment the count."""
        self._count += amount

    def is_exceeded(self) -> bool:
        return self._count >= self._limit

    def is_incurred(self) -> bool:
        return self._count > 0

    def reset(self) -> None:
        self._count = 0

    def budget_type(self) -> str:
        return "counting"

    def copy(self) -> "CountingBudget":
        budget = CountingBudget(self._limit)
        budget._count = self._count
        return budget


class PathBudget(CountingBudget):
    """Budget limiting the number of paths explored."""

    def budget_type(self) -> str:
        return "paths"


class ChoicePointBudget(CountingBudget):
    """Budget limiting the number of choice points."""

    def budget_type(self) -> str:
        return "choice_points"


class DepthBudget(Budget):
    """Budget limiting the maximum search depth."""

    def __init__(self, max_depth: int) -> None:
        self._max_depth = max_depth
        self._current_depth = 0

    @property
    def max_depth(self) -> int:
        return self._max_depth

    @property
    def current_depth(self) -> int:
        return self._current_depth

    def set_depth(self, depth: int) -> None:
        """Set the current depth."""
        self._current_depth = depth

    def is_exceeded(self) -> bool:
        return self._current_depth > self._max_depth

    def is_incurred(self) -> bool:
        return self._current_depth > 0

    def reset(self) -> None:
        self._current_depth = 0

    def budget_type(self) -> str:
        return "depth"

    def copy(self) -> "DepthBudget":
        budget = DepthBudget(self._max_depth)
        budget._current_depth = self._current_depth
        return budget


class SolverCallBudget(CountingBudget):
    """Budget limiting the number of solver calls."""

    def budget_type(self) -> str:
        return "solver_calls"


class CompositeBudget(Budget):
    """A budget composed of multiple sub-budgets.

    The composite is exceeded if any sub-budget is exceeded.
    """

    def __init__(self, *budgets: Budget) -> None:
        self._budgets = list(budgets)

    @property
    def budgets(self) -> list[Budget]:
        return list(self._budgets)

    def add_budget(self, budget: Budget) -> None:
        """Add a sub-budget."""
        self._budgets.append(budget)

    def is_exceeded(self) -> bool:
        return any(b.is_exceeded() for b in self._budgets)

    def is_incurred(self) -> bool:
        return any(b.is_incurred() for b in self._budgets)

    def reset(self) -> None:
        for b in self._budgets:
            b.reset()

    def budget_type(self) -> str:
        return "composite"

    def get_exceeded_budget(self) -> Optional[Budget]:
        """Return the first exceeded budget, or None."""
        for b in self._budgets:
            if b.is_exceeded():
                return b
        return None

    def copy(self) -> "CompositeBudget":
        return CompositeBudget(*[b.copy() for b in self._budgets])
