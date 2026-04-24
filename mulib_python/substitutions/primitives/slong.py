"""Slong - symbolic long integer (64-bit)."""

from __future__ import annotations

import abc
import itertools
from typing import TYPE_CHECKING, Any

from mulib_python.expressions import (
    Expression, Sum, Sub, Mul, Div, Mod, Neg,
    BitwiseAnd, BitwiseOr, BitwiseXor,
    ShiftLeft, ShiftRight, LogicalShiftRight,
)
from mulib_python.constraints import Lt, Lte, Eq
from mulib_python.substitutions.primitives.sprimitive import SymSprimitiveLeaf
from mulib_python.substitutions.primitives.snumber import (
    AbstractSnumber, ConcSnumber, SymSnumber
)

# Sentinel used by interning-guarded ``__init__`` to detect already-initialised
# cached instances.
_UNSET: object = object()

if TYPE_CHECKING:
    from mulib_python.substitutions.primitives.sint import Sint, Sbool, ConcSbool
    from mulib_python.substitutions.primitives.sdouble import Sdouble
    from mulib_python.substitutions.primitives.sfloat import Sfloat


def _to_signed_long(val: int) -> int:
    """Truncate to signed 64-bit."""
    return ((val + 2**63) % 2**64) - 2**63


class Slong(AbstractSnumber, abc.ABC):
    """Symbolic long integer (64-bit)."""

    __slots__ = ()

    @property
    def is_fp(self) -> bool:
        return False

    # --- Factory methods ---
    
    @staticmethod
    def conc_slong(l: int) -> "ConcSlong":
        """Create a concrete Slong."""
        return ConcSlong(l)

    @staticmethod
    def new_input_symbolic_slong(explicit_id: int | None = None) -> "SymSlongLeaf":
        """Create a new symbolic Slong leaf."""
        return SymSlongLeaf(explicit_id)

    @staticmethod
    def new_expression_symbolic_slong(expr: Expression) -> "SymSlong":
        """Create a symbolic Slong wrapping an expression."""
        return SymSlong(expr)

    # --- Helper for coercion ---
    
    def _coerce_slong(self, other: Any) -> "Slong":
        if isinstance(other, Slong):
            return other
        if isinstance(other, int):
            return Slong.conc_slong(other)
        raise TypeError(f"Cannot coerce {type(other).__name__} to Slong")

    def _dispatch_binary_long(
        self, 
        other: Any,
        conc_op,
        expr_builder,
    ) -> "Slong":
        try:
            other_slong = self._coerce_slong(other)
        except TypeError:
            return NotImplemented
        
        if isinstance(self, ConcSlong) and isinstance(other_slong, ConcSlong):
            return Slong.conc_slong(conc_op(self._value, other_slong._value))
        
        return Slong.new_expression_symbolic_slong(expr_builder(self, other_slong))

    def _dispatch_comparison(
        self,
        other: Any,
        conc_op,
        constraint_builder,
    ) -> "Sbool":
        from mulib_python.substitutions.primitives.sint import Sbool, ConcSbool
        try:
            other_slong = self._coerce_slong(other)
        except TypeError:
            return NotImplemented
        
        if isinstance(self, ConcSlong) and isinstance(other_slong, ConcSlong):
            return ConcSbool.TRUE if conc_op(self._value, other_slong._value) else ConcSbool.FALSE
        
        return Sbool.new_constraint_sbool(constraint_builder(self, other_slong))

    # --- Arithmetic operators ---
    
    def __add__(self, other) -> "Slong":
        return self._dispatch_binary_long(
            other,
            lambda a, b: _to_signed_long(a + b),
            lambda l, r: Sum(l, r)
        )

    def __radd__(self, other) -> "Slong":
        return self._coerce_slong(other).__add__(self)

    def __sub__(self, other) -> "Slong":
        return self._dispatch_binary_long(
            other,
            lambda a, b: _to_signed_long(a - b),
            lambda l, r: Sub(l, r)
        )

    def __rsub__(self, other) -> "Slong":
        return self._coerce_slong(other).__sub__(self)

    def __mul__(self, other) -> "Slong":
        return self._dispatch_binary_long(
            other,
            lambda a, b: _to_signed_long(a * b),
            lambda l, r: Mul(l, r)
        )

    def __rmul__(self, other) -> "Slong":
        return self._coerce_slong(other).__mul__(self)

    def __truediv__(self, other) -> "Slong":
        def java_div(a: int, b: int) -> int:
            if b == 0:
                raise ZeroDivisionError("long division by zero")
            q = abs(a) // abs(b)
            if (a < 0) ^ (b < 0):
                q = -q
            return _to_signed_long(q)
        
        return self._dispatch_binary_long(
            other,
            java_div,
            lambda l, r: Div(l, r)
        )

    def __rtruediv__(self, other) -> "Slong":
        return self._coerce_slong(other).__truediv__(self)

    __floordiv__ = __truediv__
    __rfloordiv__ = __rtruediv__

    def __mod__(self, other) -> "Slong":
        def java_mod(a: int, b: int) -> int:
            if b == 0:
                raise ZeroDivisionError("long modulo by zero")
            return _to_signed_long(a % b)
        
        return self._dispatch_binary_long(
            other,
            java_mod,
            lambda l, r: Mod(l, r)
        )

    def __rmod__(self, other) -> "Slong":
        return self._coerce_slong(other).__mod__(self)

    def __neg__(self) -> "Slong":
        if isinstance(self, ConcSlong):
            return Slong.conc_slong(_to_signed_long(-self._value))
        return Slong.new_expression_symbolic_slong(Neg(self))

    def __pos__(self) -> "Slong":
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

    # --- Bitwise operators ---
    
    def __and__(self, other) -> "Slong":
        return self._dispatch_binary_long(
            other,
            lambda a, b: a & b,
            lambda l, r: BitwiseAnd(l, r)
        )

    def __rand__(self, other) -> "Slong":
        return self.__and__(other)

    def __or__(self, other) -> "Slong":
        return self._dispatch_binary_long(
            other,
            lambda a, b: a | b,
            lambda l, r: BitwiseOr(l, r)
        )

    def __ror__(self, other) -> "Slong":
        return self.__or__(other)

    def __xor__(self, other) -> "Slong":
        return self._dispatch_binary_long(
            other,
            lambda a, b: a ^ b,
            lambda l, r: BitwiseXor(l, r)
        )

    def __rxor__(self, other) -> "Slong":
        return self.__xor__(other)

    def __invert__(self) -> "Slong":
        return -(self + Slong.conc_slong(1))

    def __lshift__(self, other) -> "Slong":
        """Left shift with Sint amount."""
        from mulib_python.substitutions.primitives.sint import Sint, ConcSint
        if isinstance(other, int):
            other = ConcSint(other)
        if not isinstance(other, Sint):
            return NotImplemented
            
        if isinstance(self, ConcSlong) and isinstance(other, ConcSint):
            return Slong.conc_slong(_to_signed_long(self._value << (other._value & 0x3F)))
        return Slong.new_expression_symbolic_slong(ShiftLeft(self, other))

    def __rlshift__(self, other) -> "Slong":
        return self._coerce_slong(other).__lshift__(self)

    def __rshift__(self, other) -> "Slong":
        """Arithmetic right shift."""
        from mulib_python.substitutions.primitives.sint import Sint, ConcSint
        if isinstance(other, int):
            other = ConcSint(other)
        if not isinstance(other, Sint):
            return NotImplemented
            
        if isinstance(self, ConcSlong) and isinstance(other, ConcSint):
            return Slong.conc_slong(self._value >> (other._value & 0x3F))
        return Slong.new_expression_symbolic_slong(ShiftRight(self, other))

    def __rrshift__(self, other) -> "Slong":
        return self._coerce_slong(other).__rshift__(self)

    def unsigned_rshift(self, other: "Sint") -> "Slong":
        """Logical (unsigned) right shift."""
        from mulib_python.substitutions.primitives.sint import Sint, ConcSint
        if isinstance(other, int):
            other = ConcSint(other)
        if not isinstance(other, Sint):
            raise TypeError(f"Expected Sint, got {type(other)}")
            
        if isinstance(self, ConcSlong) and isinstance(other, ConcSint):
            shift = other._value & 0x3F
            val = self._value
            if val >= 0:
                return Slong.conc_slong(val >> shift)
            unsigned = val & 0xFFFFFFFFFFFFFFFF
            return Slong.conc_slong(unsigned >> shift)
        return Slong.new_expression_symbolic_slong(LogicalShiftRight(self, other))

    # --- Compare-and-return-int (LCMP) ---
    
    def cmp(self, rhs: "Slong") -> "Sint":
        """Three-way comparison returning Sint (-1, 0, or 1)."""
        from mulib_python.substitutions.primitives.sint import Sint, ConcSint
        
        if isinstance(self, ConcSlong) and isinstance(rhs, ConcSlong):
            if self._value < rhs._value:
                return ConcSint.MINUS_ONE
            elif self._value > rhs._value:
                return ConcSint.ONE
            else:
                return ConcSint.ZERO
        
        # Symbolic: need to build conditional expression
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
    
    def to_sint(self) -> "Sint":
        """Convert to Sint (l2i)."""
        from mulib_python.substitutions.primitives.sint import Sint, ConcSint
        if isinstance(self, ConcSlong):
            # Truncate to 32-bit signed
            val = self._value
            val = ((val + 2**31) % 2**32) - 2**31
            return ConcSint(int(val))
        return Sint.new_expression_symbolic_sint(self)

    def to_sdouble(self) -> "Sdouble":
        """Convert to Sdouble (l2d)."""
        from mulib_python.substitutions.primitives.sdouble import Sdouble, ConcSdouble
        if isinstance(self, ConcSlong):
            return ConcSdouble(float(self._value))
        return Sdouble.new_expression_symbolic_sdouble(self)

    def to_sfloat(self) -> "Sfloat":
        """Convert to Sfloat (l2f)."""
        from mulib_python.substitutions.primitives.sfloat import Sfloat, ConcSfloat
        if isinstance(self, ConcSlong):
            return ConcSfloat(float(self._value))
        return Sfloat.new_expression_symbolic_sfloat(self)

    # --- Choice methods ---
    
    def lt_choice(self, rhs: "Slong | None" = None) -> bool:
        if rhs is None:
            rhs = Slong.conc_slong(0)
        return bool(self.__lt__(rhs))

    def lte_choice(self, rhs: "Slong | None" = None) -> bool:
        if rhs is None:
            rhs = Slong.conc_slong(0)
        return bool(self.__le__(rhs))

    def eq_choice(self, rhs: "Slong | None" = None) -> bool:
        if rhs is None:
            rhs = Slong.conc_slong(0)
        return bool(self.__eq__(rhs))

    def not_eq_choice(self, rhs: "Slong | None" = None) -> bool:
        if rhs is None:
            rhs = Slong.conc_slong(0)
        return bool(self.__ne__(rhs))

    def gt_choice(self, rhs: "Slong | None" = None) -> bool:
        if rhs is None:
            rhs = Slong.conc_slong(0)
        return bool(self.__gt__(rhs))

    def gte_choice(self, rhs: "Slong | None" = None) -> bool:
        if rhs is None:
            rhs = Slong.conc_slong(0)
        return bool(self.__ge__(rhs))


class ConcSlong(Slong, ConcSnumber):
    """Concrete long integer."""

    __slots__ = ("_value", "_hash")

    _LOW_CACHE = -128
    _HIGH_CACHE = 127
    _CACHE: tuple["ConcSlong", ...] = ()

    def __new__(cls, value=_UNSET):
        if cls is ConcSlong and ConcSlong._CACHE and value is not _UNSET:
            try:
                v = _to_signed_long(value)
            except TypeError:
                return object.__new__(cls)
            if ConcSlong._LOW_CACHE <= v <= ConcSlong._HIGH_CACHE:
                return ConcSlong._CACHE[v - ConcSlong._LOW_CACHE]
        return object.__new__(cls)

    def __init__(self, value: int) -> None:
        value = _to_signed_long(value)
        if getattr(self, "_value", _UNSET) == value:
            return
        object.__setattr__(self, "_value", value)
        object.__setattr__(self, "_hash", hash(value))
        object.__setattr__(self, "_concolic", None)

    def __setattr__(self, name: str, value: object) -> None:
        raise AttributeError("ConcSlong is immutable")

    @property
    def value(self) -> int:
        return self._value

    @property
    def is_fp(self) -> bool:
        return False

    def int_val(self) -> int:
        # Truncate to 32-bit signed
        return ((self._value + 2**31) % 2**32) - 2**31

    def double_val(self) -> float:
        return float(self._value)

    def float_val(self) -> float:
        return float(self._value)

    def long_val(self) -> int:
        return self._value

    def short_val(self) -> int:
        return ((self._value + 32768) % 65536) - 32768

    def byte_val(self) -> int:
        return ((self._value + 128) % 256) - 128

    def char_val(self) -> str:
        return chr(self._value & 0xFFFF)

    def __repr__(self) -> str:
        return f"{self._value}L"

    def __hash__(self) -> int:
        return self._hash

    def __eq__(self, other) -> "Sbool":  # type: ignore[override]
        from mulib_python.substitutions.primitives.sint import Sbool, ConcSbool
        if isinstance(other, ConcSlong):
            return ConcSbool.TRUE if self._value == other._value else ConcSbool.FALSE
        if isinstance(other, int):
            return ConcSbool.TRUE if self._value == other else ConcSbool.FALSE
        if isinstance(other, Slong):
            return Sbool.new_constraint_sbool(Eq(self, other))
        return NotImplemented


# Build the small-int cache first so that the named-constant assignments
# below pick up the cached singletons (otherwise they would be standalone
# instances and ``ConcSlong(0) is ConcSlong.ZERO`` would be False).
ConcSlong._CACHE = tuple(
    ConcSlong.__new__(ConcSlong) for _ in range(ConcSlong._HIGH_CACHE - ConcSlong._LOW_CACHE + 1)
)
for _i, _obj in enumerate(ConcSlong._CACHE):
    _val = _i + ConcSlong._LOW_CACHE
    object.__setattr__(_obj, "_value", _val)
    object.__setattr__(_obj, "_hash", hash(_val))
    object.__setattr__(_obj, "_concolic", None)

# Convenience constants
ConcSlong.ZERO = ConcSlong(0)
ConcSlong.ONE = ConcSlong(1)
ConcSlong.MINUS_ONE = ConcSlong(-1)


class SymSlong(Slong, SymSnumber):
    """Symbolic long wrapping an expression."""

    __slots__ = ("_represented_expression", "_hash")

    def __init__(self, represented_expression: Expression) -> None:
        object.__setattr__(self, "_represented_expression", represented_expression)
        object.__setattr__(self, "_hash", hash(represented_expression))
        object.__setattr__(self, "_concolic", None)

    def __setattr__(self, name: str, value: object) -> None:
        raise AttributeError("SymSlong is immutable")

    @property
    def represented_expression(self) -> Expression:
        return self._represented_expression

    @property
    def is_fp(self) -> bool:
        return self._represented_expression.is_fp

    def __repr__(self) -> str:
        return f"SymSlong{{{self._represented_expression!r}}}"

    def __hash__(self) -> int:
        return self._hash


class SymSlongLeaf(SymSlong, SymSprimitiveLeaf):
    """Named symbolic long leaf."""

    __slots__ = ("_id",)
    _next_id = itertools.count(1)

    def __init__(self, explicit_id: int | str | None = None) -> None:
        if isinstance(explicit_id, str):
            # Accept string ID directly
            id_str = explicit_id
        elif explicit_id is not None:
            # Use explicit numeric ID
            id_str = f"Slong{explicit_id}"
        else:
            # Generate new numeric ID
            n = next(SymSlongLeaf._next_id)
            id_str = f"Slong{n}"
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
