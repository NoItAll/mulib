"""Pure-data AST nodes representing boolean / logical constraints.

These nodes form an immutable constraint tree.  No solver (Z3 or otherwise)
is imported here.

Hierarchy::

    Constraint (ABC)

    Logical:
        AbstractTwoSidedConstraint      – binary logical connectives
            And, Or, Xor, Implication, Equivalence
        Not                             – logical negation
        BoolIte                         – boolean if-then-else

    Numeric comparison:
        AbstractTwoSidedMathematicalConstraint
            Lt, Lte, Eq
        In                              – set membership

    Array / partner-class stubs (fleshed out in Phase 4):
        ArrayConstraint
        ArrayAccessConstraint
        ArrayInitializationConstraint
        PartnerClassObjectConstraint
"""

from __future__ import annotations

import abc
import enum
import functools
from typing import Tuple, TYPE_CHECKING

if TYPE_CHECKING:
    from mulib_python.expressions import Expression


# ---------------------------------------------------------------------------
# Abstract base
# ---------------------------------------------------------------------------

class Constraint(abc.ABC):
    """Abstract base class for all constraint nodes.

    :class:`~mulib_python.substitutions.Sbool` (defined in a later phase)
    will also implement this interface.
    """

    __slots__ = ()

    @abc.abstractmethod
    def __repr__(self) -> str: ...

    @abc.abstractmethod
    def __eq__(self, other: object) -> bool: ...

    @abc.abstractmethod
    def __hash__(self) -> int: ...


# ---------------------------------------------------------------------------
# Singletons for TRUE / FALSE (used by simplification helpers)
# ---------------------------------------------------------------------------

class _BoolLiteral(Constraint):
    """Internal singleton for the constant ``True`` / ``False`` constraints."""

    __slots__ = ("_value", "_hash")

    def __init__(self, value: bool) -> None:
        object.__setattr__(self, "_value", value)
        object.__setattr__(self, "_hash", hash((_BoolLiteral, value)))

    def __setattr__(self, name: str, value: object) -> None:
        raise AttributeError("Constraint nodes are immutable")

    @property
    def value(self) -> bool:
        return self._value

    def __repr__(self) -> str:
        return "TRUE" if self._value else "FALSE"

    def __eq__(self, other: object) -> bool:
        if not isinstance(other, _BoolLiteral):
            return NotImplemented
        return self._value == other._value

    def __hash__(self) -> int:
        return self._hash


#: Singleton representing the constant ``True`` constraint.
TRUE: Constraint = _BoolLiteral(True)
#: Singleton representing the constant ``False`` constraint.
FALSE: Constraint = _BoolLiteral(False)


# ---------------------------------------------------------------------------
# Binary logical connectives
# ---------------------------------------------------------------------------

class AbstractTwoSidedConstraint(Constraint):
    """Binary logical constraint with a left-hand and right-hand side.

    Sub-classes expose a ``_OP`` class attribute (operator symbol) used by
    the default ``__repr__`` and may override :py:meth:`new_instance` for
    algebraic simplification.
    """

    __slots__ = ("lhs", "rhs", "_hash")

    _OP: str = "?"

    def __init__(self, lhs: Constraint, rhs: Constraint) -> None:
        if not isinstance(lhs, Constraint):
            raise TypeError(f"lhs must be a Constraint, got {type(lhs)!r}")
        if not isinstance(rhs, Constraint):
            raise TypeError(f"rhs must be a Constraint, got {type(rhs)!r}")
        object.__setattr__(self, "lhs", lhs)
        object.__setattr__(self, "rhs", rhs)
        object.__setattr__(self, "_hash", hash((type(self), lhs, rhs)))

    def __setattr__(self, name: str, value: object) -> None:
        raise AttributeError("Constraint nodes are immutable")

    @classmethod
    def new_instance(cls, lhs: Constraint, rhs: Constraint) -> Constraint:
        """Create an instance, potentially with algebraic simplification.

        Sub-classes override this method to implement simplification rules
        (e.g. ``And.new_instance(TRUE, x)`` returns ``x``).
        """
        return cls(lhs, rhs)

    def __repr__(self) -> str:
        return f"({self.lhs!r} {self._OP} {self.rhs!r})"

    def __eq__(self, other: object) -> bool:
        if not isinstance(other, type(self)):
            return NotImplemented
        return self.lhs == other.lhs and self.rhs == other.rhs

    def __hash__(self) -> int:
        return self._hash


class And(AbstractTwoSidedConstraint):
    """Logical conjunction: ``lhs ∧ rhs``."""

    __slots__ = ()
    _OP = "&&"

    @classmethod
    def new_instance(cls, lhs: Constraint, rhs: Constraint) -> Constraint:
        """Return a simplified conjunction.

        * ``And.new_instance(TRUE, x)``  → ``x``
        * ``And.new_instance(x, TRUE)``  → ``x``
        * ``And.new_instance(FALSE, x)`` → ``FALSE``
        * ``And.new_instance(x, FALSE)`` → ``FALSE``
        * Otherwise creates a new :class:`And` node.
        """
        if lhs is TRUE:
            return rhs
        if rhs is TRUE:
            return lhs
        if lhs is FALSE or rhs is FALSE:
            return FALSE
        return cls(lhs, rhs)


class Or(AbstractTwoSidedConstraint):
    """Logical disjunction: ``lhs ∨ rhs``."""

    __slots__ = ()
    _OP = "||"

    @classmethod
    def new_instance(cls, lhs: Constraint, rhs: Constraint) -> Constraint:
        """Return a simplified disjunction.

        * ``Or.new_instance(FALSE, x)``  → ``x``
        * ``Or.new_instance(x, FALSE)``  → ``x``
        * ``Or.new_instance(TRUE, x)``   → ``TRUE``
        * ``Or.new_instance(x, TRUE)``   → ``TRUE``
        * Otherwise creates a new :class:`Or` node.
        """
        if lhs is FALSE:
            return rhs
        if rhs is FALSE:
            return lhs
        if lhs is TRUE or rhs is TRUE:
            return TRUE
        return cls(lhs, rhs)


class Xor(AbstractTwoSidedConstraint):
    """Logical exclusive-or: ``lhs ⊕ rhs``."""

    __slots__ = ()
    _OP = "^"


class Implication(AbstractTwoSidedConstraint):
    """Logical implication: ``lhs → rhs`` (i.e. ``¬lhs ∨ rhs``)."""

    __slots__ = ()
    _OP = "=>"


class Equivalence(AbstractTwoSidedConstraint):
    """Logical bi-implication: ``lhs ↔ rhs``."""

    __slots__ = ()
    _OP = "<=>"


# ---------------------------------------------------------------------------
# Unary logical negation
# ---------------------------------------------------------------------------

class Not(Constraint):
    """Logical negation: ``¬constraint``."""

    __slots__ = ("constraint", "_hash")

    def __init__(self, constraint: Constraint) -> None:
        if not isinstance(constraint, Constraint):
            raise TypeError(f"constraint must be a Constraint, got {type(constraint)!r}")
        object.__setattr__(self, "constraint", constraint)
        object.__setattr__(self, "_hash", hash((Not, constraint)))

    def __setattr__(self, name: str, value: object) -> None:
        raise AttributeError("Constraint nodes are immutable")

    def __repr__(self) -> str:
        return f"(¬{self.constraint!r})"

    def __eq__(self, other: object) -> bool:
        if not isinstance(other, Not):
            return NotImplemented
        return self.constraint == other.constraint

    def __hash__(self) -> int:
        return self._hash


# ---------------------------------------------------------------------------
# Boolean if-then-else
# ---------------------------------------------------------------------------

class BoolIte(Constraint):
    """Boolean if-then-else: ``condition ? if_case : else_case``.

    Attributes
    ----------
    condition:
        Selects which branch is taken.
    if_case:
        The constraint returned when *condition* is ``True``.
    else_case:
        The constraint returned when *condition* is ``False``.
    """

    __slots__ = ("condition", "if_case", "else_case", "_hash")

    def __init__(
        self,
        condition: Constraint,
        if_case: Constraint,
        else_case: Constraint,
    ) -> None:
        for name, val in (("condition", condition), ("if_case", if_case), ("else_case", else_case)):
            if not isinstance(val, Constraint):
                raise TypeError(f"{name} must be a Constraint, got {type(val)!r}")
        object.__setattr__(self, "condition", condition)
        object.__setattr__(self, "if_case", if_case)
        object.__setattr__(self, "else_case", else_case)
        object.__setattr__(self, "_hash", hash((BoolIte, condition, if_case, else_case)))

    def __setattr__(self, name: str, value: object) -> None:
        raise AttributeError("Constraint nodes are immutable")

    def __repr__(self) -> str:
        return f"(ite {self.condition!r} {self.if_case!r} {self.else_case!r})"

    def __eq__(self, other: object) -> bool:
        if not isinstance(other, BoolIte):
            return NotImplemented
        return (
            self.condition == other.condition
            and self.if_case == other.if_case
            and self.else_case == other.else_case
        )

    def __hash__(self) -> int:
        return self._hash


# ---------------------------------------------------------------------------
# Numeric comparison constraints
# ---------------------------------------------------------------------------

class AbstractTwoSidedMathematicalConstraint(Constraint):
    """Binary numeric comparison constraint.

    Attributes
    ----------
    lhs:
        Left-hand :class:`~mulib_python.expressions.Expression`.
    rhs:
        Right-hand :class:`~mulib_python.expressions.Expression`.
    """

    __slots__ = ("lhs", "rhs", "_hash")

    _OP: str = "?"

    def __init__(self, lhs: "Expression", rhs: "Expression") -> None:
        object.__setattr__(self, "lhs", lhs)
        object.__setattr__(self, "rhs", rhs)
        object.__setattr__(self, "_hash", hash((type(self), lhs, rhs)))

    def __setattr__(self, name: str, value: object) -> None:
        raise AttributeError("Constraint nodes are immutable")

    def __repr__(self) -> str:
        return f"({self.lhs!r} {self._OP} {self.rhs!r})"

    def __eq__(self, other: object) -> bool:
        if not isinstance(other, type(self)):
            return NotImplemented
        return self.lhs == other.lhs and self.rhs == other.rhs

    def __hash__(self) -> int:
        return self._hash


class Lt(AbstractTwoSidedMathematicalConstraint):
    """Strict less-than: ``lhs < rhs``."""

    __slots__ = ()
    _OP = "<"


class Lte(AbstractTwoSidedMathematicalConstraint):
    """Less-than-or-equal: ``lhs ≤ rhs``."""

    __slots__ = ()
    _OP = "<="


class Eq(AbstractTwoSidedMathematicalConstraint):
    """Equality: ``lhs == rhs``.

    Inequality (``lhs != rhs``) is represented as ``Not(Eq(lhs, rhs))``.
    """

    __slots__ = ()
    _OP = "=="


# ---------------------------------------------------------------------------
# Set-membership constraint
# ---------------------------------------------------------------------------

class In(Constraint):
    """Set-membership constraint: ``element ∈ set``.

    Attributes
    ----------
    element:
        The expression whose membership is tested.
    set:
        An immutable tuple of :class:`~mulib_python.expressions.Expression`
        values that form the set.
    """

    __slots__ = ("element", "set", "_hash")

    def __init__(self, element: "Expression", set: Tuple["Expression", ...]) -> None:
        object.__setattr__(self, "element", element)
        object.__setattr__(self, "set", tuple(set))
        object.__setattr__(self, "_hash", hash((In, element, tuple(set))))

    def __setattr__(self, name: str, value: object) -> None:
        raise AttributeError("Constraint nodes are immutable")

    def __repr__(self) -> str:
        return f"({self.element!r} ∈ {{{', '.join(repr(e) for e in self.set)}}})"

    def __eq__(self, other: object) -> bool:
        if not isinstance(other, In):
            return NotImplemented
        return self.element == other.element and self.set == other.set

    def __hash__(self) -> int:
        return self._hash


# ---------------------------------------------------------------------------
# Array / partner-class constraint stubs  (Phase 4 will flesh these out)
# ---------------------------------------------------------------------------

class ArrayConstraint(Constraint):
    """Base for array-related constraints.

    Attributes
    ----------
    partner_class_object_id:
        An :class:`~mulib_python.expressions.Expression` (typically an Sint)
        identifying the symbolic array object.
    index:
        An :class:`~mulib_python.expressions.Expression` representing the
        array index.
    """

    __slots__ = ("partner_class_object_id", "index", "_hash")

    def __init__(
        self,
        partner_class_object_id: "Expression",
        index: "Expression",
    ) -> None:
        object.__setattr__(self, "partner_class_object_id", partner_class_object_id)
        object.__setattr__(self, "index", index)
        object.__setattr__(self, "_hash", hash((type(self), partner_class_object_id, index)))

    def __setattr__(self, name: str, value: object) -> None:
        raise AttributeError("Constraint nodes are immutable")

    def __repr__(self) -> str:
        return f"{type(self).__name__}(id={self.partner_class_object_id!r}, index={self.index!r})"

    def __eq__(self, other: object) -> bool:
        if not isinstance(other, type(self)):
            return NotImplemented
        return (
            self.partner_class_object_id == other.partner_class_object_id
            and self.index == other.index
        )

    def __hash__(self) -> int:
        return self._hash

    @property
    def array_id(self) -> str:
        """Stable string identifier for the array.

        Derived from :attr:`partner_class_object_id`:

        * a concrete :class:`~mulib_python.substitutions.primitives.sint.ConcSint`
          contributes its integer ``value``;
        * a symbolic :class:`~mulib_python.substitutions.primitives.sint.SymSintLeaf`
          contributes its variable ``id`` (already namespaced as ``Sint…``).

        Two constraints that share the same ``partner_class_object_id``
        therefore share the same ``array_id``.

        Falling back to ``repr`` for arbitrary Expression nodes was a
        footgun: two structurally distinct expressions can share a printed
        form, which would alias them in the solver-state registry.  Such
        expressions are therefore rejected explicitly.
        """
        # Local import to avoid a hard import cycle at module load.
        from mulib_python.substitutions.primitives.sint import (
            ConcSint, SymSintLeaf,
        )
        pid = self.partner_class_object_id
        if isinstance(pid, ConcSint):
            return f"arr#c:{pid.value}"
        if isinstance(pid, SymSintLeaf):
            return f"arr#s:{pid.id}"
        raise TypeError(
            f"array_id is only defined for ConcSint or SymSintLeaf "
            f"partner_class_object_id values; got {type(pid).__name__}"
        )


class ArrayAccessConstraint(ArrayConstraint):
    """Records a STORE or SELECT operation on a symbolic array.

    Attributes
    ----------
    type:
        Whether this is a ``STORE`` or ``SELECT`` operation.
    value:
        The expression stored or selected.
    """

    class Type(enum.Enum):
        STORE = "STORE"
        SELECT = "SELECT"

    __slots__ = ("type", "value", "_hash")

    def __init__(
        self,
        partner_class_object_id: "Expression",
        index: "Expression",
        type: "ArrayAccessConstraint.Type",
        value: object,
    ) -> None:
        super().__init__(partner_class_object_id, index)
        object.__setattr__(self, "type", type)
        object.__setattr__(self, "value", value)
        object.__setattr__(self, "_hash", hash((ArrayAccessConstraint, partner_class_object_id, index, type, value)))

    def __repr__(self) -> str:
        return (
            f"ArrayAccessConstraint(id={self.partner_class_object_id!r},"
            f" index={self.index!r}, type={self.type.value}, value={self.value!r})"
        )

    def __eq__(self, other: object) -> bool:
        if not isinstance(other, ArrayAccessConstraint):
            return NotImplemented
        return (
            self.partner_class_object_id == other.partner_class_object_id
            and self.index == other.index
            and self.type == other.type
            and self.value == other.value
        )

    def __hash__(self) -> int:
        return self._hash

    @property
    def is_store(self) -> bool:
        """``True`` for STORE accesses, ``False`` for SELECT accesses."""
        return self.type is ArrayAccessConstraint.Type.STORE


class ArrayInitializationConstraint(ArrayConstraint):
    """Records the initialization of a symbolic array.

    Attributes
    ----------
    value_type:
        The Python type of array elements.
    length:
        An :class:`~mulib_python.expressions.Expression` representing the
        array length.
    default_value:
        Optional default value for indices that are never written to.  May
        be ``None`` to let the solver representation pick a type-appropriate
        default (e.g. ``0`` for ints, the null sentinel for partner-class
        arrays).
    initial_values:
        Optional mapping ``{index: value}`` of values that the array is
        known to hold at construction time.  ``None`` means "no fixed
        initial values".
    """

    __slots__ = (
        "value_type", "length", "default_value", "initial_values", "_hash",
    )

    def __init__(
        self,
        partner_class_object_id: "Expression",
        index: "Expression",
        value_type: type,
        length: "Expression",
        default_value: object = None,
        initial_values: object = None,
    ) -> None:
        super().__init__(partner_class_object_id, index)
        object.__setattr__(self, "value_type", value_type)
        object.__setattr__(self, "length", length)
        object.__setattr__(self, "default_value", default_value)
        # Freeze a copy of any provided mapping so the constraint stays
        # immutable.  We hash via a tuple of items in *insertion order*
        # rather than sorted order, because keys (array indices) are not
        # required to be orderable in general — a future caller may pass
        # symbolic-int keys or other non-comparable values.  Two
        # ``initial_values`` mappings that compare equal may therefore have
        # different hashes if their insertion orders differ; this is a
        # deliberate trade-off (correct equality, conservative hashing).
        if initial_values is None:
            frozen_initials = None
            hash_initials: object = None
        else:
            frozen_initials = dict(initial_values)
            hash_initials = tuple(frozen_initials.items())
        object.__setattr__(self, "initial_values", frozen_initials)
        object.__setattr__(
            self,
            "_hash",
            hash((
                ArrayInitializationConstraint,
                partner_class_object_id,
                index,
                value_type,
                length,
                default_value,
                hash_initials,
            )),
        )

    def __repr__(self) -> str:
        return (
            f"ArrayInitializationConstraint(id={self.partner_class_object_id!r},"
            f" index={self.index!r}, value_type={self.value_type.__name__},"
            f" length={self.length!r})"
        )

    def __eq__(self, other: object) -> bool:
        if not isinstance(other, ArrayInitializationConstraint):
            return NotImplemented
        return (
            self.partner_class_object_id == other.partner_class_object_id
            and self.index == other.index
            and self.value_type == other.value_type
            and self.length == other.length
            and self.default_value == other.default_value
            and self.initial_values == other.initial_values
        )

    def __hash__(self) -> int:
        return self._hash

    @property
    def element_type(self) -> type:
        """Alias for :attr:`value_type` matching the solver-manager API."""
        return self.value_type


class PartnerClassObjectConstraint(Constraint):
    """Base stub for constraints over symbolic object fields.

    Will be fleshed out in Phase 4.
    """

    __slots__ = ("partner_class_object_id", "_hash")

    def __init__(self, partner_class_object_id: "Expression") -> None:
        object.__setattr__(self, "partner_class_object_id", partner_class_object_id)
        object.__setattr__(self, "_hash", hash((type(self), partner_class_object_id)))

    def __setattr__(self, name: str, value: object) -> None:
        raise AttributeError("Constraint nodes are immutable")

    def __repr__(self) -> str:
        return f"{type(self).__name__}(id={self.partner_class_object_id!r})"

    def __eq__(self, other: object) -> bool:
        if not isinstance(other, type(self)):
            return NotImplemented
        return self.partner_class_object_id == other.partner_class_object_id

    def __hash__(self) -> int:
        return self._hash
