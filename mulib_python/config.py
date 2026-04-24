"""Configuration for mulib_python.

This module provides comprehensive configuration options for symbolic execution,
modeled after the Java mulib library's MulibConfig.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from enum import Enum, auto
from typing import Any, Callable, Dict, List, Optional, Set, TYPE_CHECKING

if TYPE_CHECKING:
    from mulib_python.search.strategy import SearchStrategy


class SolverType(Enum):
    """Solver backend type."""
    Z3_INCREMENTAL = auto()
    Z3_GLOBAL_LEARNING = auto()


class ArraySolverType(Enum):
    """Array constraint solving strategy."""
    HISTORY_BASED = auto()      # Track SELECT/STORE history
    EAGER_ARRAY_THEORY = auto()  # Use solver's native array theory


class ExecutionMode(Enum):
    """Execution mode for symbolic execution."""
    SYMBOLIC = auto()      # Pure symbolic execution
    CONCOLIC = auto()      # Concolic execution with concrete inputs


class LabelOption(Enum):
    """Options for labeling/concretizing values."""
    INTEGERS_AS_INTS = auto()
    INTEGERS_AS_BOOLS = auto()  # Treat integers as booleans when possible


@dataclass
class MulibConfig:
    """Configuration for mulib symbolic execution.

    This is an immutable configuration object. Use MulibConfigBuilder
    to create instances with custom values.

    Attributes
    ----------
    search_strategy : SearchStrategy
        The search strategy for exploring paths.
    execution_mode : ExecutionMode
        Whether to use pure symbolic or concolic execution.
    solver_type : SolverType
        The solver backend to use.
    array_solver_type : ArraySolverType
        The array constraint solving strategy.
    solver_timeout_ms : int
        Timeout for individual solver calls in milliseconds.
    budget_global_time_seconds : float
        Global time budget in seconds (0 for unlimited).
    budget_max_paths : int
        Maximum number of paths to explore (0 for unlimited).
    budget_max_depth : int
        Maximum search depth (0 for unlimited).
    budget_max_choice_points : int
        Maximum choice points to create (0 for unlimited).
    search_allow_exceptions : bool
        Whether to allow exceptions in search region.
    treat_bools_as_ints : bool
        Whether to represent booleans as integers in the solver.
    free_variables_as_bounded : bool
        Whether free symbolic variables should have bounded ranges.
    int_min : int
        Minimum value for bounded integers.
    int_max : int
        Maximum value for bounded integers.
    label_options : Set[LabelOption]
        Options for labeling/concretizing values.
    """

    # Search configuration
    search_strategy: "SearchStrategy" = None  # type: ignore
    execution_mode: ExecutionMode = ExecutionMode.SYMBOLIC
    
    # Solver configuration
    solver_type: SolverType = SolverType.Z3_INCREMENTAL
    array_solver_type: ArraySolverType = ArraySolverType.HISTORY_BASED
    solver_timeout_ms: int = 10000
    
    # Budget configuration
    budget_global_time_seconds: float = 0.0  # 0 means unlimited
    budget_max_paths: int = 0
    budget_max_depth: int = 0
    budget_max_choice_points: int = 0
    
    # Execution options
    search_allow_exceptions: bool = False
    treat_bools_as_ints: bool = False
    
    # Value bounds
    free_variables_as_bounded: bool = False
    int_min: int = -(2**31)
    int_max: int = 2**31 - 1
    
    # Label options
    label_options: Set[LabelOption] = field(default_factory=set)
    
    # Custom hooks
    post_run_hooks: List[Callable] = field(default_factory=list)
    
    def __post_init__(self) -> None:
        # Import here to avoid circular imports
        from mulib_python.search.strategy import SearchStrategy
        if self.search_strategy is None:
            self.search_strategy = SearchStrategy.DFS

    @staticmethod
    def default() -> "MulibConfig":
        """Create a default configuration."""
        return MulibConfig()

    @staticmethod
    def builder() -> "MulibConfigBuilder":
        """Create a builder for customizing configuration."""
        return MulibConfigBuilder()


class MulibConfigBuilder:
    """Builder for creating MulibConfig instances.

    Example
    -------
    >>> config = (MulibConfig.builder()
    ...     .set_search_strategy(SearchStrategy.BFS)
    ...     .set_solver_timeout(5000)
    ...     .set_max_paths(100)
    ...     .build())
    """

    def __init__(self) -> None:
        from mulib_python.search.strategy import SearchStrategy
        self._search_strategy = SearchStrategy.DFS
        self._execution_mode = ExecutionMode.SYMBOLIC
        self._solver_type = SolverType.Z3_INCREMENTAL
        self._array_solver_type = ArraySolverType.HISTORY_BASED
        self._solver_timeout_ms = 10000
        self._budget_global_time_seconds = 0.0
        self._budget_max_paths = 0
        self._budget_max_depth = 0
        self._budget_max_choice_points = 0
        self._search_allow_exceptions = False
        self._treat_bools_as_ints = False
        self._free_variables_as_bounded = False
        self._int_min = -(2**31)
        self._int_max = 2**31 - 1
        self._label_options: Set[LabelOption] = set()
        self._post_run_hooks: List[Callable] = []

    def set_search_strategy(self, strategy: "SearchStrategy") -> "MulibConfigBuilder":
        """Set the search strategy."""
        self._search_strategy = strategy
        return self

    def set_execution_mode(self, mode: ExecutionMode) -> "MulibConfigBuilder":
        """Set the execution mode."""
        self._execution_mode = mode
        return self

    def set_solver_type(self, solver_type: SolverType) -> "MulibConfigBuilder":
        """Set the solver type."""
        self._solver_type = solver_type
        return self

    def set_array_solver_type(self, array_type: ArraySolverType) -> "MulibConfigBuilder":
        """Set the array solver type."""
        self._array_solver_type = array_type
        return self

    def set_solver_timeout(self, timeout_ms: int) -> "MulibConfigBuilder":
        """Set solver timeout in milliseconds."""
        self._solver_timeout_ms = timeout_ms
        return self

    def set_global_time_budget(self, seconds: float) -> "MulibConfigBuilder":
        """Set global time budget in seconds."""
        self._budget_global_time_seconds = seconds
        return self

    def set_max_paths(self, max_paths: int) -> "MulibConfigBuilder":
        """Set maximum number of paths."""
        self._budget_max_paths = max_paths
        return self

    def set_max_depth(self, max_depth: int) -> "MulibConfigBuilder":
        """Set maximum search depth."""
        self._budget_max_depth = max_depth
        return self

    def set_max_choice_points(self, max_choice_points: int) -> "MulibConfigBuilder":
        """Set maximum choice points."""
        self._budget_max_choice_points = max_choice_points
        return self

    def set_allow_exceptions(self, allow: bool) -> "MulibConfigBuilder":
        """Set whether to allow exceptions in search region."""
        self._search_allow_exceptions = allow
        return self

    def set_treat_bools_as_ints(self, treat_as_ints: bool) -> "MulibConfigBuilder":
        """Set whether to represent booleans as integers."""
        self._treat_bools_as_ints = treat_as_ints
        return self

    def set_bounded_variables(
        self, bounded: bool, int_min: int = -(2**31), int_max: int = 2**31 - 1
    ) -> "MulibConfigBuilder":
        """Set whether free variables should be bounded."""
        self._free_variables_as_bounded = bounded
        self._int_min = int_min
        self._int_max = int_max
        return self

    def add_label_option(self, option: LabelOption) -> "MulibConfigBuilder":
        """Add a label option."""
        self._label_options.add(option)
        return self

    def add_post_run_hook(self, hook: Callable) -> "MulibConfigBuilder":
        """Add a post-run hook."""
        self._post_run_hooks.append(hook)
        return self

    def build(self) -> MulibConfig:
        """Build the configuration."""
        return MulibConfig(
            search_strategy=self._search_strategy,
            execution_mode=self._execution_mode,
            solver_type=self._solver_type,
            array_solver_type=self._array_solver_type,
            solver_timeout_ms=self._solver_timeout_ms,
            budget_global_time_seconds=self._budget_global_time_seconds,
            budget_max_paths=self._budget_max_paths,
            budget_max_depth=self._budget_max_depth,
            budget_max_choice_points=self._budget_max_choice_points,
            search_allow_exceptions=self._search_allow_exceptions,
            treat_bools_as_ints=self._treat_bools_as_ints,
            free_variables_as_bounded=self._free_variables_as_bounded,
            int_min=self._int_min,
            int_max=self._int_max,
            label_options=set(self._label_options),
            post_run_hooks=list(self._post_run_hooks),
        )
