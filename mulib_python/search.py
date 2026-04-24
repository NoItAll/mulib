"""Search tree: ChoiceOption and PathSolution structures."""

from __future__ import annotations

from collections import deque
from typing import Any


class ChoiceOption:
    """One option (true/false branch) at a choice point."""

    __slots__ = ("parent", "z3_constraint", "taken_value", "children", "status",
                 "depth")

    def __init__(self, parent, z3_constraint, taken_value):
        self.parent = parent
        self.z3_constraint = z3_constraint  # z3 BoolRef or None for root
        self.taken_value = taken_value      # bool (which branch this represents)
        self.children: list[ChoiceOption] = []
        self.status = "unknown"             # unknown | sat | unsat | done
        self.depth = (parent.depth + 1) if parent is not None else 0

    def set_children(self, *cs):
        self.children = list(cs)

    def set_sat(self):
        self.status = "sat"

    def set_unsat(self):
        self.status = "unsat"


class PathSolution:
    """Result returned from one fully-executed search path."""

    __slots__ = ("return_value", "labels", "leaf_option")

    def __init__(self, return_value, labels: dict[str, Any], leaf_option: ChoiceOption):
        self.return_value = return_value
        self.labels = labels
        self.leaf_option = leaf_option

    def __repr__(self):
        return f"PathSolution(return={self.return_value!r}, labels={self.labels})"


class SearchTree:
    """DFS search tree.  Maintains a queue of unexplored ChoiceOption nodes."""

    def __init__(self):
        self.root = ChoiceOption(parent=None, z3_constraint=None, taken_value=None)
        self.root.set_sat()
        # LIFO for DFS
        self._stack: deque[ChoiceOption] = deque()

    def queue(self, option: ChoiceOption):
        self._stack.append(option)

    def poll_next(self):
        while self._stack:
            co = self._stack.pop()
            if co.status == "unsat":
                continue
            return co
        return None

    @staticmethod
    def path_to(option: ChoiceOption) -> list[ChoiceOption]:
        out: list[ChoiceOption] = []
        cur = option
        while cur is not None and cur.parent is not None:
            out.append(cur)
            cur = cur.parent
        out.reverse()
        return out
