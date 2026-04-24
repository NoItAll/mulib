"""Search tree management."""

from __future__ import annotations

from typing import Any, Iterator, List, Optional, TYPE_CHECKING

from mulib_python.search.trees.choice import (
    TreeNode, Choice, ChoiceOption, PathSolutionNode, Fail, ExceededBudget,
    NodeState, TreeNodeVisitor
)
from mulib_python.search.trees.deques import SearchDeque, create_deque_for_strategy

if TYPE_CHECKING:
    from mulib_python.search.strategy import SearchStrategy
    from mulib_python.constraints import Constraint
    from mulib_python.solution import Solution


class SearchTree:
    """Manages the search tree for symbolic execution.

    The search tree tracks:
    - All choice points encountered
    - Which options have been explored
    - Solutions found
    - Failed/pruned paths

    It uses a deque to manage the frontier of unexplored options
    according to the search strategy.
    """

    def __init__(
        self,
        strategy: "SearchStrategy" = None,
        deque: Optional[SearchDeque] = None,
    ) -> None:
        from mulib_python.search.strategy import SearchStrategy
        
        if strategy is None:
            strategy = SearchStrategy.DFS
        
        self._strategy = strategy
        self._deque = deque or create_deque_for_strategy(strategy)
        self._root: Optional[TreeNode] = None
        self._current: Optional[TreeNode] = None
        self._solutions: List["Solution"] = []
        self._failed_paths: List[Fail] = []
        self._exceeded_paths: List[ExceededBudget] = []
        self._choice_count = 0

    @property
    def strategy(self) -> "SearchStrategy":
        return self._strategy

    @property
    def root(self) -> Optional[TreeNode]:
        return self._root

    @property
    def current(self) -> Optional[TreeNode]:
        return self._current

    @property
    def solutions(self) -> List["Solution"]:
        return list(self._solutions)

    @property
    def choice_count(self) -> int:
        return self._choice_count

    def is_empty(self) -> bool:
        """Return True if no unexplored options remain."""
        return self._deque.is_empty()

    def pending_count(self) -> int:
        """Return the number of pending options."""
        return len(self._deque)

    def create_choice(
        self,
        choice_type: str,
        options: List[Any],
        constraint: Optional["Constraint"] = None,
    ) -> Choice:
        """Create a new choice point with the given options.

        The options are added to the frontier according to the search strategy.
        Returns the Choice node.
        """
        parent_depth = self._current.depth if self._current else 0
        
        choice = Choice(
            choice_type=choice_type,
            constraint=constraint,
            parent=self._current,
            depth=parent_depth + 1,
        )
        
        if self._current is not None:
            self._current.add_child(choice)
        else:
            self._root = choice
        
        self._choice_count += 1
        
        # Create and add options
        for i, value in enumerate(options):
            option = ChoiceOption(
                option_index=i,
                value=value,
                parent=choice,
                depth=choice.depth + 1,
            )
            choice.add_child(option)
            self._deque.push(option)
        
        return choice

    def next_option(self) -> Optional[ChoiceOption]:
        """Get the next option to explore from the frontier.

        Returns None if no options remain.
        """
        while not self._deque.is_empty():
            option = self._deque.pop()
            if option is not None and option.state == NodeState.OPEN:
                option.set_state(NodeState.ACTIVE)
                self._current = option
                return option
        return None

    def mark_solution(
        self,
        return_value: Any,
        path_constraints: tuple = (),
        solution: Optional["Solution"] = None,
    ) -> PathSolutionNode:
        """Mark the current path as a successful solution."""
        node = PathSolutionNode(
            return_value=return_value,
            path_constraints=path_constraints,
            parent=self._current,
            depth=self._current.depth + 1 if self._current else 1,
        )
        
        if self._current is not None:
            self._current.add_child(node)
            self._current.set_state(NodeState.CLOSED)
        
        if solution is not None:
            self._solutions.append(solution)
        
        return node

    def mark_fail(
        self,
        reason: str = "infeasible",
        exception: Optional[Exception] = None,
    ) -> Fail:
        """Mark the current path as failed."""
        node = Fail(
            reason=reason,
            exception=exception,
            parent=self._current,
            depth=self._current.depth + 1 if self._current else 1,
        )
        
        if self._current is not None:
            self._current.add_child(node)
            self._current.set_state(NodeState.PRUNED)
        
        self._failed_paths.append(node)
        return node

    def mark_exceeded_budget(self, budget_type: str = "unknown") -> ExceededBudget:
        """Mark the current path as exceeded budget."""
        node = ExceededBudget(
            budget_type=budget_type,
            parent=self._current,
            depth=self._current.depth + 1 if self._current else 1,
        )
        
        if self._current is not None:
            self._current.add_child(node)
            self._current.set_state(NodeState.EXCEEDED)
        
        self._exceeded_paths.append(node)
        return node

    def backtrack_to(self, node: TreeNode) -> None:
        """Set the current node for backtracking purposes."""
        self._current = node

    def get_path_to_current(self) -> List[TreeNode]:
        """Get the path from root to current node."""
        if self._current is None:
            return []
        
        path = []
        node: Optional[TreeNode] = self._current
        while node is not None:
            path.append(node)
            node = node.parent
        
        path.reverse()
        return path

    def get_path_constraints(self) -> tuple:
        """Get all constraints along the path to current."""
        constraints = []
        for node in self.get_path_to_current():
            if isinstance(node, ChoiceOption) and node.option_constraint:
                constraints.append(node.option_constraint)
            elif isinstance(node, Choice) and node.constraint:
                constraints.append(node.constraint)
        return tuple(constraints)

    def depth(self) -> int:
        """Return the current depth in the tree."""
        return self._current.depth if self._current else 0

    def all_nodes(self) -> Iterator[TreeNode]:
        """Iterate over all nodes in the tree (BFS order)."""
        if self._root is None:
            return
        
        queue = [self._root]
        while queue:
            node = queue.pop(0)
            yield node
            queue.extend(node.children)

    def clear(self) -> None:
        """Clear the search tree."""
        self._root = None
        self._current = None
        self._solutions.clear()
        self._failed_paths.clear()
        self._exceeded_paths.clear()
        self._deque.clear()
        self._choice_count = 0


class TreeStatisticsVisitor(TreeNodeVisitor):
    """Visitor that collects statistics about the search tree."""

    def __init__(self) -> None:
        self.choice_count = 0
        self.option_count = 0
        self.solution_count = 0
        self.fail_count = 0
        self.exceeded_count = 0
        self.max_depth = 0

    def visit_choice(self, node: Choice) -> None:
        self.choice_count += 1
        self.max_depth = max(self.max_depth, node.depth)

    def visit_choice_option(self, node: ChoiceOption) -> None:
        self.option_count += 1
        self.max_depth = max(self.max_depth, node.depth)

    def visit_path_solution(self, node: PathSolutionNode) -> None:
        self.solution_count += 1
        self.max_depth = max(self.max_depth, node.depth)

    def visit_fail(self, node: Fail) -> None:
        self.fail_count += 1
        self.max_depth = max(self.max_depth, node.depth)

    def visit_exceeded_budget(self, node: ExceededBudget) -> None:
        self.exceeded_count += 1
        self.max_depth = max(self.max_depth, node.depth)


def collect_statistics(tree: SearchTree) -> TreeStatisticsVisitor:
    """Collect statistics about a search tree."""
    visitor = TreeStatisticsVisitor()
    for node in tree.all_nodes():
        node.accept(visitor)
    return visitor
