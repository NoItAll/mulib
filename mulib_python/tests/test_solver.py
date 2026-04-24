"""Tests for solver manager and Z3 integration."""

import pytest
from mulib_python.z3_solver_manager import (
    Z3IncrementalSolverManager,
    Z3GlobalLearningSolverManager,
)
from mulib_python.z3_adapter import Z3MulibAdapter
from mulib_python.substitutions.primitives.sint import (
    ConcSint, SymSintLeaf, ConcSbool, SymSboolLeaf,
)
from mulib_python.substitutions.primitives.slong import ConcSlong, SymSlongLeaf
from mulib_python.substitutions.primitives.sdouble import ConcSdouble, SymSdoubleLeaf
from mulib_python.constraints import Lt, Lte, Eq, And, Or, Not
from mulib_python.expressions import Sum, Sub, Mul
from mulib_python.solution import Labels, Solution


# =============================================================================
# Z3MulibAdapter Tests
# =============================================================================

def test_adapter_translate_concrete_int():
    adapter = Z3MulibAdapter()
    
    c = ConcSint(42)
    z3_expr = adapter.translate(c)
    
    import z3
    assert z3.is_int_value(z3_expr)
    assert z3_expr.as_long() == 42


def test_adapter_translate_symbolic_int():
    adapter = Z3MulibAdapter()
    
    x = SymSintLeaf("x")
    z3_expr = adapter.translate(x)
    
    import z3
    assert z3.is_int(z3_expr)


def test_adapter_translate_sum():
    adapter = Z3MulibAdapter()
    
    x = SymSintLeaf("x")
    y = SymSintLeaf("y")
    z = x + y
    
    z3_expr = adapter.translate(z)
    import z3
    assert str(z3_expr) == "x + y"


def test_adapter_translate_constraint_lt():
    adapter = Z3MulibAdapter()
    
    x = SymSintLeaf("x")
    c = Lt(x, ConcSint(10))
    
    z3_expr = adapter.translate(c)
    import z3
    # Z3 may normalize to "10 > x" or "x < 10" - both are equivalent
    assert z3.is_bool(z3_expr)
    assert str(z3_expr) in ("x < 10", "10 > x")


def test_adapter_translate_and_or():
    adapter = Z3MulibAdapter()
    
    x = SymSintLeaf("x")
    c1 = Lt(x, ConcSint(10))
    c2 = Lt(ConcSint(0), x)
    
    conj = And(c1, c2)
    z3_expr = adapter.translate(conj)
    import z3
    assert "And" in str(z3_expr) or ("x < 10" in str(z3_expr) and "0 < x" in str(z3_expr))


def test_adapter_translate_bool():
    adapter = Z3MulibAdapter()
    
    b = SymSboolLeaf("b")
    z3_expr = adapter.translate(b)
    
    import z3
    assert z3.is_bool(z3_expr)


def test_adapter_extract_value():
    adapter = Z3MulibAdapter()
    import z3

    # Create the model in the adapter's own context so that extracted
    # variables match.
    solver = z3.Solver(ctx=adapter.ctx)
    x = z3.Int("x", ctx=adapter.ctx)
    solver.add(x == 42)
    solver.check()
    model = solver.model()
    
    # Create a symbolic int and extract its value
    sx = SymSintLeaf("x")
    value = adapter.extract_value(model, sx)
    assert value == 42


# =============================================================================
# Z3IncrementalSolverManager Tests
# =============================================================================

def test_solver_manager_creation():
    sm = Z3IncrementalSolverManager()
    assert sm.get_level() == 0


def test_solver_manager_add_constraint():
    sm = Z3IncrementalSolverManager()
    x = SymSintLeaf("x")
    
    sm.add_constraint(Lt(x, ConcSint(10)))
    assert sm.is_satisfiable()


def test_solver_manager_unsatisfiable():
    sm = Z3IncrementalSolverManager()
    x = SymSintLeaf("x")
    
    sm.add_constraint(Lt(x, ConcSint(5)))
    sm.add_constraint(Lt(ConcSint(10), x))
    
    assert not sm.is_satisfiable()


def test_solver_manager_backtracking():
    sm = Z3IncrementalSolverManager()
    x = SymSintLeaf("x")
    
    sm.add_constraint(Lt(x, ConcSint(10)))
    sm.add_constraint_after_new_backtracking_point(Lt(x, ConcSint(5)))
    
    assert sm.get_level() == 1
    assert sm.is_satisfiable()
    
    sm.backtrack_once()
    
    assert sm.get_level() == 0
    assert sm.is_satisfiable()


def test_solver_manager_check_with_new_constraint():
    sm = Z3IncrementalSolverManager()
    x = SymSintLeaf("x")
    
    sm.add_constraint(Lt(x, ConcSint(10)))
    sm.add_constraint(Lt(ConcSint(0), x))
    
    # Should be satisfiable
    assert sm.check_with_new_constraint(Lt(x, ConcSint(8)))
    
    # Should be unsatisfiable (x > 10 conflicts with x < 10)
    assert not sm.check_with_new_constraint(Lt(ConcSint(10), x))
    
    # Original constraints should still be intact
    assert sm.is_satisfiable()


def test_solver_manager_get_label():
    sm = Z3IncrementalSolverManager()
    x = SymSintLeaf("x")
    
    sm.add_constraint(Lt(x, ConcSint(10)))
    sm.add_constraint(Lt(ConcSint(0), x))
    
    assert sm.is_satisfiable()
    
    label = sm.get_label(x)
    assert isinstance(label, int)
    assert 0 < label < 10


def test_solver_manager_label_solution():
    sm = Z3IncrementalSolverManager()
    x = SymSintLeaf("x")
    y = SymSintLeaf("y")
    
    sm.add_constraint(Eq(x, ConcSint(5)))
    sm.add_constraint(Eq(y, ConcSint(10)))
    
    result = x + y
    remembered = {"x": x, "y": y}
    
    solution = sm.label_solution(result, remembered)
    
    assert isinstance(solution, Solution)
    assert solution.labels.get_label_for_id("x") == 5
    assert solution.labels.get_label_for_id("y") == 10


def test_solver_manager_multiple_backtrack():
    sm = Z3IncrementalSolverManager()
    x = SymSintLeaf("x")
    
    sm.add_constraint(Lt(x, ConcSint(100)))
    
    sm.add_constraint_after_new_backtracking_point(Lt(x, ConcSint(50)))
    sm.add_constraint_after_new_backtracking_point(Lt(x, ConcSint(25)))
    sm.add_constraint_after_new_backtracking_point(Lt(x, ConcSint(10)))
    
    assert sm.get_level() == 3
    
    sm.backtrack(2)
    
    assert sm.get_level() == 1


def test_solver_manager_backtrack_all():
    sm = Z3IncrementalSolverManager()
    x = SymSintLeaf("x")
    
    for i in range(5):
        sm.add_constraint_after_new_backtracking_point(Lt(x, ConcSint(100 - i*10)))
    
    assert sm.get_level() == 5
    
    sm.backtrack_all()
    
    assert sm.get_level() == 0


# =============================================================================
# Z3GlobalLearningSolverManager Tests
# =============================================================================

def test_global_learning_solver():
    sm = Z3GlobalLearningSolverManager()
    x = SymSintLeaf("x")
    
    # Add global constraint
    sm.add_global_constraint(Lt(ConcSint(0), x))
    
    # Add local constraint with backtracking
    sm.add_constraint_after_new_backtracking_point(Lt(x, ConcSint(10)))
    
    assert sm.is_satisfiable()
    label = sm.get_label(x)
    assert 0 < label < 10
    
    # Backtrack
    sm.backtrack_all()
    
    # Global constraint should still be in effect
    assert sm.is_satisfiable()
    label = sm.get_label(x)
    assert label > 0


# =============================================================================
# Floating Point Tests
# =============================================================================

def test_solver_with_doubles():
    sm = Z3IncrementalSolverManager()
    d = SymSdoubleLeaf("d")
    
    sm.add_constraint(Lt(ConcSdouble(0.0), d))
    sm.add_constraint(Lt(d, ConcSdouble(1.0)))
    
    assert sm.is_satisfiable()
    label = sm.get_label(d)
    assert 0 < label < 1


# =============================================================================
# Boolean Tests
# =============================================================================

def test_solver_with_bools():
    sm = Z3IncrementalSolverManager()
    b = SymSboolLeaf("b")
    
    # b must be true
    sm.add_constraint(b)
    
    assert sm.is_satisfiable()
    label = sm.get_label(b)
    assert label is True


# =============================================================================
# Timeout Test
# =============================================================================

def test_solver_timeout():
    sm = Z3IncrementalSolverManager(timeout_ms=1000)
    # Just verify it doesn't crash
    assert sm.is_satisfiable()


# =============================================================================
# Edge Cases
# =============================================================================

def test_empty_solver():
    sm = Z3IncrementalSolverManager()
    assert sm.is_satisfiable()  # Empty constraints are satisfiable


def test_concrete_only():
    sm = Z3IncrementalSolverManager()
    
    # 5 < 10 - always true
    sm.add_constraint(Lt(ConcSint(5), ConcSint(10)))
    assert sm.is_satisfiable()
    
    # 10 < 5 - always false
    sm.add_constraint(Lt(ConcSint(10), ConcSint(5)))
    assert not sm.is_satisfiable()
