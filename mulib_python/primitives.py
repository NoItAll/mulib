"""Symbolic primitives: Sint, Sbool, Sdouble.

Each primitive wraps a z3 expression and (for concrete values) a Python value.
Magic methods build new symbolic values from operations.
"""

from __future__ import annotations

import z3

from .exceptions import MulibIllegalStateException


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _wrap_int(x):
    if isinstance(x, Sint):
        return x
    if isinstance(x, bool):
        return ConcSint(1 if x else 0)
    if isinstance(x, int):
        return ConcSint(x)
    raise TypeError(f"Cannot convert {type(x).__name__} to Sint")


def _wrap_bool(x):
    if isinstance(x, Sbool):
        return x
    if isinstance(x, bool):
        return ConcSbool(x)
    raise TypeError(f"Cannot convert {type(x).__name__} to Sbool")


def _wrap_double(x):
    if isinstance(x, Sdouble):
        return x
    if isinstance(x, Sint):
        return SymSdouble(z3.ToReal(x.z3_expr()))
    if isinstance(x, (int, float)):
        return ConcSdouble(float(x))
    raise TypeError(f"Cannot convert {type(x).__name__} to Sdouble")


# ---------------------------------------------------------------------------
# Sint
# ---------------------------------------------------------------------------

class Sint:
    """Symbolic integer (abstract)."""

    __slots__ = ()

    def z3_expr(self):  # pragma: no cover - abstract
        raise NotImplementedError

    # --- arithmetic ---------------------------------------------------------
    def __add__(self, other):
        other = _wrap_int(other)
        if isinstance(self, ConcSint) and isinstance(other, ConcSint):
            return ConcSint(self.value + other.value)
        return SymSint(self.z3_expr() + other.z3_expr())

    __radd__ = __add__

    def __sub__(self, other):
        other = _wrap_int(other)
        if isinstance(self, ConcSint) and isinstance(other, ConcSint):
            return ConcSint(self.value - other.value)
        return SymSint(self.z3_expr() - other.z3_expr())

    def __rsub__(self, other):
        return _wrap_int(other).__sub__(self)

    def __mul__(self, other):
        other = _wrap_int(other)
        if isinstance(self, ConcSint) and isinstance(other, ConcSint):
            return ConcSint(self.value * other.value)
        return SymSint(self.z3_expr() * other.z3_expr())

    __rmul__ = __mul__

    def __floordiv__(self, other):
        other = _wrap_int(other)
        if isinstance(self, ConcSint) and isinstance(other, ConcSint) and other.value != 0:
            # Java integer division semantics: truncate toward zero
            a, b = self.value, other.value
            q = abs(a) // abs(b)
            if (a < 0) ^ (b < 0):
                q = -q
            return ConcSint(q)
        return SymSint(self.z3_expr() / other.z3_expr())

    def __rfloordiv__(self, other):
        return _wrap_int(other).__floordiv__(self)

    __truediv__ = __floordiv__
    __rtruediv__ = __rfloordiv__

    def __mod__(self, other):
        other = _wrap_int(other)
        if isinstance(self, ConcSint) and isinstance(other, ConcSint) and other.value != 0:
            return ConcSint(self.value - (self.value // other.value) * other.value)
        return SymSint(self.z3_expr() % other.z3_expr())

    def __neg__(self):
        if isinstance(self, ConcSint):
            return ConcSint(-self.value)
        return SymSint(-self.z3_expr())

    def __pos__(self):
        return self

    def __abs__(self):
        if isinstance(self, ConcSint):
            return ConcSint(abs(self.value))
        e = self.z3_expr()
        return SymSint(z3.If(e >= 0, e, -e))

    # --- comparisons --------------------------------------------------------
    def _cmp(self, other, py_op, z3_op):
        other = _wrap_int(other)
        if isinstance(self, ConcSint) and isinstance(other, ConcSint):
            return ConcSbool(py_op(self.value, other.value))
        return SymSbool(z3_op(self.z3_expr(), other.z3_expr()))

    def __lt__(self, other): return self._cmp(other, lambda a, b: a < b,  lambda a, b: a < b)
    def __le__(self, other): return self._cmp(other, lambda a, b: a <= b, lambda a, b: a <= b)
    def __gt__(self, other): return self._cmp(other, lambda a, b: a > b,  lambda a, b: a > b)
    def __ge__(self, other): return self._cmp(other, lambda a, b: a >= b, lambda a, b: a >= b)

    def __eq__(self, other):
        try:
            other = _wrap_int(other)
        except TypeError:
            return NotImplemented
        if isinstance(self, ConcSint) and isinstance(other, ConcSint):
            return ConcSbool(self.value == other.value)
        return SymSbool(self.z3_expr() == other.z3_expr())

    def __ne__(self, other):
        try:
            other = _wrap_int(other)
        except TypeError:
            return NotImplemented
        if isinstance(self, ConcSint) and isinstance(other, ConcSint):
            return ConcSbool(self.value != other.value)
        return SymSbool(self.z3_expr() != other.z3_expr())

    def __hash__(self):
        if isinstance(self, ConcSint):
            return hash(("ConcSint", self.value))
        return id(self)

    # --- bitwise (via 32-bit BV) -------------------------------------------
    @staticmethod
    def _to_bv(e):
        return z3.Int2BV(e, 32)

    @staticmethod
    def _from_bv(bv):
        return z3.BV2Int(bv, is_signed=True)

    def _bitop(self, other, py_op, z3_bv_op):
        other = _wrap_int(other)
        if isinstance(self, ConcSint) and isinstance(other, ConcSint):
            return ConcSint(py_op(self.value, other.value))
        bv = z3_bv_op(self._to_bv(self.z3_expr()), self._to_bv(other.z3_expr()))
        return SymSint(self._from_bv(bv))

    def __and__(self, other): return self._bitop(other, lambda a, b: a & b, lambda a, b: a & b)
    def __or__(self, other):  return self._bitop(other, lambda a, b: a | b, lambda a, b: a | b)
    def __xor__(self, other): return self._bitop(other, lambda a, b: a ^ b, lambda a, b: a ^ b)
    __rand__ = __and__
    __ror__ = __or__
    __rxor__ = __xor__

    def __invert__(self):
        if isinstance(self, ConcSint):
            return ConcSint(~self.value)
        return SymSint(self._from_bv(~self._to_bv(self.z3_expr())))

    def __lshift__(self, other):
        other = _wrap_int(other)
        if isinstance(self, ConcSint) and isinstance(other, ConcSint):
            return ConcSint(self.value << other.value)
        return SymSint(self._from_bv(self._to_bv(self.z3_expr()) << self._to_bv(other.z3_expr())))

    def __rshift__(self, other):
        other = _wrap_int(other)
        if isinstance(self, ConcSint) and isinstance(other, ConcSint):
            return ConcSint(self.value >> other.value)
        return SymSint(self._from_bv(self._to_bv(self.z3_expr()) >> self._to_bv(other.z3_expr())))

    def __bool__(self):
        # Truthiness on an int: treat as (self != 0)
        return bool(self != 0)

    def __int__(self):
        if isinstance(self, ConcSint):
            return self.value
        raise MulibIllegalStateException("Cannot convert symbolic Sint to int")


class ConcSint(Sint):
    __slots__ = ("value",)

    def __init__(self, value: int):
        self.value = int(value)

    def z3_expr(self):
        return z3.IntVal(self.value)

    def __repr__(self):
        return f"ConcSint({self.value})"


class SymSint(Sint):
    __slots__ = ("_z3", "_name")

    def __init__(self, z3_expr, name: str | None = None):
        self._z3 = z3_expr
        self._name = name

    def z3_expr(self):
        return self._z3

    def __repr__(self):
        return f"SymSint({self._name or self._z3})"


# ---------------------------------------------------------------------------
# Sbool
# ---------------------------------------------------------------------------

class Sbool:
    """Symbolic boolean (abstract)."""

    __slots__ = ()

    def z3_expr(self):  # pragma: no cover - abstract
        raise NotImplementedError

    def __and__(self, other):
        other = _wrap_bool(other)
        if isinstance(self, ConcSbool) and isinstance(other, ConcSbool):
            return ConcSbool(self.value and other.value)
        return SymSbool(z3.And(self.z3_expr(), other.z3_expr()))

    def __or__(self, other):
        other = _wrap_bool(other)
        if isinstance(self, ConcSbool) and isinstance(other, ConcSbool):
            return ConcSbool(self.value or other.value)
        return SymSbool(z3.Or(self.z3_expr(), other.z3_expr()))

    def __xor__(self, other):
        other = _wrap_bool(other)
        if isinstance(self, ConcSbool) and isinstance(other, ConcSbool):
            return ConcSbool(bool(self.value) ^ bool(other.value))
        return SymSbool(z3.Xor(self.z3_expr(), other.z3_expr()))

    __rand__ = __and__
    __ror__ = __or__
    __rxor__ = __xor__

    def __invert__(self):
        if isinstance(self, ConcSbool):
            return ConcSbool(not self.value)
        return SymSbool(z3.Not(self.z3_expr()))

    def __eq__(self, other):
        try:
            other = _wrap_bool(other)
        except TypeError:
            return NotImplemented
        if isinstance(self, ConcSbool) and isinstance(other, ConcSbool):
            return ConcSbool(self.value == other.value)
        return SymSbool(self.z3_expr() == other.z3_expr())

    def __ne__(self, other):
        return ~(self == other)

    def __hash__(self):
        if isinstance(self, ConcSbool):
            return hash(("ConcSbool", self.value))
        return id(self)

    def __bool__(self):
        if isinstance(self, ConcSbool):
            return self.value
        from .execution import SymbolicExecution
        se = SymbolicExecution.get()
        if se is None:
            raise MulibIllegalStateException("No SymbolicExecution active; cannot evaluate symbolic bool")
        return se.bool_choice(self)


class ConcSbool(Sbool):
    __slots__ = ("value",)

    def __init__(self, value: bool):
        self.value = bool(value)

    def z3_expr(self):
        return z3.BoolVal(self.value)

    def __repr__(self):
        return f"ConcSbool({self.value})"


class SymSbool(Sbool):
    __slots__ = ("_z3", "_name")

    def __init__(self, z3_expr, name: str | None = None):
        self._z3 = z3_expr
        self._name = name

    def z3_expr(self):
        return self._z3

    def __repr__(self):
        return f"SymSbool({self._name or self._z3})"


# ---------------------------------------------------------------------------
# Sdouble (modeled with z3 Reals for simplicity)
# ---------------------------------------------------------------------------

class Sdouble:
    __slots__ = ()

    def z3_expr(self):  # pragma: no cover
        raise NotImplementedError

    def __add__(self, other):
        other = _wrap_double(other)
        if isinstance(self, ConcSdouble) and isinstance(other, ConcSdouble):
            return ConcSdouble(self.value + other.value)
        return SymSdouble(self.z3_expr() + other.z3_expr())

    __radd__ = __add__

    def __sub__(self, other):
        other = _wrap_double(other)
        if isinstance(self, ConcSdouble) and isinstance(other, ConcSdouble):
            return ConcSdouble(self.value - other.value)
        return SymSdouble(self.z3_expr() - other.z3_expr())

    def __rsub__(self, other):
        return _wrap_double(other).__sub__(self)

    def __mul__(self, other):
        other = _wrap_double(other)
        if isinstance(self, ConcSdouble) and isinstance(other, ConcSdouble):
            return ConcSdouble(self.value * other.value)
        return SymSdouble(self.z3_expr() * other.z3_expr())

    __rmul__ = __mul__

    def __truediv__(self, other):
        other = _wrap_double(other)
        if isinstance(self, ConcSdouble) and isinstance(other, ConcSdouble) and other.value != 0:
            return ConcSdouble(self.value / other.value)
        return SymSdouble(self.z3_expr() / other.z3_expr())

    def __rtruediv__(self, other):
        return _wrap_double(other).__truediv__(self)

    def __neg__(self):
        if isinstance(self, ConcSdouble):
            return ConcSdouble(-self.value)
        return SymSdouble(-self.z3_expr())

    def _cmp(self, other, py_op, z3_op):
        other = _wrap_double(other)
        if isinstance(self, ConcSdouble) and isinstance(other, ConcSdouble):
            return ConcSbool(py_op(self.value, other.value))
        return SymSbool(z3_op(self.z3_expr(), other.z3_expr()))

    def __lt__(self, other): return self._cmp(other, lambda a, b: a < b,  lambda a, b: a < b)
    def __le__(self, other): return self._cmp(other, lambda a, b: a <= b, lambda a, b: a <= b)
    def __gt__(self, other): return self._cmp(other, lambda a, b: a > b,  lambda a, b: a > b)
    def __ge__(self, other): return self._cmp(other, lambda a, b: a >= b, lambda a, b: a >= b)

    def __eq__(self, other):
        try:
            other = _wrap_double(other)
        except TypeError:
            return NotImplemented
        if isinstance(self, ConcSdouble) and isinstance(other, ConcSdouble):
            return ConcSbool(self.value == other.value)
        return SymSbool(self.z3_expr() == other.z3_expr())

    def __ne__(self, other):
        return ~(self == other)

    def __hash__(self):
        if isinstance(self, ConcSdouble):
            return hash(("ConcSdouble", self.value))
        return id(self)


class ConcSdouble(Sdouble):
    __slots__ = ("value",)

    def __init__(self, value: float):
        self.value = float(value)

    def z3_expr(self):
        return z3.RealVal(self.value)

    def __repr__(self):
        return f"ConcSdouble({self.value})"


class SymSdouble(Sdouble):
    __slots__ = ("_z3", "_name")

    def __init__(self, z3_expr, name: str | None = None):
        self._z3 = z3_expr
        self._name = name

    def z3_expr(self):
        return self._z3

    def __repr__(self):
        return f"SymSdouble({self._name or self._z3})"
