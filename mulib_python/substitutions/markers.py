"""Marker abstract base classes for substituted values."""

from __future__ import annotations

import abc


class Substituted(abc.ABC):
    """Marker base class for all substituted values (symbolic or concrete)."""
    __slots__ = ()


class Sym(Substituted, abc.ABC):
    """Marker for symbolic values only."""
    __slots__ = ()


class Conc(Substituted, abc.ABC):
    """Marker for concrete values only."""
    __slots__ = ()
