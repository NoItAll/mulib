from mulib_python.primitives import (
    ConcSint, SymSint, ConcSbool, SymSbool, ConcSdouble, SymSdouble,
)
import z3


def test_concrete_arithmetic_returns_concrete():
    a = ConcSint(3); b = ConcSint(4)
    s = a + b
    assert isinstance(s, ConcSint)
    assert s.value == 7
    assert (a * b).value == 12
    assert (b - a).value == 1
    assert (b // a).value == 1
    assert (b % a).value == 1
    assert (-a).value == -3


def test_concrete_comparison():
    a = ConcSint(3); b = ConcSint(4)
    r = a < b
    assert isinstance(r, ConcSbool)
    assert r.value is True
    assert (a == ConcSint(3)).value is True
    assert (a != b).value is True


def test_symbolic_plus_concrete_yields_symbolic():
    x = SymSint(z3.Int("x"), name="x")
    expr = x + ConcSint(1)
    assert isinstance(expr, SymSint)


def test_repr():
    assert "3" in repr(ConcSint(3))
    assert "True" in repr(ConcSbool(True))
    assert "1.5" in repr(ConcSdouble(1.5))


def test_bitwise_concrete():
    a = ConcSint(0b1100); b = ConcSint(0b1010)
    assert (a & b).value == 0b1000
    assert (a | b).value == 0b1110
    assert (a ^ b).value == 0b0110
    assert (~ConcSint(0)).value == -1


def test_bool_ops_concrete():
    t = ConcSbool(True); f = ConcSbool(False)
    assert (t & f).value is False
    assert (t | f).value is True
    assert (t ^ f).value is True
    assert (~t).value is False


def test_double_arithmetic():
    a = ConcSdouble(1.5); b = ConcSdouble(2.5)
    assert (a + b).value == 4.0
    assert (b - a).value == 1.0
    assert (a * b).value == 3.75
    assert (b / a).value == 2.5 / 1.5
