"""Tests for constraints and expressions."""

import pytest
from mulib_python.constraints import (
    Constraint, And, Or, Not, Xor, Implication, Equivalence,
    Lt, Lte, Eq, In, BoolIte, TRUE, FALSE,
)
from mulib_python.expressions import (
    Expression, Sum, Sub, Mul, Div, Mod, Neg,
    BitwiseAnd, BitwiseOr, BitwiseXor,
    ShiftLeft, ShiftRight, LogicalShiftRight,
    ExpressionIte,
)
from mulib_python.substitutions.primitives.sint import ConcSint, SymSintLeaf


# =============================================================================
# Expression Tests
# =============================================================================

def test_sum_expression():
    x = SymSintLeaf("x")
    y = SymSintLeaf("y")
    
    expr = Sum(x, y)
    assert expr.lhs == x
    assert expr.rhs == y


def test_sub_expression():
    x = SymSintLeaf("x")
    c = ConcSint(5)
    
    expr = Sub(x, c)
    assert expr.lhs == x
    assert expr.rhs == c


def test_mul_expression():
    x = SymSintLeaf("x")
    c = ConcSint(2)
    
    expr = Mul(x, c)
    assert expr.lhs == x


def test_div_expression():
    x = SymSintLeaf("x")
    c = ConcSint(2)
    
    expr = Div(x, c)
    assert expr.lhs == x


def test_mod_expression():
    x = SymSintLeaf("x")
    c = ConcSint(3)
    
    expr = Mod(x, c)
    assert expr.lhs == x


def test_neg_expression():
    x = SymSintLeaf("x")
    
    expr = Neg(x)
    assert expr.expr == x


def test_bitwise_and_expression():
    x = SymSintLeaf("x")
    y = SymSintLeaf("y")
    
    expr = BitwiseAnd(x, y)
    assert expr.lhs == x
    assert expr.rhs == y


def test_bitwise_or_expression():
    x = SymSintLeaf("x")
    y = SymSintLeaf("y")
    
    expr = BitwiseOr(x, y)
    assert expr.lhs == x


def test_bitwise_xor_expression():
    x = SymSintLeaf("x")
    y = SymSintLeaf("y")
    
    expr = BitwiseXor(x, y)
    assert expr.lhs == x


def test_shift_left_expression():
    x = SymSintLeaf("x")
    c = ConcSint(2)
    
    expr = ShiftLeft(x, c)
    assert expr.lhs == x


def test_shift_right_expression():
    x = SymSintLeaf("x")
    c = ConcSint(2)
    
    expr = ShiftRight(x, c)
    assert expr.lhs == x


def test_logical_shift_right_expression():
    x = SymSintLeaf("x")
    c = ConcSint(2)
    
    expr = LogicalShiftRight(x, c)
    assert expr.lhs == x


def test_expression_ite():
    from mulib_python.substitutions.primitives.sint import SymSboolLeaf
    
    cond = SymSboolLeaf("b")
    x = SymSintLeaf("x")
    y = SymSintLeaf("y")
    
    expr = ExpressionIte(cond, x, y)
    assert expr.condition == cond
    assert expr.if_expr == x
    assert expr.else_expr == y


# =============================================================================
# Constraint Tests
# =============================================================================

def test_lt_constraint():
    x = SymSintLeaf("x")
    c = ConcSint(10)
    
    constraint = Lt(x, c)
    assert constraint.lhs == x
    assert constraint.rhs == c


def test_lte_constraint():
    x = SymSintLeaf("x")
    c = ConcSint(10)
    
    constraint = Lte(x, c)
    assert constraint.lhs == x


def test_eq_constraint():
    x = SymSintLeaf("x")
    y = SymSintLeaf("y")
    
    constraint = Eq(x, y)
    assert constraint.lhs == x
    assert constraint.rhs == y


def test_and_constraint():
    x = SymSintLeaf("x")
    
    c1 = Lt(x, ConcSint(10))
    c2 = Lt(ConcSint(0), x)
    
    conj = And(c1, c2)
    assert conj.lhs == c1
    assert conj.rhs == c2


def test_or_constraint():
    x = SymSintLeaf("x")
    
    c1 = Lt(x, ConcSint(0))
    c2 = Lt(ConcSint(10), x)
    
    disj = Or(c1, c2)
    assert disj.lhs == c1
    assert disj.rhs == c2


def test_not_constraint():
    x = SymSintLeaf("x")
    c = Lt(x, ConcSint(10))
    
    neg = Not(c)
    assert neg.constraint == c


def test_xor_constraint():
    from mulib_python.substitutions.primitives.sint import SymSboolLeaf
    
    b1 = SymSboolLeaf("b1")
    b2 = SymSboolLeaf("b2")
    
    xor = Xor(b1, b2)
    assert xor.lhs == b1
    assert xor.rhs == b2


def test_implication_constraint():
    from mulib_python.substitutions.primitives.sint import SymSboolLeaf
    
    b1 = SymSboolLeaf("b1")
    b2 = SymSboolLeaf("b2")
    
    impl = Implication(b1, b2)
    assert impl.lhs == b1
    assert impl.rhs == b2


def test_equivalence_constraint():
    from mulib_python.substitutions.primitives.sint import SymSboolLeaf
    
    b1 = SymSboolLeaf("b1")
    b2 = SymSboolLeaf("b2")
    
    equiv = Equivalence(b1, b2)
    assert equiv.lhs == b1
    assert equiv.rhs == b2


def test_in_constraint():
    x = SymSintLeaf("x")
    values = [ConcSint(1), ConcSint(2), ConcSint(3)]
    
    constraint = In(x, values)
    assert constraint.element == x
    assert len(constraint.set) == 3


def test_bool_ite_constraint():
    from mulib_python.substitutions.primitives.sint import SymSboolLeaf
    
    cond = SymSboolLeaf("c")
    t = SymSboolLeaf("t")
    f = SymSboolLeaf("f")
    
    ite = BoolIte(cond, t, f)
    assert ite.condition == cond
    assert ite.if_case == t
    assert ite.else_case == f


def test_true_false_singletons():
    assert TRUE is not FALSE
    # Both should be constraints
    assert isinstance(TRUE, Constraint)
    assert isinstance(FALSE, Constraint)


# =============================================================================
# Constraint Nesting Tests
# =============================================================================

def test_nested_and_or():
    x = SymSintLeaf("x")
    
    c1 = Lt(x, ConcSint(10))
    c2 = Lt(ConcSint(0), x)
    c3 = Eq(x, ConcSint(5))
    
    nested = And(Or(c1, c2), c3)
    
    assert isinstance(nested.lhs, Or)
    assert nested.rhs == c3


def test_deeply_nested():
    x = SymSintLeaf("x")
    
    base = Lt(x, ConcSint(100))
    
    # Nest several levels
    c = base
    for _ in range(5):
        c = Not(c)
    
    assert isinstance(c, Not)


# =============================================================================
# Expression with Constraint Tests
# =============================================================================

def test_expression_produces_constraint():
    x = SymSintLeaf("x")
    y = SymSintLeaf("y")
    
    # This should produce an Sbool/Constraint
    result = x < y
    
    from mulib_python.substitutions.primitives.sint import Sbool
    assert isinstance(result, Sbool)


# =============================================================================
# Repr Tests
# =============================================================================

def test_constraint_repr():
    x = SymSintLeaf("x")
    c = Lt(x, ConcSint(10))
    
    r = repr(c)
    assert "x" in r or "Sint" in r


def test_expression_repr():
    x = SymSintLeaf("x")
    y = SymSintLeaf("y")
    
    expr = Sum(x, y)
    r = repr(expr)
    assert "+" in r or "Sum" in r
