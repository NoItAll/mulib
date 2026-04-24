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


# =============================================================================
# PartnerClassArraySolverRepresentation Tests (arrays of symbolic objects)
# =============================================================================


class _DummyPartnerClass:
    """Stand-in non-primitive type used to exercise partner-class arrays."""


def test_partner_class_array_rejects_primitive_element_type():
    from mulib_python.array_repr import PartnerClassArraySolverRepresentation

    with pytest.raises(ValueError):
        PartnerClassArraySolverRepresentation(
            array_id="bad",
            element_type=int,
            length=ConcSint(4),
        )


def test_partner_class_array_default_is_null_sentinel():
    from mulib_python.array_repr import PartnerClassArraySolverRepresentation

    rep = PartnerClassArraySolverRepresentation(
        array_id="objs",
        element_type=_DummyPartnerClass,
        length=ConcSint(4),
    )

    default = rep.get_default_value()
    # Sentinel for null is ConcSint(-1) (matches Java's MINUS_ONE).
    assert isinstance(default, ConcSint)
    assert default._value == -1


def test_partner_class_array_store_none_coerced_to_null():
    from mulib_python.array_repr import PartnerClassArraySolverRepresentation

    rep = PartnerClassArraySolverRepresentation(
        array_id="objs",
        element_type=_DummyPartnerClass,
        length=ConcSint(4),
    )
    adapter = Z3MulibAdapter()

    rep.store(ConcSint(0), None, adapter)

    # The history must contain a STORE whose value is the null sentinel,
    # not Python's None (which the Z3 adapter cannot translate).
    stores = [op for op in rep._history if op.is_store]
    assert len(stores) == 1
    assert isinstance(stores[0].value, ConcSint)
    assert stores[0].value._value == -1


def test_partner_class_array_store_then_select_with_z3():
    """End-to-end: storing object IDs and selecting them through Z3."""
    from mulib_python.array_repr import PartnerClassArraySolverRepresentation
    from mulib_python.z3_solver_manager import Z3IncrementalSolverManager

    sm = Z3IncrementalSolverManager()
    adapter = sm.adapter

    rep = PartnerClassArraySolverRepresentation(
        array_id="objs",
        element_type=_DummyPartnerClass,
        length=ConcSint(4),
    )

    # Store a couple of object IDs (concrete Sints) and a None (-> null).
    rep.store(ConcSint(0), ConcSint(101), adapter)
    rep.store(ConcSint(1), ConcSint(202), adapter)
    rep.store(ConcSint(2), None, adapter)

    # Select each slot into a fresh symbolic variable and assert via Z3.
    r0, r1, r2, r3 = (
        SymSintLeaf("r0"), SymSintLeaf("r1"),
        SymSintLeaf("r2"), SymSintLeaf("r3"),
    )
    for index, result in [(0, r0), (1, r1), (2, r2), (3, r3)]:
        for c in rep.select(ConcSint(index), result, adapter):
            sm.solver.add(c)

    assert sm.is_satisfiable()
    assert sm.get_label(r0) == 101
    assert sm.get_label(r1) == 202
    # Index 2 was stored as null -> sentinel -1.
    assert sm.get_label(r2) == -1
    # Index 3 was never stored -> default is also the null sentinel.
    assert sm.get_label(r3) == -1


def test_partner_class_array_copy_independent_history():
    from mulib_python.array_repr import PartnerClassArraySolverRepresentation

    rep = PartnerClassArraySolverRepresentation(
        array_id="objs",
        element_type=_DummyPartnerClass,
        length=ConcSint(4),
    )
    adapter = Z3MulibAdapter()

    rep.store(ConcSint(0), ConcSint(7), adapter)
    snapshot = rep.copy()

    # Mutate the original after the snapshot.
    rep.store(ConcSint(1), ConcSint(8), adapter)

    assert len(snapshot._history) == 1
    assert len(rep._history) == 2
    # The copy must be the partner-class subclass, not the primitive parent.
    assert isinstance(snapshot, PartnerClassArraySolverRepresentation)


def test_solver_manager_dispatches_partner_class_array():
    """Z3IncrementalSolverManager picks the partner-class rep for object arrays."""
    from mulib_python.array_repr import (
        PartnerClassArraySolverRepresentation,
        PrimitiveValuedArraySolverRepresentation,
    )
    from mulib_python.constraints import ArrayInitializationConstraint
    from mulib_python.z3_solver_manager import Z3IncrementalSolverManager

    sm = Z3IncrementalSolverManager()

    # Object-typed array -> partner-class representation.
    obj_init = ArrayInitializationConstraint(
        partner_class_object_id=ConcSint(101),
        index=ConcSint(0),
        value_type=_DummyPartnerClass,
        length=ConcSint(4),
    )
    sm.add_array_constraint(obj_init)
    obj_rep = sm._state.current_object_states.get_array(obj_init.array_id)
    assert isinstance(obj_rep, PartnerClassArraySolverRepresentation)

    # Primitive-typed array -> primitive representation.
    prim_init = ArrayInitializationConstraint(
        partner_class_object_id=ConcSint(202),
        index=ConcSint(0),
        value_type=int,
        length=ConcSint(4),
    )
    sm.add_array_constraint(prim_init)
    prim_rep = sm._state.current_object_states.get_array(prim_init.array_id)
    assert type(prim_rep) is PrimitiveValuedArraySolverRepresentation


def test_solver_manager_array_access_via_constraints():
    """End-to-end: add init + STORE + SELECT constraints through the manager."""
    from mulib_python.constraints import (
        ArrayAccessConstraint, ArrayInitializationConstraint,
    )
    from mulib_python.z3_solver_manager import Z3IncrementalSolverManager

    sm = Z3IncrementalSolverManager()

    array_ref = ConcSint(7)
    init = ArrayInitializationConstraint(
        partner_class_object_id=array_ref,
        index=ConcSint(0),
        value_type=_DummyPartnerClass,
        length=ConcSint(3),
    )
    sm.add_array_constraint(init)

    # STORE object id 99 at index 0.
    sm.add_array_constraint(ArrayAccessConstraint(
        partner_class_object_id=array_ref,
        index=ConcSint(0),
        type=ArrayAccessConstraint.Type.STORE,
        value=ConcSint(99),
    ))

    # SELECT into a fresh symbolic var.
    result = SymSintLeaf("res")
    sm.add_array_constraint(ArrayAccessConstraint(
        partner_class_object_id=array_ref,
        index=ConcSint(0),
        type=ArrayAccessConstraint.Type.SELECT,
        value=result,
    ))

    assert sm.is_satisfiable()
    assert sm.get_label(result) == 99
