"""Snumber hierarchy - numeric symbolic primitives."""

from __future__ import annotations

import abc
from typing import TYPE_CHECKING

from mulib_python.expressions import Expression
from mulib_python.substitutions.primitives.sprimitive import (
    Sprimitive, SymSprimitive
)
from mulib_python.substitutions.markers import Conc

if TYPE_CHECKING:
    pass


class Snumber(Sprimitive, Expression, abc.ABC):
    """Base for all numeric primitives (both concrete and symbolic).
    
    Snumber is both an Sprimitive and an Expression, allowing numeric
    primitives to be used directly in expression trees.
    """
    __slots__ = ()


class ConcSprimitive(Sprimitive, Conc, abc.ABC):
    """Marker for concrete primitives."""
    __slots__ = ()


class ConcSnumber(Snumber, ConcSprimitive, abc.ABC):
    """Concrete number providing value accessors."""
    __slots__ = ()

    @abc.abstractmethod
    def int_val(self) -> int:
        """Return the value as an int."""
        ...

    @abc.abstractmethod
    def double_val(self) -> float:
        """Return the value as a double (float)."""
        ...

    @abc.abstractmethod
    def float_val(self) -> float:
        """Return the value as a float."""
        ...

    @abc.abstractmethod
    def long_val(self) -> int:
        """Return the value as a long (int)."""
        ...

    @abc.abstractmethod
    def short_val(self) -> int:
        """Return the value as a signed 16-bit int."""
        ...

    @abc.abstractmethod
    def byte_val(self) -> int:
        """Return the value as a signed 8-bit int."""
        ...

    @abc.abstractmethod
    def char_val(self) -> str:
        """Return the value as a single character."""
        ...


class SymSnumber(Snumber, SymSprimitive, abc.ABC):
    """Symbolic number wrapping an Expression."""
    __slots__ = ()

    @property
    @abc.abstractmethod
    def represented_expression(self) -> Expression:
        """Return the wrapped expression."""
        ...


class AbstractSnumber(Snumber, abc.ABC):
    """Shared base for all Snumber implementations.
    
    This provides the foundation for both concrete and symbolic numbers.
    """
    __slots__ = ('_concolic',)  # Optional concolic wrapper

    def __init__(self) -> None:
        # _concolic slot is used by ConcolicCalculationFactory
        object.__setattr__(self, '_concolic', None)


class Sfpnumber(AbstractSnumber, abc.ABC):
    """Floating-point number marker."""
    __slots__ = ()

    @property
    def is_fp(self) -> bool:
        """Return True since this is a floating-point number."""
        return True
