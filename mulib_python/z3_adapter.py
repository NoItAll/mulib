"""Z3 adapter - translates mulib AST to Z3 expressions."""

from __future__ import annotations

from typing import Any, Dict, Optional, TYPE_CHECKING

import z3

from mulib_python.expressions import (
    Expression, Sum, Sub, Mul, Div, Mod, Neg,
    BitwiseAnd, BitwiseOr, BitwiseXor,
    ShiftLeft, ShiftRight, LogicalShiftRight,
    ExpressionIte,
)
from mulib_python.constraints import (
    Constraint, And, Or, Not, Xor, Implication, Equivalence,
    Lt, Lte, Eq, In, BoolIte, TRUE, FALSE, _BoolLiteral,
    ArrayAccessConstraint, ArrayInitializationConstraint,
    PartnerClassObjectConstraint,
)

if TYPE_CHECKING:
    pass


class Z3MulibAdapter:
    """Translates mulib AST nodes to Z3 expressions.

    Each adapter is bound to a single :class:`z3.Context`.  Threads that
    need to solve concurrently each create their own adapter (and hence
    their own Z3 context) so that Z3's per-context C state stays
    thread-local.  This mirrors the Java implementation, which gives each
    ``Z3SolverManager`` an independent ``Context``.
    """

    def __init__(
        self,
        treat_bools_as_ints: bool = False,
        ctx: Optional[z3.Context] = None,
    ) -> None:
        self._cache: Dict[int, z3.ExprRef] = {}
        self._const_cache: Dict[str, z3.ExprRef] = {}
        self._treat_bools_as_ints = treat_bools_as_ints
        self._ctx: z3.Context = ctx if ctx is not None else z3.Context()

    @property
    def ctx(self) -> z3.Context:
        return self._ctx

    def clear_cache(self) -> None:
        """Clear the translation cache."""
        self._cache.clear()

    def translate(self, node: Any) -> z3.ExprRef:
        """Translate a mulib node to a Z3 expression.

        Note: We don't cache by id() because Python can reuse IDs after
        garbage collection, leading to stale cache hits.
        """
        return self._translate_impl(node)

    def _translate_impl(self, node: Any) -> z3.ExprRef:
        """Internal translation implementation."""
        ctx = self._ctx
        # Prologue: accept raw Python primitives directly.  This makes the
        # adapter robust when callers (or constraints that haven't coerced
        # for some reason) pass a bare value through.  ``bool`` is checked
        # before ``int`` because ``bool`` is a subclass of ``int`` in
        # Python.
        if node is True or node is False or isinstance(node, bool):
            if self._treat_bools_as_ints:
                return z3.IntVal(1 if node else 0, ctx=ctx)
            return z3.BoolVal(bool(node), ctx=ctx)
        if isinstance(node, int):
            return z3.IntVal(node, ctx=ctx)
        if isinstance(node, float):
            return z3.RealVal(node, ctx=ctx)

        # Handle constraint literals
        if node is TRUE or (isinstance(node, _BoolLiteral) and node._value):
            return z3.BoolVal(True, ctx=ctx)
        if node is FALSE or (isinstance(node, _BoolLiteral) and not node._value):
            return z3.BoolVal(False, ctx=ctx)

        # Handle concrete primitive values
        from mulib_python.substitutions.primitives.sint import (
            ConcSint, ConcSbool, ConcSbyte, ConcSchar, ConcSshort
        )
        from mulib_python.substitutions.primitives.slong import ConcSlong
        from mulib_python.substitutions.primitives.sdouble import ConcSdouble
        from mulib_python.substitutions.primitives.sfloat import ConcSfloat

        if isinstance(node, ConcSbool):
            if self._treat_bools_as_ints:
                return z3.IntVal(1 if node._value else 0, ctx=ctx)
            return z3.BoolVal(node._value, ctx=ctx)

        if isinstance(node, (ConcSint, ConcSbyte, ConcSchar, ConcSshort)):
            return z3.IntVal(node._value, ctx=ctx)

        if isinstance(node, ConcSlong):
            return z3.IntVal(node._value, ctx=ctx)

        if isinstance(node, (ConcSdouble, ConcSfloat)):
            return z3.RealVal(node._value, ctx=ctx)

        # Handle symbolic leaf variables
        from mulib_python.substitutions.primitives.sint import (
            SymSintLeaf, SymSboolLeaf, SymSbyteLeaf, SymScharLeaf, SymSshortLeaf
        )
        from mulib_python.substitutions.primitives.slong import SymSlongLeaf
        from mulib_python.substitutions.primitives.sdouble import SymSdoubleLeaf
        from mulib_python.substitutions.primitives.sfloat import SymSfloatLeaf

        if isinstance(node, SymSboolLeaf):
            if node._id not in self._const_cache:
                if self._treat_bools_as_ints:
                    self._const_cache[node._id] = z3.Int(node._id, ctx=ctx)
                else:
                    self._const_cache[node._id] = z3.Bool(node._id, ctx=ctx)
            return self._const_cache[node._id]

        if isinstance(node, (SymSintLeaf, SymSbyteLeaf, SymScharLeaf, SymSshortLeaf)):
            if node._id not in self._const_cache:
                self._const_cache[node._id] = z3.Int(node._id, ctx=ctx)
            return self._const_cache[node._id]

        if isinstance(node, SymSlongLeaf):
            if node._id not in self._const_cache:
                self._const_cache[node._id] = z3.Int(node._id, ctx=ctx)
            return self._const_cache[node._id]

        if isinstance(node, (SymSdoubleLeaf, SymSfloatLeaf)):
            if node._id not in self._const_cache:
                self._const_cache[node._id] = z3.Real(node._id, ctx=ctx)
            return self._const_cache[node._id]

        # Handle symbolic wrappers
        from mulib_python.substitutions.primitives.sint import (
            SymSint, SymSbool, SymSbyte, SymSchar, SymSshort
        )
        from mulib_python.substitutions.primitives.slong import SymSlong
        from mulib_python.substitutions.primitives.sdouble import SymSdouble
        from mulib_python.substitutions.primitives.sfloat import SymSfloat

        if isinstance(node, SymSbool):
            return self.translate(node._represented_constraint)

        if isinstance(node, (SymSint, SymSbyte, SymSchar, SymSshort)):
            return self.translate(node._represented_expression)

        if isinstance(node, SymSlong):
            return self.translate(node._represented_expression)

        if isinstance(node, (SymSdouble, SymSfloat)):
            return self.translate(node._represented_expression)

        # Handle expression nodes
        if isinstance(node, Sum):
            return self.translate(node.lhs) + self.translate(node.rhs)

        if isinstance(node, Sub):
            return self.translate(node.lhs) - self.translate(node.rhs)

        if isinstance(node, Mul):
            return self.translate(node.lhs) * self.translate(node.rhs)

        if isinstance(node, Div):
            lhs = self.translate(node.lhs)
            rhs = self.translate(node.rhs)
            return lhs / rhs

        if isinstance(node, Mod):
            lhs = self.translate(node.lhs)
            rhs = self.translate(node.rhs)
            if z3.is_int(lhs) and z3.is_int(rhs):
                return lhs % rhs
            # For reals, we need to simulate mod
            return lhs - (lhs / rhs) * rhs

        if isinstance(node, Neg):
            return -self.translate(node.expr)

        # Handle bitwise operations (convert to BitVec for shifts/bitwise)
        if isinstance(node, BitwiseAnd):
            lhs = self._to_bv32(self.translate(node.lhs))
            rhs = self._to_bv32(self.translate(node.rhs))
            return z3.BV2Int(lhs & rhs)

        if isinstance(node, BitwiseOr):
            lhs = self._to_bv32(self.translate(node.lhs))
            rhs = self._to_bv32(self.translate(node.rhs))
            return z3.BV2Int(lhs | rhs)

        if isinstance(node, BitwiseXor):
            lhs = self._to_bv32(self.translate(node.lhs))
            rhs = self._to_bv32(self.translate(node.rhs))
            return z3.BV2Int(lhs ^ rhs)

        if isinstance(node, ShiftLeft):
            lhs = self._to_bv32(self.translate(node.lhs))
            rhs = self._to_bv32(self.translate(node.rhs))
            return z3.BV2Int(lhs << rhs)

        if isinstance(node, ShiftRight):
            lhs = self._to_bv32(self.translate(node.lhs))
            rhs = self._to_bv32(self.translate(node.rhs))
            return z3.BV2Int(lhs >> rhs)

        if isinstance(node, LogicalShiftRight):
            lhs = self._to_bv32(self.translate(node.lhs))
            rhs = self._to_bv32(self.translate(node.rhs))
            return z3.BV2Int(z3.LShR(lhs, rhs))

        if isinstance(node, ExpressionIte):
            cond = self.translate(node.condition)
            if_expr = self.translate(node.if_expr)
            else_expr = self.translate(node.else_expr)
            return z3.If(cond, if_expr, else_expr, ctx=ctx)

        # Handle constraint nodes
        if isinstance(node, And):
            return z3.And(self.translate(node.lhs), self.translate(node.rhs))

        if isinstance(node, Or):
            return z3.Or(self.translate(node.lhs), self.translate(node.rhs))

        if isinstance(node, Not):
            return z3.Not(self.translate(node.constraint))

        if isinstance(node, Xor):
            return z3.Xor(self.translate(node.lhs), self.translate(node.rhs))

        if isinstance(node, Implication):
            return z3.Implies(self.translate(node.lhs), self.translate(node.rhs))

        if isinstance(node, Equivalence):
            lhs = self.translate(node.lhs)
            rhs = self.translate(node.rhs)
            return lhs == rhs

        if isinstance(node, Lt):
            return self.translate(node.lhs) < self.translate(node.rhs)

        if isinstance(node, Lte):
            return self.translate(node.lhs) <= self.translate(node.rhs)

        if isinstance(node, Eq):
            return self.translate(node.lhs) == self.translate(node.rhs)

        if isinstance(node, In):
            elem = self.translate(node.element)
            clauses = [elem == self.translate(s) for s in node.set]
            return z3.Or(*clauses) if clauses else z3.BoolVal(False, ctx=ctx)

        if isinstance(node, BoolIte):
            cond = self.translate(node.condition)
            if_case = self.translate(node.if_case)
            else_case = self.translate(node.else_case)
            return z3.If(cond, if_case, else_case, ctx=ctx)

        # Array / partner-class constraints are *not* representable as a
        # single Z3 boolean formula: they update solver-side state (the
        # array history) and must therefore be routed through
        # ``Z3IncrementalSolverManager.add_array_constraint``.  Silently
        # dropping them by returning ``BoolVal(True)`` previously made
        # misrouted constraints (e.g. nested inside ``And``/``Or``) appear
        # to "succeed", which is a footgun.  Fail loudly instead.
        if isinstance(node, (ArrayAccessConstraint, ArrayInitializationConstraint, PartnerClassObjectConstraint)):
            raise TypeError(
                f"{type(node).__name__} cannot be translated as a plain Z3 "
                f"boolean; route it through "
                f"Z3IncrementalSolverManager.add_array_constraint instead."
            )

        raise TypeError(f"Cannot translate {type(node).__name__}: {node!r}")

    def _to_bv32(self, expr: z3.ExprRef) -> z3.BitVecRef:
        """Convert an integer expression to a 32-bit bitvector."""
        if z3.is_bv(expr):
            return expr
        return z3.Int2BV(expr, 32)

    def extract_value(self, model: z3.ModelRef, var: Any) -> Any:
        """Extract a concrete Python value from the Z3 model for the given variable."""
        z3_expr = self.translate(var)
        z3_val = model.eval(z3_expr, model_completion=True)
        return self._z3_to_python(z3_val, var)

    def _z3_to_python(self, z3_val: z3.ExprRef, original: Any = None) -> Any:
        """Convert a Z3 value to a Python value."""
        if z3.is_int_value(z3_val):
            return z3_val.as_long()
        
        if z3.is_bool(z3_val):
            return z3.is_true(z3_val)
        
        if z3.is_rational_value(z3_val):
            num = z3_val.numerator_as_long()
            den = z3_val.denominator_as_long()
            return num / den if den != 0 else 0.0
        
        if z3.is_algebraic_value(z3_val):
            # Approximate algebraic numbers
            return float(z3_val.approx(10).as_fraction())
        
        # Try as_long for other integer types
        try:
            return z3_val.as_long()
        except (AttributeError, z3.Z3Exception):
            pass
        
        # Return string representation as fallback
        return str(z3_val)
