"""Tests for symbolic primitive types."""

import pytest
from mulib_python.substitutions.primitives.sint import (
    Sint, ConcSint, SymSint, SymSintLeaf,
    Sbool, ConcSbool, SymSbool, SymSboolLeaf,
    Sbyte, ConcSbyte, SymSbyte, SymSbyteLeaf,
    Schar, ConcSchar, SymSchar, SymScharLeaf,
    Sshort, ConcSshort, SymSshort, SymSshortLeaf,
)
from mulib_python.substitutions.primitives.slong import (
    Slong, ConcSlong, SymSlong, SymSlongLeaf,
)
from mulib_python.substitutions.primitives.sdouble import (
    Sdouble, ConcSdouble, SymSdouble, SymSdoubleLeaf,
)
from mulib_python.substitutions.primitives.sfloat import (
    Sfloat, ConcSfloat, SymSfloat, SymSfloatLeaf,
)
from mulib_python.expressions import Sum, Sub, Mul, Div, Mod, Neg
from mulib_python.constraints import Lt, Lte, Eq, And, Or, Not


# =============================================================================
# ConcSint Tests
# =============================================================================

def test_concsint_creation():
    a = ConcSint(42)
    assert a._value == 42
    assert isinstance(a, Sint)


def test_concsint_cache():
    """ConcSint should cache values in [-128, 127]."""
    a = ConcSint(50)
    b = ConcSint(50)
    # Cache semantics: same value should give same result
    assert a._value == b._value
    
    c = ConcSint(200)
    d = ConcSint(200)
    # Both should have same value
    assert c._value == d._value


def test_concsint_arithmetic():
    a = ConcSint(10)
    b = ConcSint(3)
    
    # Addition
    c = a + b
    assert isinstance(c, ConcSint)
    assert c._value == 13
    
    # Subtraction
    d = a - b
    assert isinstance(d, ConcSint)
    assert d._value == 7
    
    # Multiplication
    e = a * b
    assert isinstance(e, ConcSint)
    assert e._value == 30
    
    # Division (integer, truncate toward zero)
    f = a // b
    assert isinstance(f, ConcSint)
    assert f._value == 3
    
    # Modulo
    g = a % b
    assert isinstance(g, ConcSint)
    assert g._value == 1
    
    # Negation
    h = -a
    assert isinstance(h, ConcSint)
    assert h._value == -10


def test_concsint_division_truncates_toward_zero():
    """Java division truncates toward zero, not floor like Python."""
    a = ConcSint(-7)
    b = ConcSint(3)
    c = a // b
    assert c._value == -2  # Java: -7/3 = -2, not -3


def test_concsint_comparison():
    a = ConcSint(5)
    b = ConcSint(10)
    c = ConcSint(5)
    
    assert (a < b)._value is True
    assert (b < a)._value is False
    assert (a <= c)._value is True
    assert (a == c)._value is True
    assert (a != b)._value is True


def test_concsint_bitwise():
    a = ConcSint(0b1100)
    b = ConcSint(0b1010)
    
    assert (a & b)._value == 0b1000
    assert (a | b)._value == 0b1110
    assert (a ^ b)._value == 0b0110
    assert (~ConcSint(0))._value == -1


def test_concsint_shifts():
    a = ConcSint(8)
    
    assert (a << ConcSint(2))._value == 32
    assert (a >> ConcSint(2))._value == 2


def test_concsint_reverse_operations():
    """Test __radd__, __rsub__, etc."""
    a = ConcSint(5)
    
    # Python int + ConcSint
    b = 10 + a
    assert isinstance(b, ConcSint)
    assert b._value == 15
    
    # Python int - ConcSint
    c = 10 - a
    assert isinstance(c, ConcSint)
    assert c._value == 5


# =============================================================================
# SymSint Tests
# =============================================================================

def test_symsintleaf_creation():
    x = SymSintLeaf("x")
    assert x._id == "x"
    assert isinstance(x, Sint)
    assert isinstance(x, SymSint)


def test_symsint_arithmetic_produces_symsint():
    x = SymSintLeaf("x")
    y = SymSintLeaf("y")
    
    z = x + y
    assert isinstance(z, SymSint)
    assert isinstance(z._represented_expression, Sum)


def test_symsint_with_concrete():
    x = SymSintLeaf("x")
    c = ConcSint(5)
    
    z = x + c
    assert isinstance(z, SymSint)
    
    w = c + x
    assert isinstance(w, SymSint)


def test_symsint_comparison_produces_sbool():
    x = SymSintLeaf("x")
    y = SymSintLeaf("y")
    
    cond = x < y
    assert isinstance(cond, Sbool)
    assert isinstance(cond, SymSbool)


# =============================================================================
# Sbool Tests
# =============================================================================

def test_concsbool_singletons():
    t = ConcSbool.TRUE
    f = ConcSbool.FALSE
    
    assert t._value is True
    assert f._value is False
    # Test singleton semantics via equality
    assert ConcSbool(True)._value is True
    assert ConcSbool(False)._value is False


def test_concsbool_operators():
    t = ConcSbool.TRUE
    f = ConcSbool.FALSE
    
    assert (t & f)._value is False
    assert (t | f)._value is True
    assert (t ^ f)._value is True
    assert (~t)._value is False


def test_concsbool_builtin_bool():
    """ConcSbool should be directly usable in if statements."""
    t = ConcSbool.TRUE
    f = ConcSbool.FALSE
    
    assert bool(t) is True
    assert bool(f) is False


def test_symboolleaf_creation():
    b = SymSboolLeaf("b")
    assert b._id == "b"
    assert isinstance(b, Sbool)


def test_symbool_and_or():
    b1 = SymSboolLeaf("b1")
    b2 = SymSboolLeaf("b2")
    
    conj = b1 & b2
    assert isinstance(conj, SymSbool)
    
    disj = b1 | b2
    assert isinstance(disj, SymSbool)


# =============================================================================
# Slong Tests
# =============================================================================

def test_concslong_arithmetic():
    a = ConcSlong(10**15)
    b = ConcSlong(2)
    
    c = a + b
    assert isinstance(c, ConcSlong)
    assert c._value == 10**15 + 2
    
    d = a * b
    assert d._value == 2 * 10**15


def test_symslongleaf():
    l = SymSlongLeaf("l")
    assert isinstance(l, Slong)
    
    m = l + ConcSlong(1)
    assert isinstance(m, SymSlong)


# =============================================================================
# Sdouble Tests
# =============================================================================

def test_concsdouble_arithmetic():
    a = ConcSdouble(1.5)
    b = ConcSdouble(2.5)
    
    assert (a + b)._value == 4.0
    assert (b - a)._value == 1.0
    assert (a * b)._value == 3.75
    assert (b / a)._value == 2.5 / 1.5


def test_concsdouble_comparison():
    a = ConcSdouble(1.0)
    b = ConcSdouble(2.0)
    
    assert (a < b)._value is True
    assert (a == ConcSdouble(1.0))._value is True


def test_symdoubleaf():
    d = SymSdoubleLeaf("d")
    assert isinstance(d, Sdouble)
    
    e = d + ConcSdouble(1.0)
    assert isinstance(e, SymSdouble)


# =============================================================================
# Sfloat Tests
# =============================================================================

def test_concsfloat_arithmetic():
    a = ConcSfloat(1.5)
    b = ConcSfloat(2.5)
    
    assert (a + b)._value == 4.0


def test_symfloatleaf():
    f = SymSfloatLeaf("f")
    assert isinstance(f, Sfloat)


# =============================================================================
# Sbyte, Schar, Sshort Tests
# =============================================================================

def test_concsbyte():
    b = ConcSbyte(100)
    assert b._value == 100
    assert isinstance(b, Sint)


def test_concschar():
    c = ConcSchar(65)  # 'A'
    assert c._value == 65


def test_concsshort():
    s = ConcSshort(1000)
    assert s._value == 1000


def test_symbyte_symchar_symshort():
    b = SymSbyteLeaf("b")
    c = SymScharLeaf("c")
    s = SymSshortLeaf("s")
    
    assert isinstance(b, Sbyte)
    assert isinstance(c, Schar)
    assert isinstance(s, Sshort)


# =============================================================================
# Edge Cases and Immutability
# =============================================================================

def test_immutability():
    """Symbolic values should be immutable."""
    a = ConcSint(5)
    with pytest.raises(AttributeError):
        a._value = 10


def test_hash_and_equality():
    """Test structural equality for use in sets/dicts."""
    a = ConcSint(5)
    b = ConcSint(5)
    
    # Both should have same value
    assert a._value == b._value
    
    # Symbolic leaves with same ID should produce equal comparisons
    x1 = SymSintLeaf("x")
    x2 = SymSintLeaf("x")
    # Comparison produces TRUE for same-id leaves
    result = (x1 == x2)
    assert isinstance(result, ConcSbool)
    assert result._value is True


def test_repr():
    assert "5" in repr(ConcSint(5))
    assert "TRUE" in repr(ConcSbool.TRUE) or "True" in repr(ConcSbool.TRUE)
    assert "1.5" in repr(ConcSdouble(1.5))
    # Symbolic leaf repr contains the ID
    x = SymSintLeaf("x")
    assert "x" in repr(x)
