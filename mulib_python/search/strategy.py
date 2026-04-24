"""Search strategy enumeration."""

from __future__ import annotations

from enum import Enum, auto


class SearchStrategy(Enum):
    """Enumeration of available search strategies.

    Attributes
    ----------
    DFS : Depth-first search
        Explores the tree by always picking the most recent choice point.
        Memory-efficient but may not find shortest paths.
    BFS : Breadth-first search
        Explores the tree level by level.
        Finds shortest paths but uses more memory.
    IDDFS : Iterative deepening depth-first search
        Combines the space efficiency of DFS with the optimal path-finding of BFS.
    RANDOM : Random search
        Randomly selects among available choice points.
        Useful for fuzzing and exploring large search spaces.
    IDDFS_W_BFS : IDDFS with BFS for early choices
        Hybrid strategy that uses BFS at the top of the tree and switches to
        IDDFS deeper down.
    """

    DFS = auto()
    BFS = auto()
    IDDFS = auto()
    RANDOM = auto()
    IDDFS_W_BFS = auto()

    def is_depth_first(self) -> bool:
        """Return True if this strategy uses depth-first exploration."""
        return self in (SearchStrategy.DFS, SearchStrategy.IDDFS, SearchStrategy.IDDFS_W_BFS)

    def is_breadth_first(self) -> bool:
        """Return True if this strategy uses breadth-first exploration."""
        return self in (SearchStrategy.BFS, SearchStrategy.IDDFS_W_BFS)
