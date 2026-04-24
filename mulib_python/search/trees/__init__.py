"""Search trees package."""

from mulib_python.search.trees.choice import (
    TreeNode,
    Choice,
    ChoiceOption,
    PathSolutionNode,
    Fail,
    ExceededBudget,
    NodeState,
    TreeNodeVisitor,
)
from mulib_python.search.trees.deques import (
    SearchDeque,
    DFSDeque,
    BFSDeque,
    RandomDeque,
    IDDFSDeque,
    create_deque_for_strategy,
)
from mulib_python.search.trees.search_tree import (
    SearchTree,
    TreeStatisticsVisitor,
    collect_statistics,
)

__all__ = [
    # Node types
    "TreeNode",
    "Choice",
    "ChoiceOption",
    "PathSolutionNode",
    "Fail",
    "ExceededBudget",
    "NodeState",
    "TreeNodeVisitor",
    # Deques
    "SearchDeque",
    "DFSDeque",
    "BFSDeque",
    "RandomDeque",
    "IDDFSDeque",
    "create_deque_for_strategy",
    # Search tree
    "SearchTree",
    "TreeStatisticsVisitor",
    "collect_statistics",
]
