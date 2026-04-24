import z3

from mulib_python.solver import Z3SolverManager
from mulib_python.primitives import SymSint


def test_check_sat_and_label():
    m = Z3SolverManager()
    x = SymSint(z3.Int("x"), name="x")
    m.add(x < 5)
    m.add(x > 1)
    m.label("x", x)
    assert m.check() is True
    labels = m.labels()
    assert 1 < labels["x"] < 5


def test_push_pop():
    m = Z3SolverManager()
    x = z3.Int("x")
    m.add(x > 0)
    m.push()
    m.add(x < 0)
    assert not m.check()
    m.pop()
    assert m.check()


def test_array_store_select():
    arr = z3.Array("a", z3.IntSort(), z3.IntSort())
    arr2 = z3.Store(arr, 0, 42)
    s = z3.Solver()
    v = z3.Int("v")
    s.add(v == z3.Select(arr2, 0))
    assert s.check() == z3.sat
    assert s.model()[v].as_long() == 42
