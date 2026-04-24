"""Sdouble - symbolic double-precision floating point."""

from __future__ import annotations

import abc
import itertools
import math
from typing import TYPE_CHECKING, Any

from mulib_python.expressions import Expression, Sum, Sub, Mul, Div, Mod, Neg
from mulib_python.constraints import Lt, Lte, Eq
from mulib_python.substitutions.primitives.sprimitive import SymSprimitiveLeaf
from mulib_python.substitutions.primitives.snumber import (
    Sfpnumber, ConcSnumber, SymSnumber
)

if TYPE_CHECKING:
    from mulib_python.substitutions.primitives.sint import Sint, Sbool, ConcSbool
    from mulib_python.substitutions.primitives.slong import Slong
    from mulib_python.substitutions.primitives.sfloat import Sfloat


class Sdouble(Sfpnumber, abc.ABC):
    """Symbolic double-precision floating point."""

    __slots__ = ()

    @property
    def is_fp(self) -> bool:
        return True

    # --- Factory methods ---
    
    @staticmethod
    def conc_sdouble(d: float) -> "ConcSdouble":
        """Create a concrete Sdouble."""
        return ConcSdouble(d)

    @staticmethod
    def new_input_symbolic_sdouble(explicit_id: int | None = None) -> "SymSdoubleLeaf":
        """Create a new symbolic Sdouble leaf."""
        return SymSdoubleLeaf(explicit_id)

    @staticmethod
    def new_expression_symbolic_sdouble(expr: Expression) -> "SymSdouble":
        """Create a symbolic Sdouble wrapping an expression."""
        return SymSdouble(expr)

    # --- Helper for coercion ---
    
    def _coerce_sdouble(self, other: Any) -> "Sdouble":
        if isinstance(other, Sdouble):
            return other
        if isinstance(other, (int, float)):
            return Sdouble.conc_sdouble(float(other))
        raise TypeError(f"Cannot coerce {type(other).__name__} to Sdouble")

    def _dispatch_binary_double(
        self, 
        other: Any,
        conc_op,
        expr_builder,
    ) -> "Sdouble":
        try:
            other_sdouble = self._coerce_sdouble(other)
        except TypeError:
            return NotImplemented
        
        if isinstance(self, ConcSdouble) and isinstance(other_sdouble, ConcSdouble):
            return Sdouble.conc_sdouble(conc_op(self._value, other_sdouble._value))
        
        return Sdouble.new_expression_symbolic_sdouble(expr_builder(self, other_sdouble))

    def _dispatch_comparison(
        self,
        other: Any,
        conc_op,
        constraint_builder,
    ) -> "Sbool":
        from mulib_python.substitutions.primitives.sint import Sbool, ConcSbool
        try:
            other_sdouble = self._coerce_sdouble(other)
        except TypeError:
            return NotImplemented
        
        if isinstance(self, ConcSdouble) and isinstance(other_sdouble, ConcSdouble):
            return ConcSbool.TRUE if conc_op(self._value, other_sdouble._value) else ConcSbool.FALSE
        
        return Sbool.new_constraint_sbool(constraint_builder(self, other_sdouble))

    # --- Arithmetic operators ---
    
    def __add__(self, other) -> "Sdouble":
        return self._dispatch_binary_double(
            other,
            lambda a, b: a + b,
            lambda l, r: Sum(l, r)
        )

    def __radd__(self, other) -> "Sdouble":
        return self._coerce_sdouble(other).__add__(self)

    def __sub__(self, other) -> "Sdouble":
        return self._dispatch_binary_double(
            other,
            lambda a, b: a - b,
            lambda l, r: Sub(l, r)
        )

    def __rsub__(self, other) -> "Sdouble":
        return self._coerce_sdouble(other).__sub__(self)

    def __mul__(self, other) -> "Sdouble":
        return self._dispatch_binary_double(
            other,
            lambda a, b: a * b,
            lambda l, r: Mul(l, r)
        )

    def __rmul__(self, other) -> "Sdouble":
        return self._coerce_sdouble(other).__mul__(self)

    def __truediv__(self, other) -> "Sdouble":
        return self._dispatch_binary_double(
            other,
            lambda a, b: a / b if b != 0 else (float('inf') if a >= 0 else float('-inf')),
            lambda l, r: Div(l, r)
        )

    def __rtruediv__(self, other) -> "Sdouble":
        return self._coerce_sdouble(other).__truediv__(self)

    __floordiv__ = __truediv__
    __rfloordiv__ = __rtruediv__

    def __mod__(self, other) -> "Sdouble":
        return self._dispatch_binary_double(
            other,
            lambda a, b: math.fmod(a, b) if b != 0 else float('nan'),
            lambda l, r: Mod(l, r)
        )

    def __rmod__(self, other) -> "Sdouble":
        return self._coerce_sdouble(other).__mod__(self)

    def __neg__(self) -> "Sdouble":
        if isinstance(self, ConcSdouble):
            return Sdouble.conc_sdouble(-self._value)
        return Sdouble.new_expression_symbolic_sdouble(Neg(self))

    def __pos__(self) -> "Sdouble":
        return self

    # --- Comparison operators ---
    
    def __lt__(self, other) -> "Sbool":
        return self._dispatch_comparison(
            other,
            lambda a, b: a < b,
            lambda l, r: Lt(l, r)
        )

    def __le__(self, other) -> "Sbool":
        return self._dispatch_comparison(
            other,
            lambda a, b: a <= b,
            lambda l, r: Lte(l, r)
        )

    def __gt__(self, other) -> "Sbool":
        return self._dispatch_comparison(
            other,
            lambda a, b: a > b,
            lambda l, r: Lt(r, l)
        )

    def __ge__(self, other) -> "Sbool":
        return self._dispatch_comparison(
            other,
            lambda a, b: a >= b,
            lambda l, r: Lte(r, l)
        )

    def __eq__(self, other) -> "Sbool":  # type: ignore[override]
        return self._dispatch_comparison(
            other,
            lambda a, b: a == b,
            lambda l, r: Eq(l, r)
        )

    def __ne__(self, other) -> "Sbool":  # type: ignore[override]
        result = self.__eq__(other)
        if result is NotImplemented:
            return result
        return result.bool_not()

    def __hash__(self) -> int:
        raise NotImplementedError("Subclass must implement __hash__")

    # --- Compare-and-return-int (DCMP) ---
    
    def cmp(self, rhs: "Sdouble") -> "Sint":
        """Three-way comparison returning Sint (-1, 0, or 1)."""
        from mulib_python.substitutions.primitives.sint import Sint, ConcSint
        
        if isinstance(self, ConcSdouble) and isinstance(rhs, ConcSdouble):
            a, b = self._value, rhs._value
            if math.isnan(a) or math.isnan(b):
                return ConcSint.ONE  # Java: NaN comparison returns 1 (or -1 for dcmpl)
            if a < b:
                return ConcSint.MINUS_ONE
            elif a > b:
                return ConcSint.ONE
            else:
                return ConcSint.ZERO
        
        from mulib_python.expressions import ExpressionIte
        return Sint.new_expression_symbolic_sint(
            ExpressionIte(
                Lt(self, rhs),
                ConcSint.MINUS_ONE,
                ExpressionIte(
                    Eq(self, rhs),
                    ConcSint.ZERO,
                    ConcSint.ONE
                )
            )
        )

    # --- Type conversion ---
    
    def to_sfloat(self) -> "Sfloat":
        """Convert to Sfloat (d2f)."""
        from mulib_python.substitutions.primitives.sfloat import Sfloat, ConcSfloat
        if isinstance(self, ConcSdouble):
            return ConcSfloat(float(self._value))
        return Sfloat.new_expression_symbolic_sfloat(self)

    def to_slong(self) -> "Slong":
        """Convert to Slong (d2l)."""
        from mulib_python.substitutions.primitives.slong import Slong, ConcSlong
        if isinstance(self, ConcSdouble):
            val = self._value
            if math.isnan(val):
                return ConcSlong(0)
            if val >= 2**63 - 1:
                return ConcSlong(2**63 - 1)
            if val <= -2**63:
                return ConcSlong(-2**63)
            return ConcSlong(int(val))
        return Slong.new_expression_symbolic_slong(self)

    def to_sint(self) -> "Sint":
        """Convert to Sint (d2i)."""
        from mulib_python.substitutions.primitives.sint import Sint, ConcSint
        if isinstance(self, ConcSdouble):
            val = self._value
            if math.isnan(val):
                return ConcSint(0)
            if val >= 2**31 - 1:
                return ConcSint(2**31 - 1)
            if val <= -2**31:
                return ConcSint(-2**31)
            return ConcSint(int(val))
        return Sint.new_expression_symbolic_sint(self)

    # --- Choice methods ---
    
    def lt_choice(self, rhs: "Sdouble | None" = None) -> bool:
        if rhs is None:
            rhs = Sdouble.conc_sdouble(0.0)
        return bool(self.__lt__(rhs))

    def lte_choice(self, rhs: "Sdouble | None" = None) -> bool:
        if rhs is None:
            rhs = Sdouble.conc_sdouble(0.0)
        return bool(self.__le__(rhs))

    def eq_choice(self, rhs: "Sdouble | None" = None) -> bool:
        if rhs is None:
            rhs = Sdouble.conc_sdouble(0.0)
        return bool(self.__eq__(rhs))

    def not_eq_choice(self, rhs: "Sdouble | None" = None) -> bool:
        if rhs is None:
            rhs = Sdouble.conc_sdouble(0.0)
        return bool(self.__ne__(rhs))

    def gt_choice(self, rhs: "Sdouble | None" = None) -> bool:
        if rhs is None:
            rhs = Sdouble.conc_sdouble(0.0)
        return bool(self.__gt__(rhs))

    def gte_choice(self, rhs: "Sdouble | None" = None) -> bool:
        if rhs is None:
            rhs = Sdouble.conc_sdouble(0.0)
        return bool(self.__ge__(rhs))


class ConcSdouble(Sdouble, ConcSnumber):
    """Concrete double-precision floating point."""

    __slots__ = ("_value", "_hash")

    def __init__(self, value: float) -> None:
        object.__setattr__(self, "_value", float(value))
        object.__setattr__(self, "_hash", hash(float(value)))
        object.__setattr__(self, "_concolic", None)

    def __setattr__(self, name: str, value: object) -> None:
        raise AttributeError("ConcSdouble is immutable")

    @property
    def value(self) -> float:
        return self._value

    @property
    def is_fp(self) -> bool:
        return True

    def int_val(self) -> int:
        val = self._value
        if math.isnan(val):
            return 0
        if val >= 2**31 - 1:
            return 2**31 - 1
        if val <= -2**31:
            return -2**31
        return int(val)

    def double_val(self) -> float:
        return self._value

    def float_val(self) -> float:
        return float(self._value)

    def long_val(self) -> int:
        val = self._value
        if math.isnan(val):
            return 0
        if val >= 2**63 - 1:
            return 2**63 - 1
        if val <= -2**63:
            return -2**63
        return int(val)

    def short_val(self) -> int:
        return ((self.int_val() + 32768) % 65536) - 32768

    def byte_val(self) -> int:
        return ((self.int_val() + 128) % 256) - 128

    def char_val(self) -> str:
        return chr(self.int_val() & 0xFFFF)

    def __repr__(self) -> str:
        return f"{self._value}d"

    def __hash__(self) -> int:
        return self._hash

    def __eq__(self, other) -> "Sbool":  # type: ignore[override]
        from mulib_python.substitutions.primitives.sint import Sbool, ConcSbool
        if isinstance(other, ConcSdouble):
            return ConcSbool.TRUE if self._value == other._value else ConcSbool.FALSE
        if isinstance(other, (int, float)):
            return ConcSbool.TRUE if self._value == float(other) else ConcSbool.FALSE
        if isinstance(other, Sdouble):
            return Sbool.new_constraint_sbool(Eq(self, other))
        return NotImplemented


# Convenience constants
ConcSdouble.ZERO = ConcSdouble(0.0)
ConcSdouble.ONE = ConcSdouble(1.0)
ConcSdouble.MINUS_ONE = ConcSdouble(-1.0)


class SymSdouble(Sdouble, SymSnumber):
    """Symbolic double wrapping an expression."""

    __slots__ = ("_represented_expression", "_hash")

    def __init__(self, represented_expression: Expression) -> None:
        object.__setattr__(self, "_represented_expression", represented_expression)
        object.__setattr__(self, "_hash", hash(represented_expression))
        object.__setattr__(self, "_concolic", None)

    def __setattr__(self, name: str, value: object) -> None:
        raise AttributeError("SymSdouble is immutable")

    @property
    def represented_expression(self) -> Expression:
        return self._represented_expression

    @property
    def is_fp(self) -> bool:
        return True

    def __repr__(self) -> str:
        return f"SymSdouble{{{self._represented_expression!r}}}"

    def __hash__(self) -> int:
        return self._hash


class SymSdoubleLeaf(SymSdouble, SymSprimitiveLeaf):
    """Named symbolic double leaf."""

    __slots__ = ("_id",)
    _next_id = itertools.count(1)

    def __init__(self, explicit_id: int | str | None = None) -> None:
        if isinstance(explicit_id, str):
            # Accept string ID directly
            id_str = explicit_id
        elif explicit_id is not None:
            # Use explicit numeric ID
            id_str = f"Sdouble{explicit_id}"
        else:
            # Generate new numeric ID
            n = next(SymSdoubleLeaf._next_id)
            id_str = f"Sdouble{n}"
        object.__setattr__(self, "_id", id_str)
        object.__setattr__(self, "_represented_expression", self)
        object.__setattr__(self, "_hash", hash(id_str))
        object.__setattr__(self, "_concolic", None)

    @property
    def id(self) -> str:
        return self._id

    def __repr__(self) -> str:
        return self._id

    def __hash__(self) -> int:
        return self._hash
