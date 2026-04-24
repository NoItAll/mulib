"""Boundary coercion helpers.

These functions wrap "Pythonic" inputs (``int``, ``bool``, ``float``, single
``str`` characters) into the appropriate ``ConcS*`` singleton/instance so the
mulib AST stays uniformly typed.

The interning that lives in each ``ConcS*.__new__`` (see
:mod:`mulib_python.substitutions.primitives.sint` etc.) means that a coerce
of a small value never allocates: ``to_sint(5)`` returns the cached
``ConcSint(5)`` singleton.

Imports are deferred inside each function to break the cycle
``constraints → coercion → sint → constraints``.

Type-dispatch ordering convention: every helper that maps "int-like" inputs
checks ``bool`` *before* ``int``, because ``isinstance(True, int) is True``
in Python.  Boolean inputs are routed to either ``ConcSbool`` (for
``to_sbool``) or to ``ConcSint``-style integer 0/1 (for the numeric coercers,
matching Java's "no implicit bool↔int conversion at the type level, but
boxed bools count as 0/1 when forced into an int context" rules).
"""

from __future__ import annotations

from typing import Any, Callable

# Re-export-friendly type aliases.
ExpressionLike = Any  # documented as Union[Expression, int, bool, float]
ConstraintLike = Any  # documented as Union[Constraint, bool]


# ---------------------------------------------------------------------------
# Per-type coercers
# ---------------------------------------------------------------------------

def to_sint(x: Any):
    """Coerce ``x`` to a :class:`Sint` instance."""
    from mulib_python.substitutions.primitives.sint import Sint
    if isinstance(x, Sint):
        return x
    # bool BEFORE int (bool is a subclass of int)
    if isinstance(x, bool):
        return Sint.conc_sint(1 if x else 0)
    if isinstance(x, int):
        return Sint.conc_sint(x)
    raise TypeError(f"Cannot coerce {type(x).__name__} to Sint")


def to_sbool(x: Any):
    """Coerce ``x`` to a :class:`Sbool` instance.

    ``int`` values are *not* accepted: this matches Java's strict bool/int
    separation and avoids accidentally turning a numeric expression into a
    boolean one.
    """
    from mulib_python.substitutions.primitives.sint import Sbool, ConcSbool
    if isinstance(x, Sbool):
        return x
    if isinstance(x, bool):
        return ConcSbool.TRUE if x else ConcSbool.FALSE
    raise TypeError(f"Cannot coerce {type(x).__name__} to Sbool")


def to_slong(x: Any):
    """Coerce ``x`` to a :class:`Slong` instance."""
    from mulib_python.substitutions.primitives.slong import Slong, ConcSlong
    if isinstance(x, Slong):
        return x
    if isinstance(x, bool):
        return ConcSlong(1 if x else 0)
    if isinstance(x, int):
        return ConcSlong(x)
    raise TypeError(f"Cannot coerce {type(x).__name__} to Slong")


def to_sdouble(x: Any):
    """Coerce ``x`` to a :class:`Sdouble` instance.

    Accepts ``int``/``bool``/``float`` and any existing ``Sdouble``.
    """
    from mulib_python.substitutions.primitives.sdouble import Sdouble, ConcSdouble
    if isinstance(x, Sdouble):
        return x
    if isinstance(x, bool):
        return ConcSdouble(1.0 if x else 0.0)
    if isinstance(x, (int, float)):
        return ConcSdouble(float(x))
    raise TypeError(f"Cannot coerce {type(x).__name__} to Sdouble")


def to_sfloat(x: Any):
    """Coerce ``x`` to a :class:`Sfloat` instance."""
    from mulib_python.substitutions.primitives.sfloat import Sfloat, ConcSfloat
    if isinstance(x, Sfloat):
        return x
    if isinstance(x, bool):
        return ConcSfloat(1.0 if x else 0.0)
    if isinstance(x, (int, float)):
        return ConcSfloat(float(x))
    raise TypeError(f"Cannot coerce {type(x).__name__} to Sfloat")


def to_sbyte(x: Any):
    """Coerce ``x`` to a :class:`Sbyte` instance (``int`` truncated to int8)."""
    from mulib_python.substitutions.primitives.sint import Sbyte, ConcSbyte
    if isinstance(x, Sbyte):
        return x
    if isinstance(x, bool):
        return ConcSbyte(1 if x else 0)
    if isinstance(x, int):
        return ConcSbyte(x)
    raise TypeError(f"Cannot coerce {type(x).__name__} to Sbyte")


def to_sshort(x: Any):
    """Coerce ``x`` to a :class:`Sshort` instance (``int`` truncated to int16)."""
    from mulib_python.substitutions.primitives.sint import Sshort, ConcSshort
    if isinstance(x, Sshort):
        return x
    if isinstance(x, bool):
        return ConcSshort(1 if x else 0)
    if isinstance(x, int):
        return ConcSshort(x)
    raise TypeError(f"Cannot coerce {type(x).__name__} to Sshort")


def to_schar(x: Any):
    """Coerce ``x`` to a :class:`Schar` instance.

    Accepts an ``int`` codepoint or a single-character ``str``.
    """
    from mulib_python.substitutions.primitives.sint import Schar, ConcSchar
    if isinstance(x, Schar):
        return x
    if isinstance(x, bool):
        return ConcSchar(1 if x else 0)
    if isinstance(x, int):
        return ConcSchar(x)
    if isinstance(x, str):
        if len(x) != 1:
            raise TypeError(
                f"Cannot coerce {x!r} to Schar: expected a single character"
            )
        return ConcSchar(ord(x))
    raise TypeError(f"Cannot coerce {type(x).__name__} to Schar")


# ---------------------------------------------------------------------------
# Generic dispatchers used by constraint-AST constructors
# ---------------------------------------------------------------------------

def to_expression(x: Any):
    """Coerce ``x`` to an :class:`Expression` operand.

    Dispatch table:

    * already an :class:`Expression` (which includes every ``Snumber`` and
      every ``Sbool``)  → returned as-is
    * ``bool``  → :class:`ConcSbool`
    * ``int``   → :class:`ConcSint`
    * ``float`` → :class:`ConcSdouble` (Python ``float`` is IEEE-754
      double-precision, so promoting to ``Sdouble`` preserves the value
      exactly; users wanting an ``Sfloat`` must wrap explicitly)

    ``None`` and ``str`` are rejected because they have no unambiguous
    Expression form (use :func:`to_schar` for chars).
    """
    from mulib_python.expressions import Expression
    if isinstance(x, Expression):
        return x
    # bool BEFORE int
    if isinstance(x, bool):
        from mulib_python.substitutions.primitives.sint import ConcSbool
        return ConcSbool.TRUE if x else ConcSbool.FALSE
    if isinstance(x, int):
        from mulib_python.substitutions.primitives.sint import Sint
        return Sint.conc_sint(x)
    if isinstance(x, float):
        from mulib_python.substitutions.primitives.sdouble import ConcSdouble
        return ConcSdouble(x)
    if x is None:
        raise TypeError("None is not a valid Expression operand")
    raise TypeError(f"Cannot coerce {type(x).__name__} to Expression")


def to_constraint(x: Any):
    """Coerce ``x`` to a :class:`Constraint`.

    ``True``/``False`` are mapped to the singleton ``TRUE``/``FALSE``
    constraint literals; any existing :class:`Constraint` (which includes
    every :class:`Sbool`) is returned as-is.
    """
    from mulib_python.constraints import Constraint, TRUE, FALSE
    if isinstance(x, Constraint):
        return x
    if x is True:
        return TRUE
    if x is False:
        return FALSE
    if isinstance(x, bool):  # safety net for bool subclasses
        return TRUE if x else FALSE
    raise TypeError(f"Cannot coerce {type(x).__name__} to Constraint")


def _value_coercer_for(value_type: type) -> Callable[[Any], Any]:
    """Pick the right coercer for an array's element type.

    ``int``/``bool``/``float`` map to the corresponding ``to_s*`` helper.
    Any other ``value_type`` (e.g. a partner-class type) returns
    :func:`to_expression`, which accepts already-built Expression operands
    but rejects raw Python primitives that cannot be unambiguously typed.
    """
    if value_type is int:
        return to_sint
    if value_type is bool:
        return to_sbool
    if value_type is float:
        return to_sdouble
    return to_expression
