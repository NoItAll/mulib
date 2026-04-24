"""MulibExecutor - orchestrates symbolic execution runs."""

from __future__ import annotations

from typing import Any, Callable, Iterator, List, Optional, TYPE_CHECKING

from mulib_python.exceptions import Backtrack
from mulib_python.solution import Solution

if TYPE_CHECKING:
    from mulib_python.config import MulibConfig
    from mulib_python.solver_manager import SolverManager
    from mulib_python.search.trees.search_tree import SearchTree
    from mulib_python.search.budget.budget_manager import BudgetManager
    from mulib_python.executor.symbolic_execution import SymbolicExecution


class MulibExecutor:
    """Orchestrates symbolic execution of a search region.

    The executor:
    1. Initializes the solver, search tree, and execution context
    2. Runs the search region function
    3. Handles backtracking and exploration
    4. Collects solutions
    5. Respects budget constraints
    """

    def __init__(
        self,
        config: Optional["MulibConfig"] = None,
    ) -> None:
        self._config = config
        self._solutions: List[Solution] = []
        self._se: Optional["SymbolicExecution"] = None
        self._search_fn: Optional[Callable] = None

    @property
    def solutions(self) -> List[Solution]:
        return list(self._solutions)

    @property
    def config(self) -> Optional["MulibConfig"]:
        return self._config

    def _create_execution_context(self) -> "SymbolicExecution":
        """Create a fresh execution context based on config."""
        from mulib_python.z3_solver_manager import Z3IncrementalSolverManager
        from mulib_python.search.trees.search_tree import SearchTree
        from mulib_python.search.strategy import SearchStrategy
        from mulib_python.search.budget.budget_manager import BudgetManager
        from mulib_python.search.choice_points.choice_point_factory import SymbolicChoicePointFactory
        from mulib_python.executor.calculation_factory import SymbolicCalculationFactory
        from mulib_python.executor.symbolic_execution import SymbolicExecution
        
        # Get config values or defaults
        if self._config:
            strategy = self._config.search_strategy
            timeout_ms = self._config.solver_timeout_ms
            max_paths = self._config.budget_max_paths
            max_depth = self._config.budget_max_depth
            max_time = self._config.budget_global_time_seconds
        else:
            strategy = SearchStrategy.DFS
            timeout_ms = 10000
            max_paths = 0  # 0 means unlimited
            max_depth = 0
            max_time = 0.0
        
        # Create components
        solver = Z3IncrementalSolverManager(timeout_ms=timeout_ms)
        tree = SearchTree(strategy=strategy)
        
        budget_manager = BudgetManager()
        if max_paths > 0:
            budget_manager.set_path_budget(max_paths)
        if max_depth > 0:
            budget_manager.set_depth_budget(max_depth)
        if max_time > 0:
            budget_manager.set_global_time_budget(max_time)
        
        calc_factory = SymbolicCalculationFactory()
        choice_factory = SymbolicChoicePointFactory(solver, tree)
        
        se = SymbolicExecution(
            solver=solver,
            tree=tree,
            calculation_factory=calc_factory,
            choice_point_factory=choice_factory,
            budget_manager=budget_manager,
        )
        
        return se

    def run(self, search_fn: Callable[["SymbolicExecution"], Any]) -> List[Solution]:
        """Run symbolic execution on the given search function.

        The search function receives a SymbolicExecution context and should
        return a value. All paths through the function are explored, and
        solutions are collected.
        
        Returns the list of solutions found.
        """
        self._search_fn = search_fn
        self._solutions.clear()
        
        se = self._create_execution_context()
        self._se = se
        
        try:
            se.activate()
            self._explore(se, search_fn)
        finally:
            se.shutdown()
            self._se = None
        
        return self._solutions

    def run_iter(self, search_fn: Callable[["SymbolicExecution"], Any]) -> Iterator[Solution]:
        """Run symbolic execution, yielding solutions as they are found."""
        self._search_fn = search_fn
        self._solutions.clear()
        
        se = self._create_execution_context()
        self._se = se
        
        try:
            se.activate()
            yield from self._explore_iter(se, search_fn)
        finally:
            se.shutdown()
            self._se = None

    def _explore(self, se: "SymbolicExecution", search_fn: Callable) -> None:
        """Explore the search space, collecting all solutions."""
        for solution in self._explore_iter(se, search_fn):
            self._solutions.append(solution)

    def _explore_iter(
        self, se: "SymbolicExecution", search_fn: Callable
    ) -> Iterator[Solution]:
        """Explore the search space, yielding solutions."""
        tree = se.tree
        budget = se.budget_manager
        
        # Initial execution
        try:
            result = search_fn(se)
            solution = se.label_solution(result)
            tree.mark_solution(result, se.get_path_constraints(), solution)
            yield solution
            
            if budget:
                budget.increment_path_count()
        except Backtrack:
            pass
        except Exception as e:
            tree.mark_fail(f"exception: {e}", e)
        
        # Continue exploring while there are pending options
        while not tree.is_empty():
            # Check budget
            if budget and budget.is_any_exceeded():
                exceeded = budget.get_first_exceeded()
                tree.mark_exceeded_budget(exceeded.budget_type() if exceeded else "unknown")
                break
            
            # Get next option
            option = tree.next_option()
            if option is None:
                break
            
            # Backtrack solver to this point
            # Calculate how many levels to backtrack
            current_level = se.solver.get_level()
            target_level = option.depth - 1
            if current_level > target_level:
                se.solver.backtrack(current_level - target_level)
            
            # Add this option's constraint
            if option.option_constraint:
                se.solver.add_constraint_after_new_backtracking_point(option.option_constraint)
            
            # Execute from this point
            try:
                result = search_fn(se)
                solution = se.label_solution(result)
                tree.mark_solution(result, se.get_path_constraints(), solution)
                yield solution
                
                if budget:
                    budget.increment_path_count()
            except Backtrack:
                pass
            except Exception as e:
                tree.mark_fail(f"exception: {e}", e)

    def get_all_solutions(self) -> List[Solution]:
        """Get all solutions found so far."""
        return list(self._solutions)

    def get_first_solution(self) -> Optional[Solution]:
        """Get the first solution, or None if no solutions found."""
        return self._solutions[0] if self._solutions else None


def run_mulib(
    search_fn: Callable[["SymbolicExecution"], Any],
    config: Optional["MulibConfig"] = None,
) -> List[Solution]:
    """Convenience function to run symbolic execution.

    Parameters
    ----------
    search_fn : callable
        The search region function. It receives a SymbolicExecution context
        and should return a value.
    config : MulibConfig, optional
        Configuration for the execution.

    Returns
    -------
    list[Solution]
        All solutions found.
    """
    executor = MulibExecutor(config)
    return executor.run(search_fn)


def run_mulib_iter(
    search_fn: Callable[["SymbolicExecution"], Any],
    config: Optional["MulibConfig"] = None,
) -> Iterator[Solution]:
    """Convenience function to run symbolic execution with lazy solution generation.

    Parameters
    ----------
    search_fn : callable
        The search region function.
    config : MulibConfig, optional
        Configuration for the execution.

    Yields
    ------
    Solution
        Solutions as they are found.
    """
    executor = MulibExecutor(config)
    yield from executor.run_iter(search_fn)
