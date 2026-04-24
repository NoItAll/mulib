"""Calculation factory for creating symbolic values."""

from __future__ import annotations

import abc
from typing import Any, Dict, Optional, TYPE_CHECKING

if TYPE_CHECKING:
    from mulib_python.executor.symbolic_execution import SymbolicExecution


class CalculationFactory(abc.ABC):
    """Abstract factory for creating symbolic/concolic values.

    Provides methods to create symbolic primitives and track them
    for later labeling.
    """

    @abc.abstractmethod
    def create_symbolic_int(self, name: Optional[str] = None) -> "Sint":
        """Create a symbolic integer."""

    @abc.abstractmethod
    def create_symbolic_long(self, name: Optional[str] = None) -> "Slong":
        """Create a symbolic long."""

    @abc.abstractmethod
    def create_symbolic_double(self, name: Optional[str] = None) -> "Sdouble":
        """Create a symbolic double."""

    @abc.abstractmethod
    def create_symbolic_float(self, name: Optional[str] = None) -> "Sfloat":
        """Create a symbolic float."""

    @abc.abstractmethod
    def create_symbolic_bool(self, name: Optional[str] = None) -> "Sbool":
        """Create a symbolic boolean."""

    @abc.abstractmethod
    def create_symbolic_byte(self, name: Optional[str] = None) -> "Sbyte":
        """Create a symbolic byte."""

    @abc.abstractmethod
    def create_symbolic_char(self, name: Optional[str] = None) -> "Schar":
        """Create a symbolic character."""

    @abc.abstractmethod
    def create_symbolic_short(self, name: Optional[str] = None) -> "Sshort":
        """Create a symbolic short."""


class SymbolicCalculationFactory(CalculationFactory):
    """Factory that creates symbolic values.

    Each created value is a fresh symbolic variable.
    """

    def __init__(self, se: Optional["SymbolicExecution"] = None) -> None:
        self._se = se
        self._counter = 0

    def _next_id(self, prefix: str, name: Optional[str] = None) -> str:
        if name:
            return name
        self._counter += 1
        return f"{prefix}_{self._counter}"

    def create_symbolic_int(self, name: Optional[str] = None) -> "Sint":
        from mulib_python.substitutions.primitives.sint import SymSintLeaf
        return SymSintLeaf(self._next_id("i", name))

    def create_symbolic_long(self, name: Optional[str] = None) -> "Slong":
        from mulib_python.substitutions.primitives.slong import SymSlongLeaf
        return SymSlongLeaf(self._next_id("l", name))

    def create_symbolic_double(self, name: Optional[str] = None) -> "Sdouble":
        from mulib_python.substitutions.primitives.sdouble import SymSdoubleLeaf
        return SymSdoubleLeaf(self._next_id("d", name))

    def create_symbolic_float(self, name: Optional[str] = None) -> "Sfloat":
        from mulib_python.substitutions.primitives.sfloat import SymSfloatLeaf
        return SymSfloatLeaf(self._next_id("f", name))

    def create_symbolic_bool(self, name: Optional[str] = None) -> "Sbool":
        from mulib_python.substitutions.primitives.sint import SymSboolLeaf
        return SymSboolLeaf(self._next_id("b", name))

    def create_symbolic_byte(self, name: Optional[str] = None) -> "Sbyte":
        from mulib_python.substitutions.primitives.sint import SymSbyteLeaf
        return SymSbyteLeaf(self._next_id("byte", name))

    def create_symbolic_char(self, name: Optional[str] = None) -> "Schar":
        from mulib_python.substitutions.primitives.sint import SymScharLeaf
        return SymScharLeaf(self._next_id("c", name))

    def create_symbolic_short(self, name: Optional[str] = None) -> "Sshort":
        from mulib_python.substitutions.primitives.sint import SymSshortLeaf
        return SymSshortLeaf(self._next_id("s", name))


class ConcolicCalculationFactory(CalculationFactory):
    """Factory for concolic execution.

    Creates symbolic values that are paired with concrete values for
    concolic execution. The concrete values guide execution while
    symbolic constraints are collected.
    """

    def __init__(
        self,
        se: Optional["SymbolicExecution"] = None,
        concrete_inputs: Optional[Dict[str, Any]] = None,
    ) -> None:
        self._se = se
        self._concrete_inputs = concrete_inputs or {}
        self._counter = 0

    def _next_id(self, prefix: str, name: Optional[str] = None) -> str:
        if name:
            return name
        self._counter += 1
        return f"{prefix}_{self._counter}"

    def _get_concrete(self, name: str, default: Any) -> Any:
        return self._concrete_inputs.get(name, default)

    def create_symbolic_int(self, name: Optional[str] = None) -> "Sint":
        from mulib_python.substitutions.primitives.sint import SymSintLeaf
        var_id = self._next_id("i", name)
        # In concolic mode, we could wrap with concrete value
        # For now, just return symbolic
        return SymSintLeaf(var_id)

    def create_symbolic_long(self, name: Optional[str] = None) -> "Slong":
        from mulib_python.substitutions.primitives.slong import SymSlongLeaf
        return SymSlongLeaf(self._next_id("l", name))

    def create_symbolic_double(self, name: Optional[str] = None) -> "Sdouble":
        from mulib_python.substitutions.primitives.sdouble import SymSdoubleLeaf
        return SymSdoubleLeaf(self._next_id("d", name))

    def create_symbolic_float(self, name: Optional[str] = None) -> "Sfloat":
        from mulib_python.substitutions.primitives.sfloat import SymSfloatLeaf
        return SymSfloatLeaf(self._next_id("f", name))

    def create_symbolic_bool(self, name: Optional[str] = None) -> "Sbool":
        from mulib_python.substitutions.primitives.sint import SymSboolLeaf
        return SymSboolLeaf(self._next_id("b", name))

    def create_symbolic_byte(self, name: Optional[str] = None) -> "Sbyte":
        from mulib_python.substitutions.primitives.sint import SymSbyteLeaf
        return SymSbyteLeaf(self._next_id("byte", name))

    def create_symbolic_char(self, name: Optional[str] = None) -> "Schar":
        from mulib_python.substitutions.primitives.sint import SymScharLeaf
        return SymScharLeaf(self._next_id("c", name))

    def create_symbolic_short(self, name: Optional[str] = None) -> "Sshort":
        from mulib_python.substitutions.primitives.sint import SymSshortLeaf
        return SymSshortLeaf(self._next_id("s", name))
