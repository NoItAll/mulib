"""Choice point factory and related utilities."""

from __future__ import annotations

import abc
from typing import Any, List, Optional, TYPE_CHECKING

from mulib_python.exceptions import Backtrack, ChoicePointExceededBudget

if TYPE_CHECKING:
    from mulib_python.constraints import Constraint
    from mulib_python.search.trees.choice import Choice
    from mulib_python.search.trees.search_tree import SearchTree
    from mulib_python.solver_manager import SolverManager


class ChoicePointFactory(abc.ABC):
    """Abstract factory for creating choice points during symbolic execution.

    Different implementations can provide different strategies for
    exploring choices (e.g., concrete-first, symbolic, lazy).
    """

    @abc.abstractmethod
    def bool_choice(
        self, condition: "Constraint", constraint_for_true: "Constraint", constraint_for_false: "Constraint"
    ) -> bool:
        """Make a boolean choice based on a symbolic condition.

        Returns True or False. If the path is infeasible, raises Backtrack.
        """

    @abc.abstractmethod
    def int_choice(
        self, low: int, high: int, constraint_fn: "callable[[int], Constraint]"
    ) -> int:
        """Make an integer choice in [low, high].

        constraint_fn(i) returns the constraint for choosing value i.
        Returns the chosen value. If no values are feasible, raises Backtrack.
        """

    @abc.abstractmethod
    def choice(self, options: List[Any], constraint_fn: "callable[[Any], Constraint]") -> Any:
        """Make a choice among arbitrary options.

        constraint_fn(option) returns the constraint for that option.
        Returns the chosen option. If no options are feasible, raises Backtrack.
        """


class ConcolicChoicePointFactory(ChoicePointFactory):
    """Choice point factory that tries concrete values first.

    For concolic execution: if the concrete value is feasible, use it;
    otherwise, try alternatives.
    """

    def __init__(
        self,
        solver: "SolverManager",
        tree: "SearchTree",
    ) -> None:
        self._solver = solver
        self._tree = tree

    def bool_choice(
        self, condition: "Constraint", constraint_for_true: "Constraint", constraint_for_false: "Constraint"
    ) -> bool:
        """Make a boolean choice."""
        from mulib_python.search.trees.choice import ChoiceOption
        
        # Create choice node in tree
        choice = self._tree.create_choice(
            choice_type="bool",
            options=[True, False],
            constraint=condition,
        )
        
        # Get the options and set their constraints
        true_option = choice.get_option(0)
        false_option = choice.get_option(1)
        if true_option:
            true_option.option_constraint = constraint_for_true
        if false_option:
            false_option.option_constraint = constraint_for_false
        
        # Try True first
        if self._solver.check_with_new_constraint(constraint_for_true):
            self._solver.add_constraint_after_new_backtracking_point(constraint_for_true)
            if true_option:
                self._tree.backtrack_to(true_option)
            return True
        
        # Try False
        if self._solver.check_with_new_constraint(constraint_for_false):
            self._solver.add_constraint_after_new_backtracking_point(constraint_for_false)
            if false_option:
                self._tree.backtrack_to(false_option)
            return False
        
        # Neither feasible
        self._tree.mark_fail("bool_choice: neither branch feasible")
        raise Backtrack("bool_choice: neither branch feasible")

    def int_choice(
        self, low: int, high: int, constraint_fn: "callable[[int], Constraint]"
    ) -> int:
        """Make an integer choice."""
        options = list(range(low, high + 1))
        return self.choice(options, constraint_fn)

    def choice(self, options: List[Any], constraint_fn: "callable[[Any], Constraint]") -> Any:
        """Make a choice among options."""
        if not options:
            raise Backtrack("choice: no options")
        
        choice = self._tree.create_choice(
            choice_type="general",
            options=options,
        )
        
        # Try each option
        for i, option in enumerate(options):
            constraint = constraint_fn(option)
            opt_node = choice.get_option(i)
            if opt_node:
                opt_node.option_constraint = constraint
            
            if self._solver.check_with_new_constraint(constraint):
                self._solver.add_constraint_after_new_backtracking_point(constraint)
                if opt_node:
                    self._tree.backtrack_to(opt_node)
                return option
        
        # No option feasible
        self._tree.mark_fail("choice: no feasible option")
        raise Backtrack("choice: no feasible option")


class SymbolicChoicePointFactory(ChoicePointFactory):
    """Choice point factory for pure symbolic execution.

    Explores all branches systematically using the search tree.
    """

    def __init__(
        self,
        solver: "SolverManager",
        tree: "SearchTree",
        check_feasibility: bool = True,
    ) -> None:
        self._solver = solver
        self._tree = tree
        self._check_feasibility = check_feasibility

    def bool_choice(
        self, condition: "Constraint", constraint_for_true: "Constraint", constraint_for_false: "Constraint"
    ) -> bool:
        """Make a boolean choice, exploring both branches."""
        from mulib_python.search.trees.choice import ChoiceOption, NodeState
        
        # Create choice node
        choice = self._tree.create_choice(
            choice_type="bool",
            options=[True, False],
            constraint=condition,
        )
        
        true_option = choice.get_option(0)
        false_option = choice.get_option(1)
        if true_option:
            true_option.option_constraint = constraint_for_true
        if false_option:
            false_option.option_constraint = constraint_for_false
        
        # Check feasibility and prune infeasible branches
        true_feasible = not self._check_feasibility or self._solver.check_with_new_constraint(constraint_for_true)
        false_feasible = not self._check_feasibility or self._solver.check_with_new_constraint(constraint_for_false)
        
        if not true_feasible and true_option:
            true_option.set_state(NodeState.PRUNED)
        if not false_feasible and false_option:
            false_option.set_state(NodeState.PRUNED)
        
        # Pick a branch to explore
        if true_feasible:
            self._solver.add_constraint_after_new_backtracking_point(constraint_for_true)
            if true_option:
                self._tree.backtrack_to(true_option)
            return True
        elif false_feasible:
            self._solver.add_constraint_after_new_backtracking_point(constraint_for_false)
            if false_option:
                self._tree.backtrack_to(false_option)
            return False
        else:
            self._tree.mark_fail("bool_choice: neither branch feasible")
            raise Backtrack("bool_choice: neither branch feasible")

    def int_choice(
        self, low: int, high: int, constraint_fn: "callable[[int], Constraint]"
    ) -> int:
        """Make an integer choice."""
        options = list(range(low, high + 1))
        return self.choice(options, constraint_fn)

    def choice(self, options: List[Any], constraint_fn: "callable[[Any], Constraint]") -> Any:
        """Make a choice among options."""
        if not options:
            raise Backtrack("choice: no options")
        
        choice = self._tree.create_choice(
            choice_type="general",
            options=options,
        )
        
        from mulib_python.search.trees.choice import NodeState
        
        first_feasible: Optional[tuple[int, Any]] = None
        
        for i, option in enumerate(options):
            constraint = constraint_fn(option)
            opt_node = choice.get_option(i)
            if opt_node:
                opt_node.option_constraint = constraint
            
            feasible = not self._check_feasibility or self._solver.check_with_new_constraint(constraint)
            
            if not feasible and opt_node:
                opt_node.set_state(NodeState.PRUNED)
            elif first_feasible is None:
                first_feasible = (i, option, constraint, opt_node)
        
        if first_feasible:
            i, option, constraint, opt_node = first_feasible
            self._solver.add_constraint_after_new_backtracking_point(constraint)
            if opt_node:
                self._tree.backtrack_to(opt_node)
            return option
        
        self._tree.mark_fail("choice: no feasible option")
        raise Backtrack("choice: no feasible option")


class LazyChoicePointFactory(SymbolicChoicePointFactory):
    """Lazy choice point factory that defers constraint checking.

    Does not check feasibility until necessary, which can be more
    efficient for large search spaces with early-terminating conditions.
    """

    def __init__(
        self,
        solver: "SolverManager",
        tree: "SearchTree",
    ) -> None:
        super().__init__(solver, tree, check_feasibility=False)
