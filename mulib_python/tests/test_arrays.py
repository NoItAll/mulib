"""Tests for array representations and constraints."""

import pytest
from mulib_python.array_repr import (
    ArrayHistorySolverRepresentation,
    PrimitiveValuedArraySolverRepresentation,
    SymbolicObjectStates,
    IncrementalSolverState,
)
from mulib_python.z3_adapter import Z3MulibAdapter
from mulib_python.substitutions.primitives.sint import ConcSint, SymSintLeaf


# =============================================================================
# ArrayHistorySolverRepresentation Tests
# =============================================================================

def test_array_creation():
    rep = ArrayHistorySolverRepresentation(
        array_id="arr1",
        element_type=int,
        length=ConcSint(10),
        default_value=ConcSint(0),
    )
    
    assert rep.array_id == "arr1"
    assert rep.get_default_value()._value == 0


def test_array_store():
    rep = ArrayHistorySolverRepresentation(
        array_id="arr",
        element_type=int,
        length=ConcSint(10),
        default_value=ConcSint(0),
    )
    adapter = Z3MulibAdapter()
    
    # Store at index 0
    constraints = rep.store(ConcSint(0), ConcSint(42), adapter)
    
    # STORE doesn't generate constraints (only SELECT does)
    assert len(constraints) == 0


def test_array_select():
    rep = ArrayHistorySolverRepresentation(
        array_id="arr",
        element_type=int,
        length=ConcSint(10),
        default_value=ConcSint(0),
    )
    adapter = Z3MulibAdapter()
    
    # SELECT before any STORE should return default
    result = SymSintLeaf("r")
    constraints = rep.select(ConcSint(0), result, adapter)
    
    # Should have a constraint that result equals default
    assert len(constraints) >= 1


def test_array_store_then_select():
    rep = ArrayHistorySolverRepresentation(
        array_id="arr",
        element_type=int,
        length=ConcSint(10),
        default_value=ConcSint(0),
    )
    adapter = Z3MulibAdapter()
    
    # Store 42 at index 0
    rep.store(ConcSint(0), ConcSint(42), adapter)
    
    # Select at index 0 - should get 42
    result = SymSintLeaf("r")
    constraints = rep.select(ConcSint(0), result, adapter)
    
    # Should have a constraint relating result to stored value
    assert len(constraints) >= 1


def test_array_copy():
    rep = ArrayHistorySolverRepresentation(
        array_id="arr",
        element_type=int,
        length=ConcSint(10),
        default_value=ConcSint(0),
    )
    adapter = Z3MulibAdapter()
    
    # Store some values
    rep.store(ConcSint(0), ConcSint(1), adapter)
    rep.store(ConcSint(1), ConcSint(2), adapter)
    
    # Copy
    copy = rep.copy()
    
    # Modify original
    rep.store(ConcSint(2), ConcSint(3), adapter)
    
    # Copy should not have the third store
    assert len(copy._history) == 2
    assert len(rep._history) == 3


# =============================================================================
# PrimitiveValuedArraySolverRepresentation Tests
# =============================================================================

def test_primitive_array_bounds_check():
    rep = PrimitiveValuedArraySolverRepresentation(
        array_id="arr",
        element_type=int,
        length=ConcSint(10),
        check_bounds=True,
    )
    adapter = Z3MulibAdapter()
    
    # Select at index 5
    result = SymSintLeaf("r")
    constraints = rep.select(ConcSint(5), result, adapter)
    
    # Should have bounds check constraints
    # 0 <= index < length
    assert len(constraints) >= 1


def test_primitive_array_no_bounds_check():
    rep = PrimitiveValuedArraySolverRepresentation(
        array_id="arr",
        element_type=int,
        length=ConcSint(10),
        check_bounds=False,
    )
    adapter = Z3MulibAdapter()
    
    # Select at index 5
    result = SymSintLeaf("r")
    constraints = rep.select(ConcSint(5), result, adapter)
    
    # Without bounds check, just the value constraint
    assert len(constraints) >= 1


def test_primitive_array_default_value():
    from mulib_python.substitutions.primitives.sint import ConcSbool
    
    rep = PrimitiveValuedArraySolverRepresentation(
        array_id="bool_arr",
        element_type=bool,
        length=ConcSint(10),
    )
    
    # Default for bool should be FALSE
    default = rep.get_default_value()
    assert default is ConcSbool.FALSE


# =============================================================================
# SymbolicObjectStates Tests
# =============================================================================

def test_symbolic_object_states():
    states = SymbolicObjectStates()
    
    rep = ArrayHistorySolverRepresentation(
        array_id="arr1",
        element_type=int,
        length=ConcSint(10),
    )
    
    states.register_array("arr1", rep)
    
    retrieved = states.get_array("arr1")
    assert retrieved is rep
    
    assert states.get_array("nonexistent") is None


def test_symbolic_object_states_copy():
    states = SymbolicObjectStates()
    
    rep = ArrayHistorySolverRepresentation(
        array_id="arr1",
        element_type=int,
        length=ConcSint(10),
    )
    
    states.register_array("arr1", rep)
    
    copy = states.copy()
    
    # Modifying original should not affect copy
    states.register_array("arr2", rep)
    
    assert "arr2" in states.arrays
    assert "arr2" not in copy.arrays


# =============================================================================
# IncrementalSolverState Tests
# =============================================================================

def test_incremental_state_push_pop():
    state = IncrementalSolverState()
    
    assert state.level == 0
    
    state.push(5)
    assert state.level == 1
    
    state.push(10)
    assert state.level == 2
    
    count = state.pop()
    assert count == 10
    assert state.level == 1


def test_incremental_state_pop_all():
    state = IncrementalSolverState()
    
    for i in range(5):
        state.push(i)
    
    assert state.level == 5
    
    state.pop_all()
    
    assert state.level == 0


def test_incremental_state_pop_empty():
    state = IncrementalSolverState()
    
    with pytest.raises(RuntimeError):
        state.pop()


# =============================================================================
# Integration with Z3 Solver
# =============================================================================

def test_array_select_with_z3():
    """Test array operations with Z3 solver integration."""
    import z3
    from mulib_python.z3_solver_manager import Z3IncrementalSolverManager
    
    sm = Z3IncrementalSolverManager()
    adapter = sm.adapter
    
    # Create array representation
    rep = ArrayHistorySolverRepresentation(
        array_id="arr",
        element_type=int,
        length=ConcSint(10),
        default_value=ConcSint(0),
    )
    
    # Store 42 at index 0
    rep.store(ConcSint(0), ConcSint(42), adapter)
    
    # Select at index 0
    result = SymSintLeaf("result")
    constraints = rep.select(ConcSint(0), result, adapter)
    
    # Add constraints to solver
    for c in constraints:
        sm.solver.add(c)
    
    # Should be satisfiable
    assert sm.is_satisfiable()
    
    # Result should be 42
    label = sm.get_label(result)
    assert label == 42
