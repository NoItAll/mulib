"""Tests for the custom array theory.

Every behavioural test in this file drives the constraints all the way
through the Z3 solver via :class:`Z3IncrementalSolverManager` rather than
inspecting internal data structures (`_history`, `_default_value`, …).
That style catches both correctness bugs in the encoding and integration
mistakes between the array-representation layer, the constraint AST and
the solver manager.
"""

import pytest

from mulib_python.array_repr import (
    NULL_REFERENCE,
    ArrayHistorySolverRepresentation,
    IncrementalSolverState,
    PrimitiveValuedArraySolverRepresentation,
    SimplePartnerClassArraySolverRepresentation,
    SymbolicObjectStates,
)
from mulib_python.constraints import (
    ArrayAccessConstraint,
    ArrayInitializationConstraint,
)
from mulib_python.substitutions.primitives.sint import (
    ConcSbool,
    ConcSint,
    SymSintLeaf,
)
from mulib_python.z3_adapter import Z3MulibAdapter
from mulib_python.z3_solver_manager import Z3IncrementalSolverManager


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

class _DummyPartnerClass:
    """Stand-in non-primitive type used to exercise partner-class arrays."""


def _add_constraints(sm: Z3IncrementalSolverManager, constraints) -> None:
    for c in constraints:
        sm.solver.add(c)


def _select_value(
    sm: Z3IncrementalSolverManager,
    rep,
    index,
    var_name: str,
):
    """SELECT through ``rep`` into a fresh symbolic var and return its label."""
    var = SymSintLeaf(var_name)
    _add_constraints(sm, rep.select(index, var, sm.adapter))
    assert sm.is_satisfiable()
    return sm.get_label(var)


# ---------------------------------------------------------------------------
# ArrayHistorySolverRepresentation: end-to-end SELECT/STORE semantics
# ---------------------------------------------------------------------------

def test_history_array_select_returns_default_when_no_stores():
    sm = Z3IncrementalSolverManager()
    rep = ArrayHistorySolverRepresentation(
        array_id="a", element_type=int, length=ConcSint(10),
        default_value=ConcSint(7),
    )
    assert _select_value(sm, rep, ConcSint(0), "v") == 7


def test_history_array_select_returns_stored_value():
    sm = Z3IncrementalSolverManager()
    rep = ArrayHistorySolverRepresentation(
        array_id="a", element_type=int, length=ConcSint(10),
        default_value=ConcSint(0),
    )
    rep.store(ConcSint(0), ConcSint(42), sm.adapter)
    assert _select_value(sm, rep, ConcSint(0), "v") == 42


def test_history_array_select_at_unstored_index_returns_default():
    sm = Z3IncrementalSolverManager()
    rep = ArrayHistorySolverRepresentation(
        array_id="a", element_type=int, length=ConcSint(10),
        default_value=ConcSint(99),
    )
    rep.store(ConcSint(0), ConcSint(1), sm.adapter)
    assert _select_value(sm, rep, ConcSint(5), "v") == 99


def test_history_array_most_recent_store_wins_for_same_index():
    """Multiple STOREs to the same index must yield the *latest* value."""
    sm = Z3IncrementalSolverManager()
    rep = ArrayHistorySolverRepresentation(
        array_id="a", element_type=int, length=ConcSint(10),
        default_value=ConcSint(0),
    )
    rep.store(ConcSint(3), ConcSint(11), sm.adapter)
    rep.store(ConcSint(3), ConcSint(22), sm.adapter)
    rep.store(ConcSint(3), ConcSint(33), sm.adapter)
    assert _select_value(sm, rep, ConcSint(3), "v") == 33


def test_history_array_initial_value_used_when_index_concrete():
    """Before any STORE, a concrete index in ``initial_values`` returns it."""
    sm = Z3IncrementalSolverManager()
    rep = ArrayHistorySolverRepresentation(
        array_id="a", element_type=int, length=ConcSint(10),
        default_value=ConcSint(0),
        initial_values={5: ConcSint(123)},
    )
    assert _select_value(sm, rep, ConcSint(5), "v") == 123


def test_history_array_initial_value_used_even_after_unrelated_stores():
    """Regression: SELECT once consulted ``initial_values`` only when no STORE
    had ever been recorded.  After a STORE at a *different* index, a SELECT
    of an index that has an initial value must still see that initial value.
    """
    sm = Z3IncrementalSolverManager()
    rep = ArrayHistorySolverRepresentation(
        array_id="a", element_type=int, length=ConcSint(10),
        default_value=ConcSint(0),
        initial_values={5: ConcSint(99)},
    )
    rep.store(ConcSint(0), ConcSint(1), sm.adapter)
    assert _select_value(sm, rep, ConcSint(5), "v") == 99


def test_history_array_store_overrides_initial_value():
    sm = Z3IncrementalSolverManager()
    rep = ArrayHistorySolverRepresentation(
        array_id="a", element_type=int, length=ConcSint(10),
        default_value=ConcSint(0),
        initial_values={5: ConcSint(99)},
    )
    rep.store(ConcSint(5), ConcSint(7), sm.adapter)
    assert _select_value(sm, rep, ConcSint(5), "v") == 7


def test_history_array_symbolic_index_picks_correct_store():
    """A symbolic index must resolve to the matching STORE via the ITE chain."""
    sm = Z3IncrementalSolverManager()
    rep = ArrayHistorySolverRepresentation(
        array_id="a", element_type=int, length=ConcSint(10),
        default_value=ConcSint(0),
    )
    rep.store(ConcSint(0), ConcSint(10), sm.adapter)
    rep.store(ConcSint(1), ConcSint(20), sm.adapter)
    rep.store(ConcSint(2), ConcSint(30), sm.adapter)

    # Pin the symbolic index to 1 via a side constraint and verify SELECT.
    idx = SymSintLeaf("idx")
    sm.solver.add(sm.adapter.translate(idx) == 1)
    assert _select_value(sm, rep, idx, "v_at_idx") == 20


def test_history_array_copy_is_independent_under_z3():
    sm = Z3IncrementalSolverManager()
    rep = ArrayHistorySolverRepresentation(
        array_id="a", element_type=int, length=ConcSint(10),
        default_value=ConcSint(0),
    )
    rep.store(ConcSint(0), ConcSint(1), sm.adapter)
    snapshot = rep.copy()

    # Mutate the original after the snapshot is taken.
    rep.store(ConcSint(1), ConcSint(99), sm.adapter)

    # Each rep should observe its own history end-to-end through Z3.
    sm_orig = Z3IncrementalSolverManager()
    sm_snap = Z3IncrementalSolverManager()
    assert _select_value(sm_orig, rep, ConcSint(1), "v_o") == 99
    assert _select_value(sm_snap, snapshot, ConcSint(1), "v_s") == 0


# ---------------------------------------------------------------------------
# PrimitiveValuedArraySolverRepresentation
# ---------------------------------------------------------------------------

def test_primitive_array_default_for_int_is_zero():
    sm = Z3IncrementalSolverManager()
    rep = PrimitiveValuedArraySolverRepresentation(
        array_id="a", element_type=int, length=ConcSint(10), check_bounds=False,
    )
    assert _select_value(sm, rep, ConcSint(3), "v") == 0


def test_primitive_array_default_for_bool_is_false():
    rep = PrimitiveValuedArraySolverRepresentation(
        array_id="a", element_type=bool, length=ConcSint(4),
    )
    assert rep.get_default_value() is ConcSbool.FALSE


def test_primitive_array_rejects_unknown_element_type():
    """Pre-existing dirty fix: the parent silently used ConcSint(0) as the
    default for any unknown element type, masking the misuse.  It must now
    raise.
    """
    with pytest.raises(ValueError):
        PrimitiveValuedArraySolverRepresentation(
            array_id="a", element_type=_DummyPartnerClass, length=ConcSint(4),
        )


def test_primitive_array_bounds_check_unsat_on_negative_index():
    sm = Z3IncrementalSolverManager()
    rep = PrimitiveValuedArraySolverRepresentation(
        array_id="a", element_type=int, length=ConcSint(10), check_bounds=True,
    )
    _add_constraints(
        sm, rep.select(ConcSint(-1), SymSintLeaf("v"), sm.adapter)
    )
    assert not sm.is_satisfiable()


def test_primitive_array_bounds_check_unsat_on_index_at_length():
    sm = Z3IncrementalSolverManager()
    rep = PrimitiveValuedArraySolverRepresentation(
        array_id="a", element_type=int, length=ConcSint(10), check_bounds=True,
    )
    _add_constraints(
        sm, rep.store(ConcSint(10), ConcSint(0), sm.adapter)
    )
    assert not sm.is_satisfiable()


def test_primitive_array_no_bounds_check_allows_oob_select():
    sm = Z3IncrementalSolverManager()
    rep = PrimitiveValuedArraySolverRepresentation(
        array_id="a", element_type=int, length=ConcSint(10), check_bounds=False,
    )
    # An out-of-bounds SELECT is satisfiable when bounds checking is off.
    assert _select_value(sm, rep, ConcSint(99), "v") == 0


# ---------------------------------------------------------------------------
# SimplePartnerClassArraySolverRepresentation
# ---------------------------------------------------------------------------

@pytest.mark.parametrize("primitive", [int, bool, float])
def test_partner_class_array_rejects_primitive_element_type(primitive):
    with pytest.raises(ValueError):
        SimplePartnerClassArraySolverRepresentation(
            array_id="a", element_type=primitive, length=ConcSint(4),
        )


def test_partner_class_array_default_is_null_sentinel():
    rep = SimplePartnerClassArraySolverRepresentation(
        array_id="a", element_type=_DummyPartnerClass, length=ConcSint(4),
    )
    assert rep.get_default_value() is NULL_REFERENCE


def test_partner_class_array_select_unstored_yields_null_sentinel():
    sm = Z3IncrementalSolverManager()
    rep = SimplePartnerClassArraySolverRepresentation(
        array_id="a", element_type=_DummyPartnerClass, length=ConcSint(4),
        check_bounds=False,
    )
    assert _select_value(sm, rep, ConcSint(0), "v") == -1


def test_partner_class_array_store_then_select_object_id():
    sm = Z3IncrementalSolverManager()
    rep = SimplePartnerClassArraySolverRepresentation(
        array_id="a", element_type=_DummyPartnerClass, length=ConcSint(4),
        check_bounds=False,
    )
    rep.store(ConcSint(2), ConcSint(101), sm.adapter)
    assert _select_value(sm, rep, ConcSint(2), "v") == 101


def test_partner_class_array_store_none_yields_null_sentinel_via_z3():
    """Storing ``None`` must be observable as ``-1`` through the solver, not
    just by inspecting the history."""
    sm = Z3IncrementalSolverManager()
    rep = SimplePartnerClassArraySolverRepresentation(
        array_id="a", element_type=_DummyPartnerClass, length=ConcSint(4),
        check_bounds=False,
    )
    rep.store(ConcSint(0), None, sm.adapter)
    assert _select_value(sm, rep, ConcSint(0), "v") == -1


def test_partner_class_array_select_with_null_sentinel_result_raises():
    """Selecting *into* the null sentinel is a programming error and must
    raise (matching Java's assertion in ``_select``)."""
    sm = Z3IncrementalSolverManager()
    rep = SimplePartnerClassArraySolverRepresentation(
        array_id="a", element_type=_DummyPartnerClass, length=ConcSint(4),
        check_bounds=False,
    )
    with pytest.raises(AssertionError):
        rep.select(ConcSint(0), NULL_REFERENCE, sm.adapter)


def test_partner_class_array_initial_value_with_none_is_coerced():
    sm = Z3IncrementalSolverManager()
    rep = SimplePartnerClassArraySolverRepresentation(
        array_id="a", element_type=_DummyPartnerClass, length=ConcSint(4),
        initial_values={1: None, 2: ConcSint(77)},
        check_bounds=False,
    )
    assert _select_value(sm, rep, ConcSint(1), "v1") == -1


def test_partner_class_array_initial_value_returned_after_unrelated_store():
    sm = Z3IncrementalSolverManager()
    rep = SimplePartnerClassArraySolverRepresentation(
        array_id="a", element_type=_DummyPartnerClass, length=ConcSint(4),
        initial_values={2: ConcSint(77)},
        check_bounds=False,
    )
    rep.store(ConcSint(0), ConcSint(5), sm.adapter)
    assert _select_value(sm, rep, ConcSint(2), "v") == 77


def test_partner_class_array_copy_independent_through_z3():
    sm_orig = Z3IncrementalSolverManager()
    sm_snap = Z3IncrementalSolverManager()
    rep = SimplePartnerClassArraySolverRepresentation(
        array_id="a", element_type=_DummyPartnerClass, length=ConcSint(4),
        check_bounds=False,
    )
    rep.store(ConcSint(0), ConcSint(7), sm_orig.adapter)
    snapshot = rep.copy()
    rep.store(ConcSint(1), ConcSint(8), sm_orig.adapter)

    assert _select_value(sm_orig, rep, ConcSint(1), "vo") == 8
    # Snapshot never saw the second store -> default is the null sentinel.
    assert _select_value(sm_snap, snapshot, ConcSint(1), "vs") == -1
    # Snapshot is the partner-class subclass after copy, not the parent.
    assert isinstance(snapshot, SimplePartnerClassArraySolverRepresentation)


def test_partner_class_array_copy_preserves_check_bounds():
    """``copy()`` must forward subclass-specific state (``_check_bounds``)
    without requiring the subclass to override ``copy``."""
    rep = SimplePartnerClassArraySolverRepresentation(
        array_id="a", element_type=_DummyPartnerClass, length=ConcSint(4),
        check_bounds=False,
    )
    snapshot = rep.copy()
    assert snapshot._check_bounds is False


def test_partner_class_array_symbolic_object_id_round_trip():
    """A symbolic ``Sint`` (representing an object handle) stored at a
    concrete index must come back via the model."""
    sm = Z3IncrementalSolverManager()
    rep = SimplePartnerClassArraySolverRepresentation(
        array_id="a", element_type=_DummyPartnerClass, length=ConcSint(4),
        check_bounds=False,
    )
    obj = SymSintLeaf("obj")
    sm.solver.add(sm.adapter.translate(obj) == 314)
    rep.store(ConcSint(0), obj, sm.adapter)
    assert _select_value(sm, rep, ConcSint(0), "v") == 314


# ---------------------------------------------------------------------------
# SymbolicObjectStates / IncrementalSolverState
# ---------------------------------------------------------------------------

def test_symbolic_object_states_register_and_get():
    states = SymbolicObjectStates()
    rep = ArrayHistorySolverRepresentation(
        array_id="arr1", element_type=int, length=ConcSint(10),
    )
    states.register_array("arr1", rep)
    assert states.get_array("arr1") is rep
    assert states.get_array("missing") is None


def test_symbolic_object_states_copy_is_independent():
    states = SymbolicObjectStates()
    rep = ArrayHistorySolverRepresentation(
        array_id="arr1", element_type=int, length=ConcSint(10),
    )
    states.register_array("arr1", rep)
    snap = states.copy()
    states.register_array("arr2", rep)
    assert "arr2" in states.arrays and "arr2" not in snap.arrays


def test_incremental_state_push_pop():
    state = IncrementalSolverState()
    assert state.level == 0
    state.push(5)
    state.push(10)
    assert state.level == 2
    assert state.pop() == 10
    assert state.level == 1


def test_incremental_state_pop_all():
    state = IncrementalSolverState()
    for i in range(5):
        state.push(i)
    state.pop_all()
    assert state.level == 0


def test_incremental_state_pop_empty_raises():
    with pytest.raises(RuntimeError):
        IncrementalSolverState().pop()


# ---------------------------------------------------------------------------
# Array constraint AST: equality / hashing
# ---------------------------------------------------------------------------

def test_array_init_constraint_equality_includes_default_value():
    a = ArrayInitializationConstraint(
        partner_class_object_id=ConcSint(1), index=ConcSint(0),
        value_type=int, length=ConcSint(4), default_value=ConcSint(0),
    )
    b = ArrayInitializationConstraint(
        partner_class_object_id=ConcSint(1), index=ConcSint(0),
        value_type=int, length=ConcSint(4), default_value=ConcSint(7),
    )
    assert a != b
    assert hash(a) != hash(b) or a == b  # different defaults -> not equal


def test_array_init_constraint_equality_includes_initial_values():
    a = ArrayInitializationConstraint(
        partner_class_object_id=ConcSint(1), index=ConcSint(0),
        value_type=int, length=ConcSint(4),
        initial_values={0: ConcSint(1)},
    )
    b = ArrayInitializationConstraint(
        partner_class_object_id=ConcSint(1), index=ConcSint(0),
        value_type=int, length=ConcSint(4),
        initial_values={0: ConcSint(2)},
    )
    assert a != b


def test_array_init_constraint_equal_for_same_inputs():
    a = ArrayInitializationConstraint(
        partner_class_object_id=ConcSint(1), index=ConcSint(0),
        value_type=int, length=ConcSint(4),
        initial_values={0: ConcSint(1), 2: ConcSint(3)},
    )
    b = ArrayInitializationConstraint(
        partner_class_object_id=ConcSint(1), index=ConcSint(0),
        value_type=int, length=ConcSint(4),
        initial_values={0: ConcSint(1), 2: ConcSint(3)},
    )
    assert a == b
    assert hash(a) == hash(b)


def test_array_init_constraint_is_hashable_with_initial_values():
    """Sanity check: the frozen ``initial_values`` must not break hashing
    (an earlier version sorted items, which would crash on non-orderable
    keys)."""
    c = ArrayInitializationConstraint(
        partner_class_object_id=ConcSint(1), index=ConcSint(0),
        value_type=int, length=ConcSint(4),
        initial_values={3: ConcSint(1), 1: ConcSint(2), 2: ConcSint(3)},
    )
    {c}  # round-trip through a set proves it's hashable


def test_array_id_uses_concrete_value():
    c = ArrayInitializationConstraint(
        partner_class_object_id=ConcSint(42), index=ConcSint(0),
        value_type=int, length=ConcSint(1),
    )
    assert c.array_id == "arr#c:42"


def test_array_id_uses_symbolic_leaf_id():
    leaf = SymSintLeaf("MyArr")
    c = ArrayInitializationConstraint(
        partner_class_object_id=leaf, index=ConcSint(0),
        value_type=int, length=ConcSint(1),
    )
    assert c.array_id == "arr#s:MyArr"


def test_array_id_distinguishes_concrete_and_symbolic_with_same_label():
    """A concrete ``42`` and a symbolic leaf called ``42`` must not collide."""
    c_conc = ArrayInitializationConstraint(
        partner_class_object_id=ConcSint(42), index=ConcSint(0),
        value_type=int, length=ConcSint(1),
    )
    c_sym = ArrayInitializationConstraint(
        partner_class_object_id=SymSintLeaf("42"), index=ConcSint(0),
        value_type=int, length=ConcSint(1),
    )
    assert c_conc.array_id != c_sym.array_id


def test_array_id_rejects_unsupported_expression_type():
    """Anything that is not a ``ConcSint`` or a ``SymSintLeaf`` must be
    rejected explicitly rather than being keyed by a possibly-colliding
    ``repr``."""
    sym_a, sym_b = SymSintLeaf("a"), SymSintLeaf("b")
    composite = sym_a + sym_b  # produces a non-leaf Sint
    c = ArrayInitializationConstraint(
        partner_class_object_id=composite, index=ConcSint(0),
        value_type=int, length=ConcSint(1),
    )
    with pytest.raises(TypeError):
        c.array_id


def test_array_access_constraint_is_store_property():
    a = ArrayAccessConstraint(
        partner_class_object_id=ConcSint(1), index=ConcSint(0),
        type=ArrayAccessConstraint.Type.STORE, value=ConcSint(0),
    )
    s = ArrayAccessConstraint(
        partner_class_object_id=ConcSint(1), index=ConcSint(0),
        type=ArrayAccessConstraint.Type.SELECT, value=ConcSint(0),
    )
    assert a.is_store is True
    assert s.is_store is False


# ---------------------------------------------------------------------------
# Z3IncrementalSolverManager.add_array_constraint dispatch + integration
# ---------------------------------------------------------------------------

def _init(value_type, *, oid=ConcSint(1), length=ConcSint(4), **kwargs):
    return ArrayInitializationConstraint(
        partner_class_object_id=oid,
        index=ConcSint(0),
        value_type=value_type,
        length=length,
        **kwargs,
    )


def _store(oid, index, value):
    return ArrayAccessConstraint(
        partner_class_object_id=oid, index=index,
        type=ArrayAccessConstraint.Type.STORE, value=value,
    )


def _select(oid, index, into):
    return ArrayAccessConstraint(
        partner_class_object_id=oid, index=index,
        type=ArrayAccessConstraint.Type.SELECT, value=into,
    )


def test_manager_dispatches_primitive_array_to_primitive_rep():
    sm = Z3IncrementalSolverManager()
    init = _init(int, oid=ConcSint(101))
    sm.add_array_constraint(init)

    rep = sm._state.current_object_states.get_array(init.array_id)
    assert type(rep) is PrimitiveValuedArraySolverRepresentation


def test_manager_dispatches_object_array_to_partner_class_rep():
    sm = Z3IncrementalSolverManager()
    init = _init(_DummyPartnerClass, oid=ConcSint(202))
    sm.add_array_constraint(init)

    rep = sm._state.current_object_states.get_array(init.array_id)
    assert isinstance(rep, SimplePartnerClassArraySolverRepresentation)


def test_manager_end_to_end_partner_class_store_select():
    sm = Z3IncrementalSolverManager()
    oid = ConcSint(7)
    sm.add_array_constraint(_init(_DummyPartnerClass, oid=oid, length=ConcSint(3)))
    sm.add_array_constraint(_store(oid, ConcSint(0), ConcSint(99)))

    res = SymSintLeaf("res")
    sm.add_array_constraint(_select(oid, ConcSint(0), res))

    assert sm.is_satisfiable()
    assert sm.get_label(res) == 99


def test_manager_end_to_end_initial_values_through_constraint():
    """`ArrayInitializationConstraint(initial_values=...)` must be honoured
    end-to-end, including for indices never written to."""
    sm = Z3IncrementalSolverManager()
    oid = ConcSint(11)
    sm.add_array_constraint(_init(
        int, oid=oid, length=ConcSint(4),
        initial_values={2: ConcSint(555)},
    ))
    res = SymSintLeaf("res")
    sm.add_array_constraint(_select(oid, ConcSint(2), res))
    assert sm.is_satisfiable()
    assert sm.get_label(res) == 555


def test_manager_invalidates_label_cache_on_init():
    """A re-init at the same array_id must not return stale labels."""
    sm = Z3IncrementalSolverManager()
    oid = ConcSint(13)
    sm.add_array_constraint(_init(int, oid=oid, length=ConcSint(4)))
    sm.add_array_constraint(_store(oid, ConcSint(0), ConcSint(7)))
    res1 = SymSintLeaf("r1")
    sm.add_array_constraint(_select(oid, ConcSint(0), res1))
    assert sm.get_label(res1) == 7

    # Re-initialise; cache must be cleared so a new SELECT works.
    sm.add_array_constraint(_init(int, oid=oid, length=ConcSint(4)))
    res2 = SymSintLeaf("r2")
    sm.add_array_constraint(_select(oid, ConcSint(0), res2))
    # No STOREs on the fresh array -> default 0.
    assert sm.get_label(res2) == 0


def test_manager_raises_on_access_before_init():
    sm = Z3IncrementalSolverManager()
    with pytest.raises(ValueError):
        sm.add_array_constraint(_store(ConcSint(99), ConcSint(0), ConcSint(0)))


def test_manager_partner_class_array_oob_unsat():
    """Bounds checks flow through partner-class arrays just like primitive ones."""
    sm = Z3IncrementalSolverManager()
    oid = ConcSint(21)
    sm.add_array_constraint(_init(_DummyPartnerClass, oid=oid, length=ConcSint(2)))
    sm.add_array_constraint(_store(oid, ConcSint(5), ConcSint(0)))
    assert not sm.is_satisfiable()


# ---------------------------------------------------------------------------
# Z3MulibAdapter must refuse to translate misrouted array constraints.
# ---------------------------------------------------------------------------

def test_adapter_refuses_array_access_constraint():
    adapter = Z3MulibAdapter()
    ac = _store(ConcSint(1), ConcSint(0), ConcSint(0))
    with pytest.raises(TypeError):
        adapter.translate(ac)


def test_adapter_refuses_array_initialization_constraint():
    adapter = Z3MulibAdapter()
    init = _init(int, oid=ConcSint(1))
    with pytest.raises(TypeError):
        adapter.translate(init)
