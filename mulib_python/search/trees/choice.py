"""Search tree node types."""

from __future__ import annotations

import abc
from dataclasses import dataclass, field
from enum import Enum, auto
from typing import Any, List, Optional, TYPE_CHECKING

if TYPE_CHECKING:
    from mulib_python.constraints import Constraint


class NodeState(Enum):
    """State of a search tree node."""
    OPEN = auto()        # Not yet explored
    ACTIVE = auto()      # Currently being explored
    CLOSED = auto()      # Fully explored
    PRUNED = auto()      # Pruned due to infeasibility
    EXCEEDED = auto()    # Pruned due to budget


class TreeNode(abc.ABC):
    """Abstract base class for all search tree nodes.

    Tree nodes form the search tree used to explore the symbolic execution
    space. They track parent-child relationships, depth, and state.
    """

    __slots__ = ("_parent", "_children", "_depth", "_state", "_id")

    _next_id = 0

    def __init__(
        self,
        parent: Optional["TreeNode"] = None,
        depth: int = 0,
    ) -> None:
        self._parent = parent
        self._children: List["TreeNode"] = []
        self._depth = depth
        self._state = NodeState.OPEN
        TreeNode._next_id += 1
        self._id = TreeNode._next_id

    @property
    def parent(self) -> Optional["TreeNode"]:
        return self._parent

    @property
    def children(self) -> List["TreeNode"]:
        return list(self._children)

    @property
    def depth(self) -> int:
        return self._depth

    @property
    def state(self) -> NodeState:
        return self._state

    @property
    def node_id(self) -> int:
        return self._id

    def set_state(self, state: NodeState) -> None:
        self._state = state

    def add_child(self, child: "TreeNode") -> None:
        self._children.append(child)
        child._parent = self

    def is_leaf(self) -> bool:
        return len(self._children) == 0

    @abc.abstractmethod
    def accept(self, visitor: "TreeNodeVisitor") -> Any:
        """Accept a visitor (Visitor pattern)."""


class TreeNodeVisitor(abc.ABC):
    """Abstract visitor for tree nodes."""

    @abc.abstractmethod
    def visit_choice(self, node: "Choice") -> Any: ...

    @abc.abstractmethod
    def visit_choice_option(self, node: "ChoiceOption") -> Any: ...

    @abc.abstractmethod
    def visit_path_solution(self, node: "PathSolutionNode") -> Any: ...

    @abc.abstractmethod
    def visit_fail(self, node: "Fail") -> Any: ...

    @abc.abstractmethod
    def visit_exceeded_budget(self, node: "ExceededBudget") -> Any: ...


@dataclass
class Choice(TreeNode):
    """A choice point in the search tree.

    Represents a point where symbolic execution must choose among multiple
    possible paths (e.g., a branching condition).

    Attributes
    ----------
    choice_type : str
        Describes the kind of choice (e.g., "bool", "int", "array_index").
    constraint : Optional[Constraint]
        The constraint associated with this choice point.
    """

    __slots__ = ("choice_type", "constraint")

    def __init__(
        self,
        choice_type: str = "bool",
        constraint: Optional["Constraint"] = None,
        parent: Optional[TreeNode] = None,
        depth: int = 0,
    ) -> None:
        # Call parent's __init__ directly to avoid dataclass/ABC issues
        TreeNode.__init__(self, parent, depth)
        self.choice_type = choice_type
        self.constraint = constraint

    def get_option(self, index: int) -> Optional["ChoiceOption"]:
        """Get the option at the given index."""
        for child in self._children:
            if isinstance(child, ChoiceOption) and child.option_index == index:
                return child
        return None

    def get_open_options(self) -> List["ChoiceOption"]:
        """Get all open options."""
        return [c for c in self._children 
                if isinstance(c, ChoiceOption) and c.state == NodeState.OPEN]

    def accept(self, visitor: TreeNodeVisitor) -> Any:
        return visitor.visit_choice(self)


@dataclass
class ChoiceOption(TreeNode):
    """A single option within a choice point.

    Attributes
    ----------
    option_index : int
        Index of this option within the parent Choice.
    value : Any
        The value selected for this option (e.g., True/False for bool choice).
    option_constraint : Optional[Constraint]
        The path constraint added when this option is taken.
    """

    __slots__ = ("option_index", "value", "option_constraint")

    def __init__(
        self,
        option_index: int = 0,
        value: Any = None,
        option_constraint: Optional["Constraint"] = None,
        parent: Optional[TreeNode] = None,
        depth: int = 0,
    ) -> None:
        TreeNode.__init__(self, parent, depth)
        self.option_index = option_index
        self.value = value
        self.option_constraint = option_constraint

    def accept(self, visitor: TreeNodeVisitor) -> Any:
        return visitor.visit_choice_option(self)


@dataclass
class PathSolutionNode(TreeNode):
    """A leaf node representing a successful execution path.

    Attributes
    ----------
    return_value : Any
        The return value produced by this path.
    path_constraints : tuple
        All constraints along the path to this solution.
    """

    __slots__ = ("return_value", "path_constraints")

    def __init__(
        self,
        return_value: Any = None,
        path_constraints: tuple = (),
        parent: Optional[TreeNode] = None,
        depth: int = 0,
    ) -> None:
        TreeNode.__init__(self, parent, depth)
        self.return_value = return_value
        self.path_constraints = path_constraints
        self._state = NodeState.CLOSED

    def accept(self, visitor: TreeNodeVisitor) -> Any:
        return visitor.visit_path_solution(self)


@dataclass
class Fail(TreeNode):
    """A leaf node representing a failed/infeasible path.

    Attributes
    ----------
    reason : str
        Description of why this path failed.
    exception : Optional[Exception]
        The exception that caused failure, if any.
    """

    __slots__ = ("reason", "exception")

    def __init__(
        self,
        reason: str = "infeasible",
        exception: Optional[Exception] = None,
        parent: Optional[TreeNode] = None,
        depth: int = 0,
    ) -> None:
        TreeNode.__init__(self, parent, depth)
        self.reason = reason
        self.exception = exception
        self._state = NodeState.CLOSED

    def accept(self, visitor: TreeNodeVisitor) -> Any:
        return visitor.visit_fail(self)


@dataclass
class ExceededBudget(TreeNode):
    """A leaf node indicating the budget was exceeded.

    Attributes
    ----------
    budget_type : str
        Which budget was exceeded (e.g., "time", "depth", "paths").
    """

    __slots__ = ("budget_type",)

    def __init__(
        self,
        budget_type: str = "unknown",
        parent: Optional[TreeNode] = None,
        depth: int = 0,
    ) -> None:
        TreeNode.__init__(self, parent, depth)
        self.budget_type = budget_type
        self._state = NodeState.EXCEEDED

    def accept(self, visitor: TreeNodeVisitor) -> Any:
        return visitor.visit_exceeded_budget(self)
