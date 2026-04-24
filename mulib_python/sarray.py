"""Symbolic arrays backed by Z3 array theory."""

from __future__ import annotations

import itertools

import z3

from .primitives import (
    Sint, ConcSint, SymSint,
    Sbool, ConcSbool, SymSbool,
    Sdouble, SymSdouble, ConcSdouble,
    _wrap_int,
)
from .exceptions import MulibIllegalStateException

_ARRAY_COUNTER = itertools.count()


def _next_array_id() -> int:
    return next(_ARRAY_COUNTER)


_KIND_SORTS = {
    "int": (z3.IntSort, lambda v: SymSint(v)),
    "bool": (z3.BoolSort, lambda v: SymSbool(v)),
    "double": (z3.RealSort, lambda v: SymSdouble(v)),
}


class Sarray:
    """Symbolic array of fixed (possibly symbolic) length."""

    def __init__(self, length, default_value="int", name: str | None = None):
        if isinstance(length, int):
            self.length = ConcSint(length)
        elif isinstance(length, Sint):
            self.length = length
        else:
            raise TypeError("length must be int or Sint")

        # Determine element kind
        if isinstance(default_value, str):
            kind = default_value
        elif default_value in (Sint, int):
            kind = "int"
        elif default_value in (Sbool, bool):
            kind = "bool"
        elif default_value in (Sdouble, float):
            kind = "double"
        else:
            raise TypeError(f"Unsupported default_value: {default_value!r}")

        if kind not in _KIND_SORTS:
            raise ValueError(f"Unknown element kind: {kind}")
        self._kind = kind
        sort_factory, wrap = _KIND_SORTS[kind]
        self._wrap = wrap
        self._array_id = _next_array_id()
        self._name = name or f"arr_{self._array_id}"
        self._z3 = z3.Array(self._name, z3.IntSort(), sort_factory())

    # ---- bounds checking --------------------------------------------------
    def _check_bounds(self, idx: Sint):
        from .api import assume
        assume(idx >= 0)
        assume(idx < self.length)

    def __len__(self):
        if isinstance(self.length, ConcSint):
            return self.length.value
        raise MulibIllegalStateException("Symbolic length array has no concrete len()")

    def __getitem__(self, index):
        idx = _wrap_int(index)
        self._check_bounds(idx)
        return self._wrap(z3.Select(self._z3, idx.z3_expr()))

    def __setitem__(self, index, value):
        idx = _wrap_int(index)
        self._check_bounds(idx)
        if self._kind == "int":
            v = _wrap_int(value).z3_expr()
        elif self._kind == "bool":
            from .primitives import _wrap_bool
            v = _wrap_bool(value).z3_expr()
        else:
            from .primitives import _wrap_double
            v = _wrap_double(value).z3_expr()
        self._z3 = z3.Store(self._z3, idx.z3_expr(), v)

    def __repr__(self):
        return f"Sarray(name={self._name}, length={self.length}, kind={self._kind})"
