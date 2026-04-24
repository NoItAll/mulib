"""Tests for boundary auto-coercion + ConcS-singleton interning.

Covers:

* ``coercion`` module helpers (correct routing of ``int``/``bool``/``float``,
  rejection of ``None``/``str`` where applicable).
* ``__new__``-based interning for every cached ``ConcS*`` class.
* Constraint / array-constraint constructors accept raw Python primitives.
* ``Z3MulibAdapter`` accepts raw primitives end-to-end.
* Backwards compatibility: explicit ``ConcSint(5)``-style calls still work
  and now return cache-shared singletons.
"""

import math

import pytest

from mulib_python.constraints import (
    And,
    ArrayAccessConstraint,
    ArrayInitializationConstraint,
    BoolIte,
    Eq,
    FALSE,
    In,
    Lt,
    Lte,
    Or,
    TRUE,
)
from mulib_python.substitutions.primitives.coercion import (
    _value_coercer_for,
    to_constraint,
    to_expression,
    to_sbool,
    to_sbyte,
    to_schar,
    to_sdouble,
    to_sfloat,
    to_sint,
    to_slong,
    to_sshort,
)
from mulib_python.substitutions.primitives.sdouble import ConcSdouble, Sdouble
from mulib_python.substitutions.primitives.sfloat import ConcSfloat
from mulib_python.substitutions.primitives.sint import (
    ConcSbool,
    ConcSbyte,
    ConcSchar,
    ConcSint,
    ConcSshort,
    Sbool,
    Sint,
    SymSintLeaf,
)
from mulib_python.substitutions.primitives.slong import ConcSlong
from mulib_python.z3_adapter import Z3MulibAdapter
from mulib_python.z3_solver_manager import Z3IncrementalSolverManager


# ---------------------------------------------------------------------------
# Per-type coercers
# ---------------------------------------------------------------------------

class TestToSint:
    def test_passes_through_existing_sint(self):
        x = ConcSint(7)
        assert to_sint(x) is x

    def test_passes_through_symbolic_sint(self):
        x = SymSintLeaf("v")
        assert to_sint(x) is x

    def test_int_is_coerced_to_cached_concsint(self):
        assert to_sint(5) is ConcSint(5)
        assert to_sint(0) is ConcSint.ZERO

    def test_bool_true_becomes_one_not_true(self):
        """``isinstance(True, int)`` is True; ``to_sint(True)`` must yield 1."""
        result = to_sint(True)
        assert result is ConcSint(1)
        # Stored value is a Python int, not a bool, after coercion.
        assert result.value == 1
        assert type(result.value) is int

    def test_bool_false_becomes_zero(self):
        assert to_sint(False) is ConcSint(0)

    def test_rejects_float(self):
        with pytest.raises(TypeError):
            to_sint(1.5)

    def test_rejects_str(self):
        with pytest.raises(TypeError):
            to_sint("5")

    def test_rejects_none(self):
        with pytest.raises(TypeError):
            to_sint(None)


class TestToSbool:
    def test_passes_through_existing_sbool(self):
        assert to_sbool(ConcSbool.TRUE) is ConcSbool.TRUE

    def test_true_is_singleton(self):
        assert to_sbool(True) is ConcSbool.TRUE

    def test_false_is_singleton(self):
        assert to_sbool(False) is ConcSbool.FALSE

    def test_rejects_int_to_avoid_silent_coercion(self):
        """``to_sbool(1)`` must raise, not silently become TRUE.  Java-side
        bool↔int conversion is explicit; we mirror that here."""
        with pytest.raises(TypeError):
            to_sbool(1)
        with pytest.raises(TypeError):
            to_sbool(0)


class TestToSlong:
    def test_int_becomes_cached_concslong(self):
        assert to_slong(0) is ConcSlong.ZERO
        assert to_slong(1) is ConcSlong.ONE

    def test_bool_first(self):
        assert to_slong(True) is ConcSlong.ONE
        assert to_slong(False) is ConcSlong.ZERO

    def test_rejects_float(self):
        with pytest.raises(TypeError):
            to_slong(1.5)


class TestToSdouble:
    def test_float_becomes_concsdouble(self):
        assert to_sdouble(0.0) is ConcSdouble.ZERO
        assert to_sdouble(1.0) is ConcSdouble.ONE

    def test_int_promoted_to_double(self):
        assert to_sdouble(0) is ConcSdouble.ZERO
        assert to_sdouble(-1) is ConcSdouble.MINUS_ONE

    def test_bool_first(self):
        assert to_sdouble(True) is ConcSdouble.ONE
        assert to_sdouble(False) is ConcSdouble.ZERO

    def test_rejects_str(self):
        with pytest.raises(TypeError):
            to_sdouble("1.0")


class TestToSfloat:
    def test_float_becomes_concsfloat(self):
        assert to_sfloat(0.0) is ConcSfloat.ZERO

    def test_int_promoted(self):
        assert to_sfloat(1) is ConcSfloat.ONE


class TestToSbyteShortChar:
    def test_to_sbyte_truncates(self):
        # ConcSbyte truncates to int8 in __init__ — so 200 -> -56.
        result = to_sbyte(200)
        assert result.value == -56

    def test_to_sshort_truncates(self):
        result = to_sshort(40000)
        # 40000 - 65536 = -25536
        assert result.value == -25536

    def test_to_schar_from_str(self):
        assert to_schar("A") is to_schar(65)
        assert to_schar("A") is ConcSchar(65)

    def test_to_schar_rejects_multichar(self):
        with pytest.raises(TypeError):
            to_schar("AB")


class TestToExpression:
    def test_dispatches_int_to_sint(self):
        assert isinstance(to_expression(5), ConcSint)

    def test_dispatches_bool_to_sbool(self):
        assert to_expression(True) is ConcSbool.TRUE
        assert to_expression(False) is ConcSbool.FALSE

    def test_dispatches_float_to_sdouble_not_sfloat(self):
        """Python ``float`` is double-precision; must not silently lose
        precision by routing to ``Sfloat``."""
        result = to_expression(3.14)
        assert isinstance(result, Sdouble)

    def test_passes_through_existing_expression(self):
        e = SymSintLeaf("v")
        assert to_expression(e) is e

    def test_rejects_none(self):
        with pytest.raises(TypeError):
            to_expression(None)

    def test_rejects_str(self):
        with pytest.raises(TypeError):
            to_expression("x")


class TestToConstraint:
    def test_true_singleton(self):
        assert to_constraint(True) is TRUE

    def test_false_singleton(self):
        assert to_constraint(False) is FALSE

    def test_passes_through_existing_constraint(self):
        c = And(TRUE, TRUE)
        assert to_constraint(c) is c

    def test_passes_through_sbool(self):
        """``Sbool`` is a Constraint subtype; must round-trip unchanged."""
        sb = ConcSbool.TRUE
        assert to_constraint(sb) is sb

    def test_rejects_int(self):
        with pytest.raises(TypeError):
            to_constraint(1)


class TestValueCoercerFor:
    def test_int_dispatch(self):
        assert _value_coercer_for(int) is to_sint

    def test_bool_dispatch(self):
        assert _value_coercer_for(bool) is to_sbool

    def test_float_dispatch(self):
        assert _value_coercer_for(float) is to_sdouble

    def test_unknown_type_falls_back_to_to_expression(self):
        class _PartnerClass:
            pass
        assert _value_coercer_for(_PartnerClass) is to_expression


# ---------------------------------------------------------------------------
# Interning via __new__
# ---------------------------------------------------------------------------

class TestConcSintInterning:
    def test_cache_range_returns_same_instance(self):
        for i in range(ConcSint._LOW_CACHE, ConcSint._HIGH_CACHE + 1):
            assert ConcSint(i) is ConcSint(i), f"failed at {i}"
            assert ConcSint(i) is Sint.conc_sint(i)
            assert ConcSint(i) is ConcSint.get_cached(i)

    def test_outside_cache_range_not_interned(self):
        assert ConcSint(10_000) is not ConcSint(10_000)
        assert ConcSint(-200) is not ConcSint(-200)

    def test_named_constants(self):
        assert ConcSint(0) is ConcSint.ZERO
        assert ConcSint(1) is ConcSint.ONE
        assert ConcSint(-1) is ConcSint.MINUS_ONE

    def test_subclass_does_not_share_cache(self):
        class MySub(ConcSint):
            pass
        # Subclass instances should not collide with the ConcSint cache.
        a = MySub(5)
        b = MySub(5)
        assert a is not ConcSint(5)
        # Subclasses get fresh allocations on each construction.
        assert a is not b


class TestConcSboolInterning:
    def test_singletons(self):
        assert ConcSbool(True) is ConcSbool.TRUE
        assert ConcSbool(False) is ConcSbool.FALSE

    def test_truthy_value_routes_to_true(self):
        # Bootstrap-safe implementation only intercepts when both singletons
        # exist; truthiness check normalises any input.
        assert ConcSbool(1) is ConcSbool.TRUE
        assert ConcSbool(0) is ConcSbool.FALSE


class TestConcSbyteInterning:
    def test_full_range_interned(self):
        for i in range(-128, 128):
            assert ConcSbyte(i) is ConcSbyte(i)
        assert ConcSbyte(0) is ConcSbyte.ZERO

    def test_truncation_then_intern(self):
        # 200 truncates to -56; both should land on the same singleton.
        assert ConcSbyte(200) is ConcSbyte(-56)


class TestConcScharInterning:
    def test_ascii_range_interned(self):
        for i in range(128):
            assert ConcSchar(i) is ConcSchar(i)
        assert ConcSchar(0) is ConcSchar.ZERO

    def test_outside_ascii_not_interned(self):
        assert ConcSchar(8000) is not ConcSchar(8000)


class TestConcSshortInterning:
    def test_small_range_interned(self):
        for i in range(-128, 128):
            assert ConcSshort(i) is ConcSshort(i)

    def test_outside_small_range_not_interned(self):
        assert ConcSshort(1000) is not ConcSshort(1000)


class TestConcSlongInterning:
    def test_named_constants(self):
        assert ConcSlong(0) is ConcSlong.ZERO
        assert ConcSlong(1) is ConcSlong.ONE
        assert ConcSlong(-1) is ConcSlong.MINUS_ONE

    def test_small_range_interned(self):
        for i in range(-128, 128):
            assert ConcSlong(i) is ConcSlong(i)

    def test_outside_small_range_not_interned(self):
        assert ConcSlong(10_000_000_000) is not ConcSlong(10_000_000_000)


class TestConcSdoubleInterning:
    def test_zero_one_minus_one_interned(self):
        assert ConcSdouble(0.0) is ConcSdouble.ZERO
        assert ConcSdouble(1.0) is ConcSdouble.ONE
        assert ConcSdouble(-1.0) is ConcSdouble.MINUS_ONE

    def test_int_zero_promoted_and_interned(self):
        assert ConcSdouble(0) is ConcSdouble.ZERO

    def test_other_values_not_interned(self):
        assert ConcSdouble(0.5) is not ConcSdouble(0.5)

    def test_nan_not_interned(self):
        nan_a = ConcSdouble(float("nan"))
        # NaN must never collapse to a sentinel singleton because NaN != NaN.
        assert nan_a is not ConcSdouble.ZERO
        assert math.isnan(nan_a.value)

    def test_negative_zero_aliases_positive_zero(self):
        # +0.0 and -0.0 compare equal in IEEE-754; sharing a singleton is
        # safe for our purposes (Z3's RealVal does not distinguish them).
        assert ConcSdouble(-0.0) is ConcSdouble.ZERO


class TestConcSfloatInterning:
    def test_named_constants(self):
        assert ConcSfloat(0.0) is ConcSfloat.ZERO
        assert ConcSfloat(1.0) is ConcSfloat.ONE
        assert ConcSfloat(-1.0) is ConcSfloat.MINUS_ONE


# ---------------------------------------------------------------------------
# Constraint constructors accept raw primitives
# ---------------------------------------------------------------------------

class TestConstraintCoercion:
    def test_eq_accepts_int_on_both_sides(self):
        sym = SymSintLeaf("x")
        c1 = Eq(sym, 5)
        c2 = Eq(5, sym)
        assert isinstance(c1.rhs, ConcSint)
        assert isinstance(c2.lhs, ConcSint)
        assert c1.rhs is ConcSint(5)

    def test_eq_int_on_both_sides_folds_to_two_concsints(self):
        c = Eq(3, 5)
        assert isinstance(c.lhs, ConcSint) and isinstance(c.rhs, ConcSint)
        # Distinct cached singletons are unequal expressions.
        assert c.lhs is not c.rhs

    def test_lt_lte_accept_raw(self):
        sym = SymSintLeaf("x")
        Lt(sym, 5)
        Lte(0, sym)

    def test_eq_with_float_rhs_yields_sdouble(self):
        sym = SymSintLeaf("x")
        c = Eq(sym, 3.14)
        assert isinstance(c.rhs, Sdouble)

    def test_in_accepts_raw_set_members(self):
        sym = SymSintLeaf("x")
        c = In(sym, (1, 2, 3))
        assert all(isinstance(e, ConcSint) for e in c.set)
        # Cached small-int singletons.
        assert c.set[0] is ConcSint(1)

    def test_boolite_condition_accepts_raw_bool(self):
        c = BoolIte(True, TRUE, FALSE)
        assert c.condition is TRUE

    def test_eq_rejects_none(self):
        with pytest.raises(TypeError):
            Eq(SymSintLeaf("x"), None)

    def test_constraint_equality_independent_of_wrapping(self):
        """``Eq(sym, 5)`` and ``Eq(sym, ConcSint(5))`` must compare equal
        and share a hash — interning + coercion makes the operands the same
        singleton."""
        sym = SymSintLeaf("x")
        c_raw = Eq(sym, 5)
        c_wrapped = Eq(sym, ConcSint(5))
        assert c_raw == c_wrapped
        assert hash(c_raw) == hash(c_wrapped)


# ---------------------------------------------------------------------------
# Array constraints
# ---------------------------------------------------------------------------

class TestArrayConstraintCoercion:
    def test_init_constraint_accepts_raw_int_length(self):
        c = ArrayInitializationConstraint(
            partner_class_object_id=1, index=0,
            value_type=int, length=4, default_value=0,
            initial_values={0: 7, 1: 8},
        )
        assert isinstance(c.length, ConcSint)
        assert c.length is ConcSint(4)
        assert all(isinstance(v, ConcSint) for v in c.initial_values.values())
        assert c.initial_values[0] is ConcSint(7)

    def test_init_constraint_bool_value_type(self):
        c = ArrayInitializationConstraint(
            partner_class_object_id=1, index=0,
            value_type=bool, length=4, default_value=False,
            initial_values={0: True, 1: False},
        )
        assert c.default_value is ConcSbool.FALSE
        assert c.initial_values[0] is ConcSbool.TRUE
        assert c.initial_values[1] is ConcSbool.FALSE

    def test_init_constraint_float_value_type(self):
        c = ArrayInitializationConstraint(
            partner_class_object_id=1, index=0,
            value_type=float, length=4, default_value=0.0,
            initial_values={0: 3.14},
        )
        assert isinstance(c.default_value, Sdouble)
        assert isinstance(c.initial_values[0], Sdouble)

    def test_init_constraint_default_value_none_preserved(self):
        c = ArrayInitializationConstraint(
            partner_class_object_id=1, index=0,
            value_type=int, length=4,
            default_value=None,
        )
        assert c.default_value is None

    def test_init_constraint_initial_values_keys_must_be_int(self):
        with pytest.raises(TypeError):
            ArrayInitializationConstraint(
                partner_class_object_id=1, index=0,
                value_type=int, length=4,
                initial_values={"0": 1},
            )

    def test_init_constraint_initial_values_bool_key_rejected(self):
        """``bool`` is a subclass of ``int`` in Python — must still reject."""
        with pytest.raises(TypeError):
            ArrayInitializationConstraint(
                partner_class_object_id=1, index=0,
                value_type=int, length=4,
                initial_values={True: 1},
            )

    def test_init_constraint_equality_across_raw_vs_wrapped(self):
        a = ArrayInitializationConstraint(
            partner_class_object_id=1, index=0,
            value_type=int, length=4, default_value=0,
            initial_values={0: 5},
        )
        b = ArrayInitializationConstraint(
            partner_class_object_id=ConcSint(1), index=ConcSint(0),
            value_type=int, length=ConcSint(4), default_value=ConcSint(0),
            initial_values={0: ConcSint(5)},
        )
        assert a == b
        assert hash(a) == hash(b)

    def test_access_constraint_accepts_raw_int(self):
        c = ArrayAccessConstraint(
            partner_class_object_id=2, index=0,
            type=ArrayAccessConstraint.Type.STORE, value=99,
        )
        assert c.partner_class_object_id is ConcSint(2)
        assert c.index is ConcSint(0)
        assert c.value is ConcSint(99)


# ---------------------------------------------------------------------------
# Z3 adapter prologue
# ---------------------------------------------------------------------------

class TestAdapterRawPrimitives:
    def test_translate_int(self):
        import z3
        adapter = Z3MulibAdapter()
        assert adapter.translate(5).eq(z3.IntVal(5, ctx=adapter.ctx))

    def test_translate_negative_int(self):
        import z3
        adapter = Z3MulibAdapter()
        assert adapter.translate(-99).eq(z3.IntVal(-99, ctx=adapter.ctx))

    def test_translate_bool_true(self):
        import z3
        adapter = Z3MulibAdapter()
        assert adapter.translate(True).eq(z3.BoolVal(True, ctx=adapter.ctx))

    def test_translate_bool_false(self):
        import z3
        adapter = Z3MulibAdapter()
        assert adapter.translate(False).eq(z3.BoolVal(False, ctx=adapter.ctx))

    def test_translate_float(self):
        import z3
        adapter = Z3MulibAdapter()
        # Z3 stores 3.14 as a rational (157/50).  Construct the same
        # rational on both sides and compare structurally.
        result = adapter.translate(3.14)
        expected = z3.RealVal(3.14, ctx=adapter.ctx)
        assert result.eq(expected)


class TestEndToEndRawPrimitives:
    def test_eq_with_raw_int_solves(self):
        sm = Z3IncrementalSolverManager()
        x = SymSintLeaf("x")
        sm.add_constraint(Eq(x, 42))
        assert sm.is_satisfiable()
        assert sm.get_label(x) == 42

    def test_lte_with_raw_bounds_solves(self):
        sm = Z3IncrementalSolverManager()
        x = SymSintLeaf("x")
        sm.add_constraint(Lte(0, x))
        sm.add_constraint(Lte(x, 10))
        sm.add_constraint(Eq(x, 7))
        assert sm.is_satisfiable()
        assert sm.get_label(x) == 7

    def test_array_init_with_raw_ints_solves(self):
        sm = Z3IncrementalSolverManager()
        init = ArrayInitializationConstraint(
            partner_class_object_id=99,
            index=0,
            value_type=int,
            length=4,
            default_value=0,
            initial_values={2: 555},
        )
        sm.add_array_constraint(init)
        res = SymSintLeaf("res")
        sm.add_array_constraint(ArrayAccessConstraint(
            partner_class_object_id=99, index=2,
            type=ArrayAccessConstraint.Type.SELECT, value=res,
        ))
        assert sm.is_satisfiable()
        assert sm.get_label(res) == 555


# ---------------------------------------------------------------------------
# Backwards compatibility — explicit ConcSint(...) calls still work
# ---------------------------------------------------------------------------

class TestBackwardsCompat:
    def test_explicit_concsint_returns_cached(self):
        a = ConcSint(5)
        b = Sint.conc_sint(5)
        c = ConcSint.ZERO + 5  # arithmetic still produces a cached value
        assert a is b
        assert b is c

    def test_arithmetic_with_int_unchanged(self):
        sym = SymSintLeaf("x")
        # Both should produce the same expression.
        e1 = sym + 5
        e2 = sym + ConcSint(5)
        # Both are SymSint wrapping a Sum; structural equality.
        assert hash(e1) == hash(e2)

    def test_eq_returns_concsbool_for_two_concsints(self):
        # Pre-existing semantics of ``ConcSint.__eq__`` returning Sbool.
        result = ConcSint(5) == ConcSint(5)
        assert result is ConcSbool.TRUE
        result = ConcSint(5) == ConcSint(6)
        assert result is ConcSbool.FALSE
