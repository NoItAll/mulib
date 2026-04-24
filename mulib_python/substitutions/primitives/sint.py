"""Sint and related types: Sbool, Sbyte, Schar, Sshort.

Sint is the fundamental symbolic integer type. Sbool, Sbyte, Schar, and Sshort
extend Sint following Java's type hierarchy.
"""

from __future__ import annotations

import abc
import itertools
from typing import TYPE_CHECKING, Union, Any

from mulib_python.expressions import (
    Expression, Sum, Sub, Mul, Div, Mod, Neg,
    BitwiseAnd, BitwiseOr, BitwiseXor,
    ShiftLeft, ShiftRight, LogicalShiftRight,
)
from mulib_python.constraints import (
    Constraint, Lt, Lte, Eq, And, Or, Not, Xor as XorConstraint, TRUE, FALSE
)
from mulib_python.substitutions.primitives.sprimitive import SymSprimitiveLeaf
from mulib_python.substitutions.primitives.snumber import (
    AbstractSnumber, ConcSnumber, SymSnumber
)

if TYPE_CHECKING:
    from mulib_python.substitutions.primitives.slong import Slong
    from mulib_python.substitutions.primitives.sdouble import Sdouble
    from mulib_python.substitutions.primitives.sfloat import Sfloat


def _to_signed_byte(val: int) -> int:
    """Truncate to signed 8-bit."""
    return ((val + 128) % 256) - 128


def _to_signed_short(val: int) -> int:
    """Truncate to signed 16-bit."""
    return ((val + 32768) % 65536) - 32768


def _to_char(val: int) -> str:
    """Convert to a single character."""
    return chr(val & 0xFFFF)


# Sentinel used by interning-guarded ``__init__`` methods to detect
# already-initialised cached instances (cf. ``ConcSint.__init__``).
_UNSET: object = object()


# ---------------------------------------------------------------------------
# Sint
# ---------------------------------------------------------------------------

class Sint(AbstractSnumber, abc.ABC):
    """Symbolic integer (abstract base)."""

    __slots__ = ()

    @property
    def is_fp(self) -> bool:
        return False

    # --- Factory methods ---
    
    @staticmethod
    def conc_sint(i: int) -> "ConcSint":
        """Create a concrete Sint (cached for [-128, 127])."""
        return ConcSint.get_cached(i)

    @staticmethod
    def new_input_symbolic_sint(explicit_id: int | None = None) -> "SymSintLeaf":
        """Create a new symbolic Sint leaf variable."""
        return SymSintLeaf(explicit_id)

    @staticmethod
    def new_expression_symbolic_sint(expr: Expression) -> "SymSint":
        """Create a symbolic Sint wrapping an expression."""
        return SymSint(expr)

    # --- Helper for binary dispatch ---
    
    def _coerce_sint(self, other: Any) -> "Sint":
        """Coerce other to Sint if possible."""
        if isinstance(other, Sint):
            return other
        if isinstance(other, bool):
            return Sint.conc_sint(1 if other else 0)
        if isinstance(other, int):
            return Sint.conc_sint(other)
        raise TypeError(f"Cannot coerce {type(other).__name__} to Sint")

    def _dispatch_binary_int(
        self, 
        other: Any,
        conc_op,
        expr_builder,
    ) -> "Sint":
        """Dispatch binary operation - concrete fast path or symbolic."""
        try:
            other_sint = self._coerce_sint(other)
        except TypeError:
            return NotImplemented
        
        if isinstance(self, ConcSint) and isinstance(other_sint, ConcSint):
            return Sint.conc_sint(conc_op(self._value, other_sint._value))
        
        # Symbolic path - check for SE
        from mulib_python.substitutions._se_context import get_se
        se = get_se()
        if se is not None:
            return Sint.new_expression_symbolic_sint(expr_builder(self, other_sint))
        
        # No SE but at least one symbolic - just build expression
        return Sint.new_expression_symbolic_sint(expr_builder(self, other_sint))

    def _dispatch_comparison(
        self,
        other: Any,
        conc_op,
        constraint_builder,
    ) -> "Sbool":
        """Dispatch comparison - concrete fast path or symbolic."""
        try:
            other_sint = self._coerce_sint(other)
        except TypeError:
            return NotImplemented
        
        if isinstance(self, ConcSint) and isinstance(other_sint, ConcSint):
            return ConcSbool.TRUE if conc_op(self._value, other_sint._value) else ConcSbool.FALSE
        
        # Symbolic comparison
        return Sbool.new_constraint_sbool(constraint_builder(self, other_sint))

    # --- Arithmetic operators ---
    
    def __add__(self, other) -> "Sint":
        return self._dispatch_binary_int(
            other,
            lambda a, b: a + b,
            lambda l, r: Sum(l, r)
        )

    def __radd__(self, other) -> "Sint":
        return self._coerce_sint(other).__add__(self)

    def __sub__(self, other) -> "Sint":
        return self._dispatch_binary_int(
            other,
            lambda a, b: a - b,
            lambda l, r: Sub(l, r)
        )

    def __rsub__(self, other) -> "Sint":
        return self._coerce_sint(other).__sub__(self)

    def __mul__(self, other) -> "Sint":
        return self._dispatch_binary_int(
            other,
            lambda a, b: a * b,
            lambda l, r: Mul(l, r)
        )

    def __rmul__(self, other) -> "Sint":
        return self._coerce_sint(other).__mul__(self)

    def __truediv__(self, other) -> "Sint":
        """Integer division (truncation toward zero, like Java)."""
        def java_div(a: int, b: int) -> int:
            if b == 0:
                raise ZeroDivisionError("integer division by zero")
            q = abs(a) // abs(b)
            if (a < 0) ^ (b < 0):
                q = -q
            return q
        
        return self._dispatch_binary_int(
            other,
            java_div,
            lambda l, r: Div(l, r)
        )

    def __rtruediv__(self, other) -> "Sint":
        return self._coerce_sint(other).__truediv__(self)

    __floordiv__ = __truediv__
    __rfloordiv__ = __rtruediv__

    def __mod__(self, other) -> "Sint":
        def java_mod(a: int, b: int) -> int:
            if b == 0:
                raise ZeroDivisionError("integer modulo by zero")
            return a % b
        
        return self._dispatch_binary_int(
            other,
            java_mod,
            lambda l, r: Mod(l, r)
        )

    def __rmod__(self, other) -> "Sint":
        return self._coerce_sint(other).__mod__(self)

    def __neg__(self) -> "Sint":
        if isinstance(self, ConcSint):
            return Sint.conc_sint(-self._value)
        return Sint.new_expression_symbolic_sint(Neg(self))

    def __pos__(self) -> "Sint":
        return self

    # --- Comparison operators (return Sbool) ---
    
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
            lambda l, r: Lt(r, l)  # gt(l, r) = lt(r, l)
        )

    def __ge__(self, other) -> "Sbool":
        return self._dispatch_comparison(
            other,
            lambda a, b: a >= b,
            lambda l, r: Lte(r, l)  # ge(l, r) = lte(r, l)
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
    
    def __and__(self, other) -> "Sint | Sbool":
        if isinstance(other, Sbool):
            # Delegate to bool logic if both are Sbool
            if isinstance(self, Sbool):
                return self.bool_and(other)
        return self._dispatch_binary_int(
            other,
            lambda a, b: a & b,
            lambda l, r: BitwiseAnd(l, r)
        )

    def __rand__(self, other) -> "Sint | Sbool":
        return self.__and__(other)

    def __or__(self, other) -> "Sint | Sbool":
        if isinstance(other, Sbool):
            if isinstance(self, Sbool):
                return self.bool_or(other)
        return self._dispatch_binary_int(
            other,
            lambda a, b: a | b,
            lambda l, r: BitwiseOr(l, r)
        )

    def __ror__(self, other) -> "Sint | Sbool":
        return self.__or__(other)

    def __xor__(self, other) -> "Sint | Sbool":
        if isinstance(other, Sbool):
            if isinstance(self, Sbool):
                return self.bool_xor(other)
        return self._dispatch_binary_int(
            other,
            lambda a, b: a ^ b,
            lambda l, r: BitwiseXor(l, r)
        )

    def __rxor__(self, other) -> "Sint | Sbool":
        return self.__xor__(other)

    def __invert__(self) -> "Sint":
        """Bitwise NOT: ~x == -x - 1."""
        return -(self + Sint.conc_sint(1))

    def __lshift__(self, other) -> "Sint":
        return self._dispatch_binary_int(
            other,
            lambda a, b: a << (b & 0x1F),  # Java masks to 5 bits
            lambda l, r: ShiftLeft(l, r)
        )

    def __rlshift__(self, other) -> "Sint":
        return self._coerce_sint(other).__lshift__(self)

    def __rshift__(self, other) -> "Sint":
        """Arithmetic right shift."""
        return self._dispatch_binary_int(
            other,
            lambda a, b: a >> (b & 0x1F),
            lambda l, r: ShiftRight(l, r)
        )

    def __rrshift__(self, other) -> "Sint":
        return self._coerce_sint(other).__rshift__(self)

    def unsigned_rshift(self, other: "Sint") -> "Sint":
        """Logical (unsigned) right shift - Java's >>>."""
        def logical_rshift(a: int, b: int) -> int:
            shift = b & 0x1F
            if a >= 0:
                return a >> shift
            # Convert to unsigned 32-bit, shift, result is positive
            unsigned = a & 0xFFFFFFFF
            return unsigned >> shift
        
        return self._dispatch_binary_int(
            other,
            logical_rshift,
            lambda l, r: LogicalShiftRight(l, r)
        )

    # --- Type conversion ---
    
    def to_sdouble(self) -> "Sdouble":
        """Convert to Sdouble (i2d)."""
        from mulib_python.substitutions.primitives.sdouble import Sdouble, ConcSdouble
        if isinstance(self, ConcSint):
            return ConcSdouble(float(self._value))
        return Sdouble.new_expression_symbolic_sdouble(self)

    def to_sfloat(self) -> "Sfloat":
        """Convert to Sfloat (i2f)."""
        from mulib_python.substitutions.primitives.sfloat import Sfloat, ConcSfloat
        if isinstance(self, ConcSint):
            return ConcSfloat(float(self._value))
        return Sfloat.new_expression_symbolic_sfloat(self)

    def to_slong(self) -> "Slong":
        """Convert to Slong (i2l)."""
        from mulib_python.substitutions.primitives.slong import Slong, ConcSlong
        if isinstance(self, ConcSint):
            return ConcSlong(self._value)
        return Slong.new_expression_symbolic_slong(self)

    def to_sbyte(self) -> "Sbyte":
        """Convert to Sbyte (i2b)."""
        if isinstance(self, ConcSint):
            return ConcSbyte(_to_signed_byte(self._value))
        return SymSbyte(self)

    def to_sshort(self) -> "Sshort":
        """Convert to Sshort (i2s)."""
        if isinstance(self, ConcSint):
            return ConcSshort(_to_signed_short(self._value))
        return SymSshort(self)

    def to_schar(self) -> "Schar":
        """Convert to Schar (i2c)."""
        if isinstance(self, ConcSint):
            return ConcSchar(self._value & 0xFFFF)
        return SymSchar(self)

    # --- Choice methods ---
    
    def lt_choice(self, rhs: "Sint | None" = None) -> bool:
        """Comparison choice point: returns Python bool."""
        if rhs is None:
            rhs = Sint.conc_sint(0)
        return bool(self.__lt__(rhs))

    def lte_choice(self, rhs: "Sint | None" = None) -> bool:
        if rhs is None:
            rhs = Sint.conc_sint(0)
        return bool(self.__le__(rhs))

    def eq_choice(self, rhs: "Sint | None" = None) -> bool:
        if rhs is None:
            rhs = Sint.conc_sint(0)
        return bool(self.__eq__(rhs))

    def not_eq_choice(self, rhs: "Sint | None" = None) -> bool:
        if rhs is None:
            rhs = Sint.conc_sint(0)
        return bool(self.__ne__(rhs))

    def gt_choice(self, rhs: "Sint | None" = None) -> bool:
        if rhs is None:
            rhs = Sint.conc_sint(0)
        return bool(self.__gt__(rhs))

    def gte_choice(self, rhs: "Sint | None" = None) -> bool:
        if rhs is None:
            rhs = Sint.conc_sint(0)
        return bool(self.__ge__(rhs))


# ---------------------------------------------------------------------------
# ConcSint
# ---------------------------------------------------------------------------

class ConcSint(Sint, ConcSnumber):
    """Concrete integer with cached small values."""

    __slots__ = ("_value", "_hash")

    _LOW_CACHE = -128
    _HIGH_CACHE = 127
    _CACHE: tuple["ConcSint", ...] = ()

    def __new__(cls, value=_UNSET):
        # Cache hit only for the exact ConcSint class (subclasses get fresh
        # instances) and only once the cache has been populated below.
        if cls is ConcSint and ConcSint._CACHE and value is not _UNSET:
            try:
                if ConcSint._LOW_CACHE <= value <= ConcSint._HIGH_CACHE:
                    return ConcSint._CACHE[value - ConcSint._LOW_CACHE]
            except TypeError:
                # Non-numeric ``value`` — fall through to allocate so the
                # __init__ raises a meaningful error.
                pass
        return object.__new__(cls)

    def __init__(self, value: int) -> None:
        # Idempotent guard: when ``__new__`` returned a cached singleton,
        # Python still re-runs ``__init__``.  Skip the work in that case so
        # we don't re-hash and don't pay the per-call attribute cost.
        if getattr(self, "_value", _UNSET) == value:
            return
        # Don't call super().__init__() to avoid AbstractSnumber init
        object.__setattr__(self, "_value", value)
        object.__setattr__(self, "_hash", hash(value))
        object.__setattr__(self, "_concolic", None)

    def __setattr__(self, name: str, value: object) -> None:
        raise AttributeError("ConcSint is immutable")

    @classmethod
    def get_cached(cls, value: int) -> "ConcSint":
        """Return a cached instance for small values, or create new."""
        if cls._LOW_CACHE <= value <= cls._HIGH_CACHE:
            return cls._CACHE[value - cls._LOW_CACHE]
        return cls(value)

    @property
    def value(self) -> int:
        return self._value

    @property
    def is_fp(self) -> bool:
        return False

    def int_val(self) -> int:
        return self._value

    def double_val(self) -> float:
        return float(self._value)

    def float_val(self) -> float:
        return float(self._value)

    def long_val(self) -> int:
        return self._value

    def short_val(self) -> int:
        return _to_signed_short(self._value)

    def byte_val(self) -> int:
        return _to_signed_byte(self._value)

    def char_val(self) -> str:
        return _to_char(self._value)

    def __repr__(self) -> str:
        return str(self._value)

    def __hash__(self) -> int:
        return self._hash

    def __eq__(self, other) -> "Sbool":  # type: ignore[override]
        if isinstance(other, ConcSint):
            return ConcSbool.TRUE if self._value == other._value else ConcSbool.FALSE
        if isinstance(other, int):
            return ConcSbool.TRUE if self._value == other else ConcSbool.FALSE
        if isinstance(other, Sint):
            return Sbool.new_constraint_sbool(Eq(self, other))
        return NotImplemented


# Initialize the cache after class definition
ConcSint._CACHE = tuple(
    ConcSint.__new__(ConcSint) for _ in range(ConcSint._HIGH_CACHE - ConcSint._LOW_CACHE + 1)
)
for _i, _obj in enumerate(ConcSint._CACHE):
    _val = _i + ConcSint._LOW_CACHE
    object.__setattr__(_obj, "_value", _val)
    object.__setattr__(_obj, "_hash", hash(_val))
    object.__setattr__(_obj, "_concolic", None)

# Convenience constants
ConcSint.MINUS_ONE = ConcSint.get_cached(-1)
ConcSint.ZERO = ConcSint.get_cached(0)
ConcSint.ONE = ConcSint.get_cached(1)


# ---------------------------------------------------------------------------
# SymSint
# ---------------------------------------------------------------------------

class SymSint(Sint, SymSnumber):
    """Symbolic integer wrapping an Expression."""

    __slots__ = ("_represented_expression", "_hash")

    def __init__(self, represented_expression: Expression) -> None:
        object.__setattr__(self, "_represented_expression", represented_expression)
        object.__setattr__(self, "_hash", hash(represented_expression))
        object.__setattr__(self, "_concolic", None)

    def __setattr__(self, name: str, value: object) -> None:
        raise AttributeError("SymSint is immutable")

    @property
    def represented_expression(self) -> Expression:
        return self._represented_expression

    @property
    def is_fp(self) -> bool:
        return self._represented_expression.is_fp

    def __repr__(self) -> str:
        return f"SymSint{{{self._represented_expression!r}}}"

    def __hash__(self) -> int:
        return self._hash

    def __eq__(self, other) -> "Sbool":  # type: ignore[override]
        if type(other) is SymSint:
            if self._represented_expression == other._represented_expression:
                return ConcSbool.TRUE
            return Sbool.new_constraint_sbool(Eq(self, other))
        return super().__eq__(other)


# ---------------------------------------------------------------------------
# SymSintLeaf
# ---------------------------------------------------------------------------

class SymSintLeaf(SymSint, SymSprimitiveLeaf):
    """Named symbolic integer leaf variable."""

    __slots__ = ("_id",)
    _next_id = itertools.count(1)

    def __init__(self, explicit_id: int | str | None = None) -> None:
        if isinstance(explicit_id, str):
            var_id = explicit_id
        elif explicit_id is not None:
            var_id = f"Sint{explicit_id}"
        else:
            var_id = f"Sint{next(SymSintLeaf._next_id)}"
        object.__setattr__(self, "_id", var_id)
        # The leaf IS its own represented expression
        object.__setattr__(self, "_represented_expression", self)
        object.__setattr__(self, "_hash", hash(var_id))
        object.__setattr__(self, "_concolic", None)

    @property
    def id(self) -> str:
        return self._id

    def __repr__(self) -> str:
        return self._id

    def __hash__(self) -> int:
        return self._hash

    def __eq__(self, other) -> "Sbool":  # type: ignore[override]
        if type(other) is SymSintLeaf:
            if self._id == other._id:
                return ConcSbool.TRUE
            return Sbool.new_constraint_sbool(Eq(self, other))
        return super().__eq__(other)


# ---------------------------------------------------------------------------
# Sbool
# ---------------------------------------------------------------------------

class Sbool(Sint, Constraint, abc.ABC):
    """Symbolic boolean - both a Sint (0/1) and a Constraint."""

    __slots__ = ()

    @staticmethod
    def conc_sbool(b: bool) -> "ConcSbool":
        """Return TRUE or FALSE singleton."""
        return ConcSbool.TRUE if b else ConcSbool.FALSE

    @staticmethod
    def new_input_symbolic_sbool(explicit_id: int | None = None) -> "SymSboolLeaf":
        """Create a new symbolic boolean leaf."""
        return SymSboolLeaf(explicit_id)

    @staticmethod
    def new_constraint_sbool(c: Constraint) -> "SymSbool":
        """Wrap a constraint in a symbolic boolean."""
        if c is TRUE:
            return ConcSbool.TRUE
        if c is FALSE:
            return ConcSbool.FALSE
        return SymSbool(c)

    # --- Boolean operations (return Sbool) ---
    
    def bool_and(self, rhs: "Sbool") -> "Sbool":
        """Logical AND."""
        if isinstance(self, ConcSbool) and isinstance(rhs, ConcSbool):
            return Sbool.conc_sbool(self._value and rhs._value)
        return Sbool.new_constraint_sbool(And.new_instance(self, rhs))

    def bool_or(self, rhs: "Sbool") -> "Sbool":
        """Logical OR."""
        if isinstance(self, ConcSbool) and isinstance(rhs, ConcSbool):
            return Sbool.conc_sbool(self._value or rhs._value)
        return Sbool.new_constraint_sbool(Or.new_instance(self, rhs))

    def bool_xor(self, rhs: "Sbool") -> "Sbool":
        """Logical XOR."""
        if isinstance(self, ConcSbool) and isinstance(rhs, ConcSbool):
            return Sbool.conc_sbool(self._value != rhs._value)
        return Sbool.new_constraint_sbool(XorConstraint(self, rhs))

    def bool_not(self) -> "Sbool":
        """Logical NOT."""
        if isinstance(self, ConcSbool):
            return Sbool.conc_sbool(not self._value)
        return Sbool.new_constraint_sbool(Not(self))

    def is_equal_to(self, rhs: "Sbool") -> "Sbool":
        """Boolean equality."""
        if isinstance(self, ConcSbool) and isinstance(rhs, ConcSbool):
            return Sbool.conc_sbool(self._value == rhs._value)
        from mulib_python.constraints import Equivalence
        return Sbool.new_constraint_sbool(Equivalence(self, rhs))

    def __bool__(self) -> bool:
        """Choice-point gateway - must be overridden by subclasses."""
        raise NotImplementedError("Sbool.__bool__ must be overridden")


# ---------------------------------------------------------------------------
# ConcSbool
# ---------------------------------------------------------------------------

class ConcSbool(Sbool, ConcSnumber):
    """Concrete boolean singleton (TRUE/FALSE)."""

    __slots__ = ("_value", "_hash")

    def __new__(cls, value=_UNSET):
        if cls is ConcSbool and value is not _UNSET:
            t = getattr(ConcSbool, "TRUE", None)
            f = getattr(ConcSbool, "FALSE", None)
            if t is not None and f is not None:
                # Both singletons exist -> intern.
                return t if value else f
        return object.__new__(cls)

    def __init__(self, value: bool) -> None:
        # Coerce to a true ``bool`` once, so callers passing ``1`` or
        # ``"yes"`` get the canonical Python boolean stored.
        value = bool(value)
        if getattr(self, "_value", _UNSET) is value:
            return
        object.__setattr__(self, "_value", value)
        object.__setattr__(self, "_hash", int(value))
        object.__setattr__(self, "_concolic", None)

    def __setattr__(self, name: str, value: object) -> None:
        raise AttributeError("ConcSbool is immutable")

    @property
    def value(self) -> bool:
        return self._value

    def is_true(self) -> bool:
        return self._value

    def is_false(self) -> bool:
        return not self._value

    def negate(self) -> "ConcSbool":
        return ConcSbool.FALSE if self._value else ConcSbool.TRUE

    @property
    def is_fp(self) -> bool:
        return False

    def int_val(self) -> int:
        return 1 if self._value else 0

    def double_val(self) -> float:
        return 1.0 if self._value else 0.0

    def float_val(self) -> float:
        return 1.0 if self._value else 0.0

    def long_val(self) -> int:
        return 1 if self._value else 0

    def short_val(self) -> int:
        return 1 if self._value else 0

    def byte_val(self) -> int:
        return 1 if self._value else 0

    def char_val(self) -> str:
        return chr(1) if self._value else chr(0)

    def __bool__(self) -> bool:
        """Concrete boolean - no choice point needed."""
        return self._value

    def __repr__(self) -> str:
        return "TRUE" if self._value else "FALSE"

    def __hash__(self) -> int:
        return self._hash

    def __eq__(self, other) -> "Sbool":  # type: ignore[override]
        if isinstance(other, ConcSbool):
            return ConcSbool.TRUE if self._value == other._value else ConcSbool.FALSE
        if isinstance(other, bool):
            return ConcSbool.TRUE if self._value == other else ConcSbool.FALSE
        if isinstance(other, Sbool):
            return self.is_equal_to(other)
        return NotImplemented

    def __invert__(self) -> "ConcSbool":
        """Logical NOT for boolean."""
        return self.negate()


# Create singletons after class definition
ConcSbool.TRUE = ConcSbool(True)
ConcSbool.FALSE = ConcSbool(False)


# ---------------------------------------------------------------------------
# SymSbool
# ---------------------------------------------------------------------------

class SymSbool(Sbool, SymSnumber):
    """Symbolic boolean wrapping a Constraint."""

    __slots__ = ("_represented_constraint", "_hash")

    def __init__(self, represented_constraint: Constraint) -> None:
        object.__setattr__(self, "_represented_constraint", represented_constraint)
        object.__setattr__(self, "_hash", hash(represented_constraint))
        object.__setattr__(self, "_concolic", None)

    def __setattr__(self, name: str, value: object) -> None:
        raise AttributeError("SymSbool is immutable")

    @property
    def represented_constraint(self) -> Constraint:
        return self._represented_constraint

    @property
    def represented_expression(self) -> Expression:
        # SymSbool is both a Constraint and an Expression
        return self

    @property
    def is_fp(self) -> bool:
        return False

    def __bool__(self) -> bool:
        """Choice-point gateway - delegates to SymbolicExecution."""
        from mulib_python.substitutions._se_context import _get_se_or_raise
        se = _get_se_or_raise()
        return se.bool_choice(self)

    def __repr__(self) -> str:
        return f"SymSbool{{{self._represented_constraint!r}}}"

    def __hash__(self) -> int:
        return self._hash

    def __eq__(self, other) -> "Sbool":  # type: ignore[override]
        if type(other) is SymSbool:
            if self._represented_constraint == other._represented_constraint:
                return ConcSbool.TRUE
        return super().__eq__(other)


# ---------------------------------------------------------------------------
# SymSboolLeaf
# ---------------------------------------------------------------------------

class SymSboolLeaf(SymSbool, SymSprimitiveLeaf):
    """Named symbolic boolean leaf variable."""

    __slots__ = ("_id",)
    _next_id = itertools.count(1)

    def __init__(self, explicit_id: int | str | None = None) -> None:
        if isinstance(explicit_id, str):
            var_id = explicit_id
        elif explicit_id is not None:
            var_id = f"Sbool{explicit_id}"
        else:
            var_id = f"Sbool{next(SymSboolLeaf._next_id)}"
        object.__setattr__(self, "_id", var_id)
        # The leaf IS its own represented constraint
        object.__setattr__(self, "_represented_constraint", self)
        object.__setattr__(self, "_hash", hash(var_id))
        object.__setattr__(self, "_concolic", None)

    @property
    def id(self) -> str:
        return self._id

    def __repr__(self) -> str:
        return self._id

    def __hash__(self) -> int:
        return self._hash

    def __eq__(self, other) -> "Sbool":  # type: ignore[override]
        if type(other) is SymSboolLeaf:
            if self._id == other._id:
                return ConcSbool.TRUE
        return super().__eq__(other)


# ---------------------------------------------------------------------------
# Sbyte
# ---------------------------------------------------------------------------

class Sbyte(Sint, abc.ABC):
    """Symbolic byte (-128 to 127)."""

    __slots__ = ()

    @staticmethod
    def conc_sbyte(b: int) -> "ConcSbyte":
        return ConcSbyte(b)

    @staticmethod
    def new_input_symbolic_sbyte(explicit_id: int | None = None) -> "SymSbyteLeaf":
        return SymSbyteLeaf(explicit_id)

    @staticmethod
    def new_expression_symbolic_sbyte(expr: Expression) -> "SymSbyte":
        return SymSbyte(expr)


class ConcSbyte(Sbyte, ConcSnumber):
    """Concrete byte."""

    __slots__ = ("_value", "_hash")

    # Bytes have a small fixed range [-128, 127]; cache the entire range.
    _LOW_CACHE = -128
    _HIGH_CACHE = 127
    _CACHE: tuple["ConcSbyte", ...] = ()

    def __new__(cls, value=_UNSET):
        if cls is ConcSbyte and ConcSbyte._CACHE and value is not _UNSET:
            try:
                v = _to_signed_byte(value)
            except TypeError:
                return object.__new__(cls)
            return ConcSbyte._CACHE[v - ConcSbyte._LOW_CACHE]
        return object.__new__(cls)

    def __init__(self, value: int) -> None:
        value = _to_signed_byte(value)
        if getattr(self, "_value", _UNSET) == value:
            return
        object.__setattr__(self, "_value", value)
        object.__setattr__(self, "_hash", hash(value))
        object.__setattr__(self, "_concolic", None)

    def __setattr__(self, name: str, value: object) -> None:
        raise AttributeError("ConcSbyte is immutable")

    @property
    def value(self) -> int:
        return self._value

    @property
    def is_fp(self) -> bool:
        return False

    def int_val(self) -> int:
        return self._value

    def double_val(self) -> float:
        return float(self._value)

    def float_val(self) -> float:
        return float(self._value)

    def long_val(self) -> int:
        return self._value

    def short_val(self) -> int:
        return self._value

    def byte_val(self) -> int:
        return self._value

    def char_val(self) -> str:
        return _to_char(self._value)

    def __repr__(self) -> str:
        return f"Sbyte({self._value})"

    def __hash__(self) -> int:
        return self._hash


ConcSbyte._CACHE = tuple(
    ConcSbyte.__new__(ConcSbyte) for _ in range(ConcSbyte._HIGH_CACHE - ConcSbyte._LOW_CACHE + 1)
)
for _i, _obj in enumerate(ConcSbyte._CACHE):
    _val = _i + ConcSbyte._LOW_CACHE
    object.__setattr__(_obj, "_value", _val)
    object.__setattr__(_obj, "_hash", hash(_val))
    object.__setattr__(_obj, "_concolic", None)

ConcSbyte.ZERO = ConcSbyte(0)


class SymSbyte(Sbyte, SymSnumber):
    """Symbolic byte wrapping an expression."""

    __slots__ = ("_represented_expression", "_hash")

    def __init__(self, represented_expression: Expression) -> None:
        object.__setattr__(self, "_represented_expression", represented_expression)
        object.__setattr__(self, "_hash", hash(represented_expression))
        object.__setattr__(self, "_concolic", None)

    def __setattr__(self, name: str, value: object) -> None:
        raise AttributeError("SymSbyte is immutable")

    @property
    def represented_expression(self) -> Expression:
        return self._represented_expression

    @property
    def is_fp(self) -> bool:
        return False

    def __repr__(self) -> str:
        return f"SymSbyte{{{self._represented_expression!r}}}"

    def __hash__(self) -> int:
        return self._hash


class SymSbyteLeaf(SymSbyte, SymSprimitiveLeaf):
    """Named symbolic byte leaf."""

    __slots__ = ("_id",)
    _next_id = itertools.count(1)

    def __init__(self, explicit_id: int | None = None) -> None:
        n = explicit_id if explicit_id is not None else next(SymSbyteLeaf._next_id)
        object.__setattr__(self, "_id", f"Sbyte{n}")
        object.__setattr__(self, "_represented_expression", self)
        object.__setattr__(self, "_hash", hash(f"Sbyte{n}"))
        object.__setattr__(self, "_concolic", None)

    @property
    def id(self) -> str:
        return self._id

    def __repr__(self) -> str:
        return self._id

    def __hash__(self) -> int:
        return self._hash


# ---------------------------------------------------------------------------
# Schar
# ---------------------------------------------------------------------------

class Schar(Sint, abc.ABC):
    """Symbolic character (0 to 65535)."""

    __slots__ = ()

    @staticmethod
    def conc_schar(c: str | int) -> "ConcSchar":
        if isinstance(c, str):
            return ConcSchar(ord(c[0]) if c else 0)
        return ConcSchar(c)

    @staticmethod
    def new_input_symbolic_schar(explicit_id: int | None = None) -> "SymScharLeaf":
        return SymScharLeaf(explicit_id)

    @staticmethod
    def new_expression_symbolic_schar(expr: Expression) -> "SymSchar":
        return SymSchar(expr)


class ConcSchar(Schar, ConcSnumber):
    """Concrete character."""

    __slots__ = ("_value", "_hash")

    # Cache the ASCII range; full Unicode (0..65535) would be too large.
    _LOW_CACHE = 0
    _HIGH_CACHE = 127
    _CACHE: tuple["ConcSchar", ...] = ()

    def __new__(cls, value=_UNSET):
        if cls is ConcSchar and ConcSchar._CACHE and value is not _UNSET:
            try:
                v = value & 0xFFFF
            except TypeError:
                return object.__new__(cls)
            if ConcSchar._LOW_CACHE <= v <= ConcSchar._HIGH_CACHE:
                return ConcSchar._CACHE[v - ConcSchar._LOW_CACHE]
        return object.__new__(cls)

    def __init__(self, value: int) -> None:
        value = value & 0xFFFF
        if getattr(self, "_value", _UNSET) == value:
            return
        object.__setattr__(self, "_value", value)
        object.__setattr__(self, "_hash", hash(value))
        object.__setattr__(self, "_concolic", None)

    def __setattr__(self, name: str, value: object) -> None:
        raise AttributeError("ConcSchar is immutable")

    @property
    def value(self) -> int:
        return self._value

    @property
    def is_fp(self) -> bool:
        return False

    def int_val(self) -> int:
        return self._value

    def double_val(self) -> float:
        return float(self._value)

    def float_val(self) -> float:
        return float(self._value)

    def long_val(self) -> int:
        return self._value

    def short_val(self) -> int:
        return _to_signed_short(self._value)

    def byte_val(self) -> int:
        return _to_signed_byte(self._value)

    def char_val(self) -> str:
        return chr(self._value)

    def __repr__(self) -> str:
        return f"Schar({chr(self._value)!r})"

    def __hash__(self) -> int:
        return self._hash


ConcSchar._CACHE = tuple(
    ConcSchar.__new__(ConcSchar) for _ in range(ConcSchar._HIGH_CACHE - ConcSchar._LOW_CACHE + 1)
)
for _i, _obj in enumerate(ConcSchar._CACHE):
    _val = _i + ConcSchar._LOW_CACHE
    object.__setattr__(_obj, "_value", _val)
    object.__setattr__(_obj, "_hash", hash(_val))
    object.__setattr__(_obj, "_concolic", None)

ConcSchar.ZERO = ConcSchar(0)


class SymSchar(Schar, SymSnumber):
    """Symbolic character wrapping an expression."""

    __slots__ = ("_represented_expression", "_hash")

    def __init__(self, represented_expression: Expression) -> None:
        object.__setattr__(self, "_represented_expression", represented_expression)
        object.__setattr__(self, "_hash", hash(represented_expression))
        object.__setattr__(self, "_concolic", None)

    def __setattr__(self, name: str, value: object) -> None:
        raise AttributeError("SymSchar is immutable")

    @property
    def represented_expression(self) -> Expression:
        return self._represented_expression

    @property
    def is_fp(self) -> bool:
        return False

    def __repr__(self) -> str:
        return f"SymSchar{{{self._represented_expression!r}}}"

    def __hash__(self) -> int:
        return self._hash


class SymScharLeaf(SymSchar, SymSprimitiveLeaf):
    """Named symbolic character leaf."""

    __slots__ = ("_id",)
    _next_id = itertools.count(1)

    def __init__(self, explicit_id: int | None = None) -> None:
        n = explicit_id if explicit_id is not None else next(SymScharLeaf._next_id)
        object.__setattr__(self, "_id", f"Schar{n}")
        object.__setattr__(self, "_represented_expression", self)
        object.__setattr__(self, "_hash", hash(f"Schar{n}"))
        object.__setattr__(self, "_concolic", None)

    @property
    def id(self) -> str:
        return self._id

    def __repr__(self) -> str:
        return self._id

    def __hash__(self) -> int:
        return self._hash


# ---------------------------------------------------------------------------
# Sshort
# ---------------------------------------------------------------------------

class Sshort(Sint, abc.ABC):
    """Symbolic short (-32768 to 32767)."""

    __slots__ = ()

    @staticmethod
    def conc_sshort(s: int) -> "ConcSshort":
        return ConcSshort(s)

    @staticmethod
    def new_input_symbolic_sshort(explicit_id: int | None = None) -> "SymSshortLeaf":
        return SymSshortLeaf(explicit_id)

    @staticmethod
    def new_expression_symbolic_sshort(expr: Expression) -> "SymSshort":
        return SymSshort(expr)


class ConcSshort(Sshort, ConcSnumber):
    """Concrete short."""

    __slots__ = ("_value", "_hash")

    # Full short range is [-32768, 32767]; only the small subset is cached.
    _LOW_CACHE = -128
    _HIGH_CACHE = 127
    _CACHE: tuple["ConcSshort", ...] = ()

    def __new__(cls, value=_UNSET):
        if cls is ConcSshort and ConcSshort._CACHE and value is not _UNSET:
            try:
                v = _to_signed_short(value)
            except TypeError:
                return object.__new__(cls)
            if ConcSshort._LOW_CACHE <= v <= ConcSshort._HIGH_CACHE:
                return ConcSshort._CACHE[v - ConcSshort._LOW_CACHE]
        return object.__new__(cls)

    def __init__(self, value: int) -> None:
        value = _to_signed_short(value)
        if getattr(self, "_value", _UNSET) == value:
            return
        object.__setattr__(self, "_value", value)
        object.__setattr__(self, "_hash", hash(value))
        object.__setattr__(self, "_concolic", None)

    def __setattr__(self, name: str, value: object) -> None:
        raise AttributeError("ConcSshort is immutable")

    @property
    def value(self) -> int:
        return self._value

    @property
    def is_fp(self) -> bool:
        return False

    def int_val(self) -> int:
        return self._value

    def double_val(self) -> float:
        return float(self._value)

    def float_val(self) -> float:
        return float(self._value)

    def long_val(self) -> int:
        return self._value

    def short_val(self) -> int:
        return self._value

    def byte_val(self) -> int:
        return _to_signed_byte(self._value)

    def char_val(self) -> str:
        return _to_char(self._value)

    def __repr__(self) -> str:
        return f"Sshort({self._value})"

    def __hash__(self) -> int:
        return self._hash


ConcSshort._CACHE = tuple(
    ConcSshort.__new__(ConcSshort) for _ in range(ConcSshort._HIGH_CACHE - ConcSshort._LOW_CACHE + 1)
)
for _i, _obj in enumerate(ConcSshort._CACHE):
    _val = _i + ConcSshort._LOW_CACHE
    object.__setattr__(_obj, "_value", _val)
    object.__setattr__(_obj, "_hash", hash(_val))
    object.__setattr__(_obj, "_concolic", None)

ConcSshort.ZERO = ConcSshort(0)


class SymSshort(Sshort, SymSnumber):
    """Symbolic short wrapping an expression."""

    __slots__ = ("_represented_expression", "_hash")

    def __init__(self, represented_expression: Expression) -> None:
        object.__setattr__(self, "_represented_expression", represented_expression)
        object.__setattr__(self, "_hash", hash(represented_expression))
        object.__setattr__(self, "_concolic", None)

    def __setattr__(self, name: str, value: object) -> None:
        raise AttributeError("SymSshort is immutable")

    @property
    def represented_expression(self) -> Expression:
        return self._represented_expression

    @property
    def is_fp(self) -> bool:
        return False

    def __repr__(self) -> str:
        return f"SymSshort{{{self._represented_expression!r}}}"

    def __hash__(self) -> int:
        return self._hash


class SymSshortLeaf(SymSshort, SymSprimitiveLeaf):
    """Named symbolic short leaf."""

    __slots__ = ("_id",)
    _next_id = itertools.count(1)

    def __init__(self, explicit_id: int | None = None) -> None:
        n = explicit_id if explicit_id is not None else next(SymSshortLeaf._next_id)
        object.__setattr__(self, "_id", f"Sshort{n}")
        object.__setattr__(self, "_represented_expression", self)
        object.__setattr__(self, "_hash", hash(f"Sshort{n}"))
        object.__setattr__(self, "_concolic", None)

    @property
    def id(self) -> str:
        return self._id

    def __repr__(self) -> str:
        return self._id

    def __hash__(self) -> int:
        return self._hash
