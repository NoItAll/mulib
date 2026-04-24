"""Pure-data AST nodes representing mathematical expressions.

These nodes form an immutable expression tree.  No solver (Z3 or otherwise)
is imported here — translation to a solver's representation happens in a
separate layer.

Hierarchy::

    Expression (ABC)
        AbstractOperatorExpression      – binary operations on two sub-expressions
            Sum, Sub, Mul, Div, Mod
            NumericBitwiseOperation
                BitwiseAnd, BitwiseOr, BitwiseXor
                ShiftLeft, ShiftRight, LogicalShiftRight
        Neg                             – unary negation
        ExpressionIte                   – if-then-else
"""

from __future__ import annotations

import abc
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    # Constraint is defined in constraints.py; imported only for type hints to
    # avoid a circular import at runtime.
    from mulib_python.constraints import Constraint


# ---------------------------------------------------------------------------
# Abstract base
# ---------------------------------------------------------------------------

class Expression(abc.ABC):
    """Abstract base class for all expression nodes."""

    __slots__ = ()

    @property
    @abc.abstractmethod
    def is_fp(self) -> bool:
        """Return ``True`` if this expression (or any sub-expression) involves
        floating-point numbers."""

    @abc.abstractmethod
    def __repr__(self) -> str: ...

    @abc.abstractmethod
    def __eq__(self, other: object) -> bool: ...

    @abc.abstractmethod
    def __hash__(self) -> int: ...


# ---------------------------------------------------------------------------
# Binary operator expressions
# ---------------------------------------------------------------------------

class AbstractOperatorExpression(Expression):
    """Binary expression node carrying a left-hand and right-hand sub-expression.

    Sub-classes only need to specify their operator symbol via the class
    attribute ``_OP`` (used by the default ``__repr__``).
    """

    __slots__ = ("lhs", "rhs", "_hash")

    _OP: str = "?"  # operator symbol for repr; overridden in each sub-class

    def __init__(self, lhs: Expression, rhs: Expression) -> None:
        """Initialise with *lhs* and *rhs* sub-expressions."""
        if not isinstance(lhs, Expression):
            raise TypeError(f"lhs must be an Expression, got {type(lhs)!r}")
        if not isinstance(rhs, Expression):
            raise TypeError(f"rhs must be an Expression, got {type(rhs)!r}")
        object.__setattr__(self, "lhs", lhs)
        object.__setattr__(self, "rhs", rhs)
        object.__setattr__(self, "_hash", hash((type(self), lhs, rhs)))

    # Prevent mutation after construction.
    def __setattr__(self, name: str, value: object) -> None:
        raise AttributeError("Expression nodes are immutable")

    @property
    def is_fp(self) -> bool:
        return self.lhs.is_fp or self.rhs.is_fp

    def __repr__(self) -> str:
        return f"({self.lhs!r} {self._OP} {self.rhs!r})"

    def __eq__(self, other: object) -> bool:
        if not isinstance(other, type(self)):
            return NotImplemented
        return self.lhs == other.lhs and self.rhs == other.rhs

    def __hash__(self) -> int:
        return self._hash


# AbstractOperatorExpression uses __slots__ but also uses cached_property.
# cached_property stores its result in the instance __dict__, which requires
# the class to have a __dict__ (i.e. not *only* __slots__).  We therefore
# intentionally omit "__dict__" from __slots__ suppression so that the
# descriptor can function correctly, while still documenting the intended
# attributes through __slots__.


class Sum(AbstractOperatorExpression):
    """Addition: ``lhs + rhs``."""
    __slots__ = ()
    _OP = "+"


class Sub(AbstractOperatorExpression):
    """Subtraction: ``lhs - rhs``."""
    __slots__ = ()
    _OP = "-"


class Mul(AbstractOperatorExpression):
    """Multiplication: ``lhs * rhs``."""
    __slots__ = ()
    _OP = "*"


class Div(AbstractOperatorExpression):
    """Division: ``lhs / rhs``."""
    __slots__ = ()
    _OP = "/"


class Mod(AbstractOperatorExpression):
    """Modulo: ``lhs % rhs``."""
    __slots__ = ()
    _OP = "%"


# ---------------------------------------------------------------------------
# Bitwise / shift operations
# ---------------------------------------------------------------------------

class NumericBitwiseOperation(AbstractOperatorExpression):
    """Base class for bitwise and shift operations."""
    __slots__ = ()


class BitwiseAnd(NumericBitwiseOperation):
    """Bitwise AND: ``lhs & rhs``."""
    __slots__ = ()
    _OP = "&"


class BitwiseOr(NumericBitwiseOperation):
    """Bitwise OR: ``lhs | rhs``."""
    __slots__ = ()
    _OP = "|"


class BitwiseXor(NumericBitwiseOperation):
    """Bitwise XOR: ``lhs ^ rhs``."""
    __slots__ = ()
    _OP = "^"


class ShiftLeft(NumericBitwiseOperation):
    """Left shift: ``lhs << rhs``."""
    __slots__ = ()
    _OP = "<<"


class ShiftRight(NumericBitwiseOperation):
    """Arithmetic right shift: ``lhs >> rhs``."""
    __slots__ = ()
    _OP = ">>"


class LogicalShiftRight(NumericBitwiseOperation):
    """Logical (unsigned) right shift: ``lhs >>> rhs``."""
    __slots__ = ()
    _OP = ">>>"


# ---------------------------------------------------------------------------
# Unary negation
# ---------------------------------------------------------------------------

class Neg(Expression):
    """Unary arithmetic negation: ``-expr``."""

    __slots__ = ("expr", "_hash")

    def __init__(self, expr: Expression) -> None:
        if not isinstance(expr, Expression):
            raise TypeError(f"expr must be an Expression, got {type(expr)!r}")
        object.__setattr__(self, "expr", expr)
        object.__setattr__(self, "_hash", hash((Neg, expr)))

    def __setattr__(self, name: str, value: object) -> None:
        raise AttributeError("Expression nodes are immutable")

    @property
    def is_fp(self) -> bool:
        return self.expr.is_fp

    def __repr__(self) -> str:
        return f"(-{self.expr!r})"

    def __eq__(self, other: object) -> bool:
        if not isinstance(other, Neg):
            return NotImplemented
        return self.expr == other.expr

    def __hash__(self) -> int:
        return self._hash


# ---------------------------------------------------------------------------
# If-then-else expression
# ---------------------------------------------------------------------------

class ExpressionIte(Expression):
    """Numeric if-then-else: ``condition ? if_expr : else_expr``.

    Attributes
    ----------
    condition:
        A :class:`~mulib_python.constraints.Constraint` that selects which
        branch is taken.
    if_expr:
        The expression returned when *condition* is ``True``.
    else_expr:
        The expression returned when *condition* is ``False``.
    """

    __slots__ = ("condition", "if_expr", "else_expr", "_hash")

    def __init__(
        self,
        condition: "Constraint",
        if_expr: Expression,
        else_expr: Expression,
    ) -> None:
        object.__setattr__(self, "condition", condition)
        object.__setattr__(self, "if_expr", if_expr)
        object.__setattr__(self, "else_expr", else_expr)
        object.__setattr__(self, "_hash", hash((ExpressionIte, condition, if_expr, else_expr)))

    def __setattr__(self, name: str, value: object) -> None:
        raise AttributeError("Expression nodes are immutable")

    @property
    def is_fp(self) -> bool:
        return self.if_expr.is_fp or self.else_expr.is_fp

    def __repr__(self) -> str:
        return f"(ite {self.condition!r} {self.if_expr!r} {self.else_expr!r})"

    def __eq__(self, other: object) -> bool:
        if not isinstance(other, ExpressionIte):
            return NotImplemented
        return (
            self.condition == other.condition
            and self.if_expr == other.if_expr
            and self.else_expr == other.else_expr
        )

    def __hash__(self) -> int:
        return self._hash
