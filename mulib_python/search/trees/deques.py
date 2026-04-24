"""Search tree deques (frontier management)."""

from __future__ import annotations

import abc
import random
from collections import deque
from typing import Deque, List, Optional, TYPE_CHECKING

if TYPE_CHECKING:
    from mulib_python.search.trees.choice import ChoiceOption


class SearchDeque(abc.ABC):
    """Abstract base class for search tree frontier management.

    The deque manages pending choice options to explore.
    Different implementations provide different search strategies.
    """

    @abc.abstractmethod
    def push(self, option: "ChoiceOption") -> None:
        """Add an option to the frontier."""

    @abc.abstractmethod
    def push_all(self, options: List["ChoiceOption"]) -> None:
        """Add multiple options to the frontier."""

    @abc.abstractmethod
    def pop(self) -> Optional["ChoiceOption"]:
        """Remove and return the next option to explore."""

    @abc.abstractmethod
    def peek(self) -> Optional["ChoiceOption"]:
        """Return the next option without removing it."""

    @abc.abstractmethod
    def is_empty(self) -> bool:
        """Return True if no options remain."""

    @abc.abstractmethod
    def __len__(self) -> int:
        """Return the number of pending options."""

    @abc.abstractmethod
    def clear(self) -> None:
        """Remove all pending options."""


class DFSDeque(SearchDeque):
    """Depth-first search deque (LIFO stack).

    Options are explored in LIFO order, giving priority to deeper nodes.
    """

    def __init__(self) -> None:
        self._stack: Deque["ChoiceOption"] = deque()

    def push(self, option: "ChoiceOption") -> None:
        self._stack.append(option)

    def push_all(self, options: List["ChoiceOption"]) -> None:
        for opt in options:
            self._stack.append(opt)

    def pop(self) -> Optional["ChoiceOption"]:
        if self._stack:
            return self._stack.pop()
        return None

    def peek(self) -> Optional["ChoiceOption"]:
        if self._stack:
            return self._stack[-1]
        return None

    def is_empty(self) -> bool:
        return len(self._stack) == 0

    def __len__(self) -> int:
        return len(self._stack)

    def clear(self) -> None:
        self._stack.clear()


class BFSDeque(SearchDeque):
    """Breadth-first search deque (FIFO queue).

    Options are explored in FIFO order, giving priority to shallower nodes.
    """

    def __init__(self) -> None:
        self._queue: Deque["ChoiceOption"] = deque()

    def push(self, option: "ChoiceOption") -> None:
        self._queue.append(option)

    def push_all(self, options: List["ChoiceOption"]) -> None:
        for opt in options:
            self._queue.append(opt)

    def pop(self) -> Optional["ChoiceOption"]:
        if self._queue:
            return self._queue.popleft()
        return None

    def peek(self) -> Optional["ChoiceOption"]:
        if self._queue:
            return self._queue[0]
        return None

    def is_empty(self) -> bool:
        return len(self._queue) == 0

    def __len__(self) -> int:
        return len(self._queue)

    def clear(self) -> None:
        self._queue.clear()


class RandomDeque(SearchDeque):
    """Random search deque.

    Options are explored in random order.
    """

    def __init__(self, seed: Optional[int] = None) -> None:
        self._list: List["ChoiceOption"] = []
        self._rng = random.Random(seed)

    def push(self, option: "ChoiceOption") -> None:
        self._list.append(option)

    def push_all(self, options: List["ChoiceOption"]) -> None:
        self._list.extend(options)

    def pop(self) -> Optional["ChoiceOption"]:
        if self._list:
            idx = self._rng.randrange(len(self._list))
            return self._list.pop(idx)
        return None

    def peek(self) -> Optional["ChoiceOption"]:
        if self._list:
            return self._rng.choice(self._list)
        return None

    def is_empty(self) -> bool:
        return len(self._list) == 0

    def __len__(self) -> int:
        return len(self._list)

    def clear(self) -> None:
        self._list.clear()


class IDDFSDeque(SearchDeque):
    """Iterative deepening depth-first search deque.

    Explores in DFS order but limits depth. When no options remain at
    current depth limit, increases the limit and retries.
    """

    def __init__(self, initial_depth_limit: int = 1) -> None:
        self._dfs_stack: Deque["ChoiceOption"] = deque()
        self._deferred: List["ChoiceOption"] = []
        self._depth_limit = initial_depth_limit

    @property
    def depth_limit(self) -> int:
        return self._depth_limit

    def push(self, option: "ChoiceOption") -> None:
        if option.depth <= self._depth_limit:
            self._dfs_stack.append(option)
        else:
            self._deferred.append(option)

    def push_all(self, options: List["ChoiceOption"]) -> None:
        for opt in options:
            self.push(opt)

    def pop(self) -> Optional["ChoiceOption"]:
        if self._dfs_stack:
            return self._dfs_stack.pop()
        # Keep increasing depth limit until we find something or run out
        while self._deferred:
            self._depth_limit += 1
            moved = False
            new_deferred = []
            for opt in self._deferred:
                if opt.depth <= self._depth_limit:
                    self._dfs_stack.append(opt)
                    moved = True
                else:
                    new_deferred.append(opt)
            self._deferred = new_deferred
            if self._dfs_stack:
                return self._dfs_stack.pop()
        return None

    def peek(self) -> Optional["ChoiceOption"]:
        if self._dfs_stack:
            return self._dfs_stack[-1]
        return None

    def is_empty(self) -> bool:
        return len(self._dfs_stack) == 0 and len(self._deferred) == 0

    def __len__(self) -> int:
        return len(self._dfs_stack) + len(self._deferred)

    def clear(self) -> None:
        self._dfs_stack.clear()
        self._deferred.clear()


def create_deque_for_strategy(strategy: "SearchStrategy") -> SearchDeque:
    """Factory function to create the appropriate deque for a strategy."""
    from mulib_python.search.strategy import SearchStrategy

    if strategy == SearchStrategy.DFS:
        return DFSDeque()
    elif strategy == SearchStrategy.BFS:
        return BFSDeque()
    elif strategy == SearchStrategy.RANDOM:
        return RandomDeque()
    elif strategy in (SearchStrategy.IDDFS, SearchStrategy.IDDFS_W_BFS):
        return IDDFSDeque()
    else:
        return DFSDeque()
