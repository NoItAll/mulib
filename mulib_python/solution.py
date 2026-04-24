"""Solution and Labels data classes."""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any, Dict, Collection


class Labels:
    """Immutable container mapping symbolic-variable names to their concrete labels.

    The key ``"return"`` always holds the return value of the search-region
    execution.
    """

    __slots__ = ("_id_to_var", "_id_to_label")

    def __init__(
        self,
        id_to_var: Dict[str, Any],
        id_to_label: Dict[str, Any],
    ) -> None:
        self._id_to_var = dict(id_to_var) if id_to_var else {}
        self._id_to_label = dict(id_to_label) if id_to_label else {}

    def get_label_for_id(self, name: str) -> Any:
        """Return the concrete label for *name*. Returns ``None`` if absent."""
        return self._id_to_label.get(name)

    def get_named_var(self, name: str) -> Any:
        """Return the search-region representation for *name*."""
        return self._id_to_var.get(name)

    @property
    def id_to_named_var(self) -> Dict[str, Any]:
        return dict(self._id_to_var)

    @property
    def id_to_label(self) -> Dict[str, Any]:
        return dict(self._id_to_label)

    @property
    def names(self) -> Collection[str]:
        return self._id_to_label.keys()

    def __repr__(self) -> str:
        return f"Labels({self._id_to_label!r})"

    def __eq__(self, other: object) -> bool:
        if isinstance(other, dict):
            return self._id_to_label == other
        if not isinstance(other, Labels):
            return NotImplemented
        return self._id_to_label == other._id_to_label

    def __getitem__(self, key: str) -> Any:
        """Allow dict-like access: labels['x']."""
        return self._id_to_label[key]

    def __contains__(self, key: str) -> bool:
        """Allow 'in' checks: 'x' in labels."""
        return key in self._id_to_label

    def __hash__(self) -> int:
        return hash(tuple(sorted(self._id_to_label.items())))


@dataclass(frozen=True)
class Solution:
    """A single solution to the search region.

    Attributes
    ----------
    return_value : Any
        The concrete return value produced by the search region.
    labels : Labels
        All remembered variables and their concrete values, including
        ``"return"``.
    """
    return_value: Any
    labels: Labels

    def __repr__(self) -> str:
        return f"Solution(return_value={self.return_value!r}, labels={self.labels!r})"


@dataclass(frozen=True)
class PathSolution(Solution):
    """A solution enriched with search-tree path metadata.

    Extra Attributes
    ----------------
    path_constraints:
        The tuple of Constraint objects that were active when this leaf was reached.
    depth:
        Depth of the leaf node in the search tree.
    exceeded_budget:
        True if execution was cut off by a budget limit.
    threw_exception:
        If SEARCH_ALLOW_EXCEPTIONS is True and the path threw, the exception
        is stored here (otherwise None).
    """
    path_constraints: tuple = field(default_factory=tuple)
    depth: int = 0
    exceeded_budget: bool = False
    threw_exception: Exception | None = None
