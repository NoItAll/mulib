"""Base classes for symbolic primitives."""

from __future__ import annotations

import abc
from typing import TYPE_CHECKING

from mulib_python.substitutions.markers import Substituted, Sym

if TYPE_CHECKING:
    pass


class Sprimitive(Substituted, abc.ABC):
    """Marker base for all primitives (symbolic or concrete)."""
    __slots__ = ()


class SymSprimitive(Sprimitive, Sym, abc.ABC):
    """Marker for symbolic primitives."""
    __slots__ = ()


class SymSprimitiveLeaf(SymSprimitive, abc.ABC):
    """Marker for named leaf variables (no sub-expression).
    
    Each leaf has a unique string identifier used by the solver.
    """
    __slots__ = ()

    @property
    @abc.abstractmethod
    def id(self) -> str:
        """Return the unique string name of this leaf variable."""
        ...
