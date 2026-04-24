# Phase 2 Implementation Plan: Symbolic Types (Substitutions)

## Overview

This document is the complete, self-contained specification for the Phase 2 implementation agent.
It covers every file to create, every class/method signature, all Python-specific design decisions,
and precise notes on where Phase 3 (SymbolicExecution) will plug in.

Phase 1 already exists at `mulib_python/`:
- `expressions.py` — immutable AST nodes (`Expression`, `Sum`, `Sub`, `Mul`, `Div`, `Mod`, `Neg`, `BitwiseAnd`, `BitwiseOr`, `BitwiseXor`, `ShiftLeft`, `ShiftRight`, `LogicalShiftRight`, `ExpressionIte`)
- `constraints.py` — immutable constraint nodes (`Constraint`, `And`, `Or`, `Not`, `Lt`, `Lte`, `Eq`, `In`, `BoolIte`, etc.)
- `exceptions.py` — `MulibException`, `MulibIllegalStateException`, `MisconfigurationException`, `NotYetImplementedException`, `Fail`, etc.

---

## 1. Files to Create

```
mulib_python/
├── substitutions/
│   ├── __init__.py                     # Public re-exports for the entire substitutions package
│   ├── _se_context.py                  # Thread-local SymbolicExecution registry
│   ├── markers.py                      # Substituted, Sym, Conc marker ABCs
│   └── primitives/
│       ├── __init__.py                 # Public re-exports for primitives
│       ├── sprimitive.py               # Sprimitive, SymSprimitive, SymSprimitiveLeaf
│       ├── snumber.py                  # Snumber, ConcSnumber, SymSnumber, AbstractSnumber, Sfpnumber
│       ├── sint.py                     # Sint + ConcSint/SymSint/SymSintLeaf
│       │                               # + Sbool + ConcSbool/SymSbool/SymSboolLeaf
│       │                               # + Sbyte + ConcSbyte/SymSbyte/SymSbyteLeaf
│       │                               # + Schar + ConcSchar/SymSchar/SymScharLeaf
│       │                               # + Sshort + ConcSshort/SymSshort/SymSshortLeaf
│       ├── slong.py                    # Slong + ConcSlong/SymSlong/SymSlongLeaf
│       ├── sdouble.py                  # Sdouble + ConcSdouble/SymSdouble/SymSdoubleLeaf
│       └── sfloat.py                   # Sfloat + ConcSfloat/SymSfloat/SymSfloatLeaf
└── value_factory.py                    # ValueFactory ABC + SymbolicValueFactory
```

**Why Sint/Sbool/Sbyte/Schar/Sshort are co-located in `sint.py`:** In the Java source, `Sbool`,
`Sbyte`, `Schar`, and `Sshort` all extend `Sint`. Grouping them eliminates circular imports that
would arise from separate files while retaining a clean module boundary.

---

## 2. Full Class Hierarchy

```
# markers.py
Substituted (ABC)                       # marker — all substituted values
    Sym(Substituted, ABC)               # marker — symbolic values only
    Conc(Substituted, ABC)              # marker — concrete values only

# sprimitive.py (imports from markers.py)
Sprimitive(Substituted, ABC)            # marker — all primitives (Sym or Conc)
SymSprimitive(Sprimitive, Sym, ABC)     # marker — symbolic primitives
SymSprimitiveLeaf(SymSprimitive, ABC)   # marker — named leaf variables (no sub-expression)
    .id: str                            # abstract property — unique string name

# snumber.py (imports from sprimitive.py, expressions.py, constraints.py)
Snumber(Sprimitive, Expression, ABC)    # number + Expression (both conc and sym)
ConcSprimitive(Sprimitive, Conc, ABC)   # marker — concrete primitive
ConcSnumber(Snumber, ConcSprimitive, ABC)  # concrete number; provides value accessors
    .int_val() -> int                   # abstract
    .double_val() -> float              # abstract
    .float_val() -> float               # abstract
    .long_val() -> int                  # abstract
    .short_val() -> int                 # abstract
    .byte_val() -> int                  # abstract
    .char_val() -> str                  # abstract (single char string)
SymSnumber(Snumber, SymSprimitive, ABC) # symbolic number; wraps an Expression
    .represented_expression: Expression # abstract property

AbstractSnumber(Snumber, ABC)           # shared base for all Snumber implementations
    # __eq__ and __hash__ are implemented here per design notes in §4

Sfpnumber(AbstractSnumber, ABC)         # floating-point marker
    .is_fp -> bool                      # returns True (final)

# sint.py
Sint(AbstractSnumber, ABC)              # is_fp = False (final)
    # ── factory methods (class-level) ──
    @staticmethod conc_sint(i: int) -> ConcSint          # cached for [-128, 127]
    @staticmethod new_input_symbolic_sint() -> SymSint   # new leaf (auto-id)
    @staticmethod new_input_symbolic_sint(id: int) -> SymSint
    @staticmethod new_expression_symbolic_sint(expr: Expression) -> SymSint

    # ── magic methods (arithmetic; each auto-wraps with SE or concrete computation) ──
    def __add__(self, other) -> Sint
    def __radd__(self, other) -> Sint
    def __sub__(self, other) -> Sint
    def __rsub__(self, other) -> Sint
    def __mul__(self, other) -> Sint
    def __rmul__(self, other) -> Sint
    def __truediv__(self, other) -> Sint    # integer div semantics (truncation)
    def __rtruediv__(self, other) -> Sint
    def __floordiv__(self, other) -> Sint   # alias for __truediv__ (JVM idiv)
    def __rfloordiv__(self, other) -> Sint
    def __mod__(self, other) -> Sint
    def __rmod__(self, other) -> Sint
    def __neg__(self) -> Sint
    def __pos__(self) -> Sint               # identity

    # ── magic methods (comparison — return Sbool, not Python bool) ──
    def __lt__(self, other) -> Sbool
    def __le__(self, other) -> Sbool
    def __gt__(self, other) -> Sbool
    def __ge__(self, other) -> Sbool
    def __eq__(self, other) -> Sbool        # see §4 for design decision
    def __ne__(self, other) -> Sbool
    def __hash__(self) -> int               # see §4

    # ── magic methods (bitwise) ──
    def __and__(self, other) -> Sint
    def __rand__(self, other) -> Sint
    def __or__(self, other) -> Sint
    def __ror__(self, other) -> Sint
    def __xor__(self, other) -> Sint
    def __rxor__(self, other) -> Sint
    def __invert__(self) -> Sint            # bitwise NOT (~x == -x - 1)
    def __lshift__(self, other) -> Sint
    def __rlshift__(self, other) -> Sint
    def __rshift__(self, other) -> Sint     # arithmetic right shift
    def __rrshift__(self, other) -> Sint

    # ── logical shift right (no Python operator; explicit method) ──
    def unsigned_rshift(self, other: Sint) -> Sint   # Java's iushr

    # ── type conversion (explicit; return target Stype) ──
    def to_sdouble(self) -> Sdouble         # i2d
    def to_sfloat(self) -> Sfloat           # i2f
    def to_slong(self) -> Slong             # i2l
    def to_sbyte(self) -> Sbyte             # i2b
    def to_sshort(self) -> Sshort           # i2s
    def to_schar(self) -> Schar             # i2c

    # ── choice methods (delegate to SE; return Python bool) ──
    def lt_choice(self, rhs: Sint | None = None) -> bool
    def lte_choice(self, rhs: Sint | None = None) -> bool
    def eq_choice(self, rhs: Sint | None = None) -> bool
    def not_eq_choice(self, rhs: Sint | None = None) -> bool
    def gt_choice(self, rhs: Sint | None = None) -> bool
    def gte_choice(self, rhs: Sint | None = None) -> bool

ConcSint(Sint, ConcSnumber)             # concrete int
    # ── class-level cache ──
    _LOW_CACHE: int = -128              # class constant
    _HIGH_CACHE: int = 127             # class constant
    _CACHE: tuple[ConcSint, ...]        # pre-built cache of 256 instances (class-level)
    MINUS_ONE: ConcSint                 # = ConcSint(-1)
    ZERO: ConcSint                      # = ConcSint(0)
    ONE: ConcSint                       # = ConcSint(1)

    def __init__(self, value: int) -> None   # private; use conc_sint() factory
    @property value: int
    def int_val(self) -> int
    def double_val(self) -> float
    def float_val(self) -> float
    def long_val(self) -> int
    def short_val(self) -> int          # truncate to signed 16-bit
    def byte_val(self) -> int           # truncate to signed 8-bit
    def char_val(self) -> str           # chr(value & 0xFFFF)
    def __repr__(self) -> str
    def __hash__(self) -> int           # = self.value
    def __eq__(self, other) -> Sbool    # returns ConcSbool

SymSint(Sint, SymSnumber)              # symbolic int wrapping an Expression
    def __init__(self, represented_expression: Expression) -> None
    @property represented_expression: Expression
    def is_fp(self) -> bool             # delegates to expression
    def __repr__(self) -> str
    def __eq__(self, other) -> Sbool
    def __hash__(self) -> int           # = hash(represented_expression)

SymSintLeaf(SymSint, SymSprimitiveLeaf)  # named leaf variable "Sint{n}"
    _next_id: itertools.count            # class-level thread-safe counter
    def __init__(self, explicit_id: int | None = None) -> None
    @property id: str                   # "Sint{n}"
    def __repr__(self) -> str
    def __eq__(self, other) -> Sbool
    def __hash__(self) -> int           # = hash(self.id)

# ────────────────────────────────────────────────────────────────────────────
# Sbool (also a Constraint — dual-role)
# ────────────────────────────────────────────────────────────────────────────
Sbool(Sint, Constraint)                 # bool is a special int; also a Constraint
    # ── factory methods ──
    @staticmethod conc_sbool(b: bool) -> ConcSbool    # returns TRUE/FALSE singleton
    @staticmethod new_input_symbolic_sbool() -> SymSbool
    @staticmethod new_input_symbolic_sbool(id: int) -> SymSbool
    @staticmethod new_constraint_sbool(c: Constraint) -> SymSbool  # wrap a constraint

    # ── bool-specific operations (return Sbool) ──
    def bool_and(self, rhs: Sbool) -> Sbool   # logical AND
    def bool_or(self, rhs: Sbool) -> Sbool    # logical OR
    def bool_xor(self, rhs: Sbool) -> Sbool   # logical XOR
    def bool_not(self) -> Sbool               # logical NOT
    def is_equal_to(self, rhs: Sbool) -> Sbool

    # ── __bool__: THIS is the choice-point gateway ──
    def __bool__(self) -> bool   # see §5 for exact implementation

    # ── override bitwise to dispatch to bool logic when rhs is Sbool ──
    def __and__(self, other) -> Sbool | Sint   # if other is Sbool → bool_and; else Sint.iand
    def __or__(self, other) -> Sbool | Sint
    def __xor__(self, other) -> Sbool | Sint

ConcSbool(Sbool, ConcSnumber)           # concrete bool
    TRUE: ConcSbool                     # singleton
    FALSE: ConcSbool                    # singleton

    def __init__(self, value: bool) -> None   # private; use conc_sbool() or TRUE/FALSE
    @property value: bool
    def is_true(self) -> bool
    def is_false(self) -> bool
    def negate(self) -> ConcSbool
    def int_val(self) -> int            # True→1, False→0
    def double_val(self) -> float
    def float_val(self) -> float
    def long_val(self) -> int
    def short_val(self) -> int
    def byte_val(self) -> int
    def char_val(self) -> str
    def __bool__(self) -> bool          # returns self.value (no choice point)
    def __repr__(self) -> str
    def __hash__(self) -> int           # int_val()

SymSbool(Sbool, SymSnumber)            # symbolic bool wrapping a Constraint
    def __init__(self, represented_constraint: Constraint) -> None
    @property represented_constraint: Constraint
    @property represented_expression: Expression   # returns self (self IS an Expression)
    def __repr__(self) -> str
    def __eq__(self, other) -> Sbool
    def __hash__(self) -> int

SymSboolLeaf(SymSbool, SymSprimitiveLeaf)  # named leaf "Sbool{n}"
    _next_id: itertools.count
    def __init__(self, explicit_id: int | None = None) -> None
    @property id: str                   # "Sbool{n}"
    @property represented_constraint: Constraint   # returns self
    def __repr__(self) -> str
    def __eq__(self, other) -> Sbool
    def __hash__(self) -> int

# ── Sbyte (extends Sint; all arithmetic inherited) ──
Sbyte(Sint)
    @staticmethod conc_sbyte(b: int) -> ConcSbyte
    @staticmethod new_input_symbolic_sbyte() -> SymSbyte
    @staticmethod new_input_symbolic_sbyte(id: int) -> SymSbyte
    @staticmethod new_expression_symbolic_sbyte(expr: Expression) -> SymSbyte

ConcSbyte(Sbyte, ConcSnumber)
    ZERO: ConcSbyte
    def __init__(self, value: int) -> None     # value must be in [-128, 127]
    @property value: int
    # value accessor methods (int_val, etc.) — truncate to byte range

SymSbyte(Sbyte, SymSnumber)
    # same structure as SymSint

SymSbyteLeaf(SymSbyte, SymSprimitiveLeaf)
    _next_id: itertools.count              # "Sbyte{n}"

# ── Schar (extends Sint) ──
Schar(Sint)
    @staticmethod conc_schar(c: str | int) -> ConcSchar   # accepts single-char str or int code
    @staticmethod new_input_symbolic_schar() -> SymSchar
    @staticmethod new_input_symbolic_schar(id: int) -> SymSchar
    @staticmethod new_expression_symbolic_schar(expr: Expression) -> SymSchar

ConcSchar(Schar, ConcSnumber)
    ZERO: ConcSchar
    def __init__(self, value: int) -> None   # value: Unicode code point (0–65535)
    def char_val(self) -> str               # chr(value)
    # other value accessors

SymSchar(Schar, SymSnumber)
SymScharLeaf(SymSchar, SymSprimitiveLeaf)   # "Schar{n}"

# ── Sshort (extends Sint) ──
Sshort(Sint)
    @staticmethod conc_sshort(s: int) -> ConcSshort
    @staticmethod new_input_symbolic_sshort() -> SymSshort
    @staticmethod new_input_symbolic_sshort(id: int) -> SymSshort
    @staticmethod new_expression_symbolic_sshort(expr: Expression) -> SymSshort

ConcSshort(Sshort, ConcSnumber)
    ZERO: ConcSshort
    def __init__(self, value: int) -> None   # value: signed 16-bit int

SymSshort(Sshort, SymSnumber)
SymSshortLeaf(SymSshort, SymSprimitiveLeaf)  # "Sshort{n}"

# ────────────────────────────────────────────────────────────────────────────
# slong.py
# ────────────────────────────────────────────────────────────────────────────
Slong(AbstractSnumber)                  # is_fp = False
    # ── factory methods ──
    @staticmethod conc_slong(l: int) -> ConcSlong
    @staticmethod new_input_symbolic_slong() -> SymSlong
    @staticmethod new_input_symbolic_slong(id: int) -> SymSlong
    @staticmethod new_expression_symbolic_slong(expr: Expression) -> SymSlong

    # ── magic methods (arithmetic) ──
    def __add__(self, other) -> Slong
    def __radd__(self, other) -> Slong
    def __sub__(self, other) -> Slong
    def __rsub__(self, other) -> Slong
    def __mul__(self, other) -> Slong
    def __rmul__(self, other) -> Slong
    def __truediv__(self, other) -> Slong
    def __rtruediv__(self, other) -> Slong
    def __floordiv__(self, other) -> Slong
    def __rfloordiv__(self, other) -> Slong
    def __mod__(self, other) -> Slong
    def __rmod__(self, other) -> Slong
    def __neg__(self) -> Slong

    # ── comparisons ──
    def __lt__(self, other) -> Sbool
    def __le__(self, other) -> Sbool
    def __gt__(self, other) -> Sbool
    def __ge__(self, other) -> Sbool
    def __eq__(self, other) -> Sbool
    def __ne__(self, other) -> Sbool
    def __hash__(self) -> int

    # ── bitwise ──
    def __and__(self, other) -> Slong
    def __or__(self, other) -> Slong
    def __xor__(self, other) -> Slong
    def __lshift__(self, other: Sint) -> Slong   # lshl (shift amount is Sint)
    def __rshift__(self, other: Sint) -> Slong   # lshr (arithmetic)
    def unsigned_rshift(self, other: Sint) -> Slong   # lushr

    # ── compare-and-return-int (LCMP) ──
    def cmp(self, rhs: Slong) -> Sint

    # ── type conversion ──
    def to_sint(self) -> Sint            # l2i
    def to_sdouble(self) -> Sdouble      # l2d
    def to_sfloat(self) -> Sfloat        # l2f

    # ── choice ──
    def lt_choice(self, rhs: Slong | None = None) -> bool
    def lte_choice(self, rhs: Slong | None = None) -> bool
    def eq_choice(self, rhs: Slong | None = None) -> bool
    def not_eq_choice(self, rhs: Slong | None = None) -> bool
    def gt_choice(self, rhs: Slong | None = None) -> bool
    def gte_choice(self, rhs: Slong | None = None) -> bool

ConcSlong(Slong, ConcSnumber)
    ZERO: ConcSlong
    ONE: ConcSlong
    MINUS_ONE: ConcSlong
    def __init__(self, value: int) -> None

SymSlong(Slong, SymSnumber)
SymSlongLeaf(SymSlong, SymSprimitiveLeaf)   # "Slong{n}"

# ────────────────────────────────────────────────────────────────────────────
# sdouble.py / sfloat.py (near-identical; differ only in type names)
# ────────────────────────────────────────────────────────────────────────────
Sdouble(Sfpnumber)                      # is_fp = True (inherited from Sfpnumber)
    # ── factory methods ──
    @staticmethod conc_sdouble(d: float) -> ConcSdouble
    @staticmethod new_input_symbolic_sdouble() -> SymSdouble
    @staticmethod new_input_symbolic_sdouble(id: int) -> SymSdouble
    @staticmethod new_expression_symbolic_sdouble(expr: Expression) -> SymSdouble

    # ── magic methods (arithmetic) ──
    def __add__(self, other) -> Sdouble
    def __radd__(self, other) -> Sdouble
    def __sub__(self, other) -> Sdouble
    def __rsub__(self, other) -> Sdouble
    def __mul__(self, other) -> Sdouble
    def __rmul__(self, other) -> Sdouble
    def __truediv__(self, other) -> Sdouble
    def __rtruediv__(self, other) -> Sdouble
    def __floordiv__(self, other) -> Sdouble
    def __rfloordiv__(self, other) -> Sdouble
    def __mod__(self, other) -> Sdouble
    def __rmod__(self, other) -> Sdouble
    def __neg__(self) -> Sdouble

    # ── comparisons ──
    def __lt__(self, other) -> Sbool
    def __le__(self, other) -> Sbool
    def __gt__(self, other) -> Sbool
    def __ge__(self, other) -> Sbool
    def __eq__(self, other) -> Sbool
    def __ne__(self, other) -> Sbool
    def __hash__(self) -> int

    # ── compare-and-return-int (DCMP) ──
    def cmp(self, rhs: Sdouble) -> Sint

    # ── type conversion ──
    def to_sfloat(self) -> Sfloat        # d2f
    def to_slong(self) -> Slong          # d2l
    def to_sint(self) -> Sint            # d2i

    # ── choice ──
    def lt_choice(self, rhs: Sdouble | None = None) -> bool
    def lte_choice(self, rhs: Sdouble | None = None) -> bool
    def eq_choice(self, rhs: Sdouble | None = None) -> bool
    def not_eq_choice(self, rhs: Sdouble | None = None) -> bool
    def gt_choice(self, rhs: Sdouble | None = None) -> bool
    def gte_choice(self, rhs: Sdouble | None = None) -> bool

ConcSdouble(Sdouble, ConcSnumber)
    ZERO: ConcSdouble
    ONE: ConcSdouble
    MINUS_ONE: ConcSdouble
    def __init__(self, value: float) -> None

SymSdouble(Sdouble, SymSnumber)
SymSdoubleLeaf(SymSdouble, SymSprimitiveLeaf)   # "Sdouble{n}"

# Sfloat mirrors Sdouble exactly; leaf id prefix = "Sfloat"
Sfloat(Sfpnumber)
    ...same structure as Sdouble but for float...
    def to_sint(self) -> Sint            # f2i
    def to_sdouble(self) -> Sdouble      # f2d
    def to_slong(self) -> Slong          # f2l

ConcSfloat(Sfloat, ConcSnumber)
    ZERO: ConcSfloat
    ONE: ConcSfloat
    MINUS_ONE: ConcSfloat

SymSfloat(Sfloat, SymSnumber)
SymSfloatLeaf(SymSfloat, SymSprimitiveLeaf)     # "Sfloat{n}"
```

---

## 3. Python Magic Method → Java Substitution Logic Mapping

All arithmetic and comparison magic methods follow the same two-path dispatch pattern:

```python
def _dispatch_binary(self, other, se_method_name: str, conc_op):
    """
    Template for all binary operator magic methods.

    1. Coerce `other` to the matching Stype if it is a plain Python number.
    2. If both operands are concrete (ConcSnumber), apply conc_op directly.
    3. Otherwise, look up the active SymbolicExecution from the thread-local
       registry and call se.<se_method_name>(self, other).
    4. If no SE is active and either operand is symbolic, raise
       MulibIllegalStateException.
    """
```

### Full Mapping Table

| Python magic method | Java equivalent in Sint | Java return type | Expression node created |
|---|---|---|---|
| `__add__` | `se.add(this, rhs)` | `Sint` | `Sum(lhs, rhs)` |
| `__sub__` | `se.sub(this, rhs)` | `Sint` | `Sub(lhs, rhs)` |
| `__mul__` | `se.mul(this, rhs)` | `Sint` | `Mul(lhs, rhs)` |
| `__truediv__` / `__floordiv__` | `se.div(this, rhs)` | `Sint` | `Div(lhs, rhs)` |
| `__mod__` | `se.mod(this, rhs)` | `Sint` | `Mod(lhs, rhs)` |
| `__neg__` | `se.neg(this)` | `Sint` | `Neg(expr)` |
| `__lt__` | `se.lt(this, rhs)` | `Sbool` | `Lt(lhs, rhs)` |
| `__le__` | `se.lte(this, rhs)` | `Sbool` | `Lte(lhs, rhs)` |
| `__gt__` | `se.gt(this, rhs)` | `Sbool` | `Lt(rhs, lhs)` |
| `__ge__` | `se.gte(this, rhs)` | `Sbool` | `Lte(rhs, lhs)` |
| `__eq__` | `se.eq(this, rhs)` | `Sbool` | `Eq(lhs, rhs)` |
| `__ne__` | `Not(se.eq(this,rhs))` | `Sbool` | `Not(Eq(lhs, rhs))` |
| `__and__` | `se.iand(this, rhs)` | `Sint` | `BitwiseAnd(lhs, rhs)` |
| `__or__` | `se.ior(this, rhs)` | `Sint` | `BitwiseOr(lhs, rhs)` |
| `__xor__` | `se.ixor(this, rhs)` | `Sint` | `BitwiseXor(lhs, rhs)` |
| `__invert__` | `se.neg(se.add(this, ONE))` | `Sint` | `Neg(Sum(expr, 1))` |
| `__lshift__` | `se.ishl(this, rhs)` | `Sint` | `ShiftLeft(lhs, rhs)` |
| `__rshift__` | `se.ishr(this, rhs)` | `Sint` | `ShiftRight(lhs, rhs)` |
| `unsigned_rshift` | `se.iushr(this, rhs)` | `Sint` | `LogicalShiftRight(lhs, rhs)` |

For **Sbool logical operations** (when both operands are `Sbool`):

| Python method | Java equivalent | Constraint node created |
|---|---|---|
| `__and__` (when rhs is Sbool) | `se.and(this, rhs)` | `And(lhs, rhs)` |
| `__or__` (when rhs is Sbool) | `se.or(this, rhs)` | `Or(lhs, rhs)` |
| `__xor__` (when rhs is Sbool) | `se.xor(this, rhs)` | `Xor(lhs, rhs)` |
| `bool_not()` | `se.not(this)` | `Not(c)` |
| `__bool__` | `se.bool_choice(this)` | creates path split |

### Concrete-only fast path

When both `self` and `other` are `ConcSnumber` (no SE call needed):

```python
# Sint.__add__ concrete fast-path
if isinstance(self, ConcSint) and isinstance(other, ConcSint):
    return Sint.conc_sint(self.value + other.value)
```

For **comparison operators** the fast path returns `ConcSbool.TRUE` or `ConcSbool.FALSE`.

### Coercion rules

When `other` is a plain Python `int` (or `float` for fp types):
- In `Sint.__add__(self, other: int)`: coerce `other` → `Sint.conc_sint(other)` before dispatch
- In `Sdouble.__add__(self, other: float)`: coerce → `Sdouble.conc_sdouble(other)`
- Raise `TypeError` if `other` is neither the matching Stype nor a compatible Python primitive

---

## 4. Concrete vs. Symbolic Variants

### ConcSint — immutable concrete integer

```python
class ConcSint(Sint, ConcSnumber):
    __slots__ = ("_value",)

    _LOW_CACHE = -128
    _HIGH_CACHE = 127
    # _CACHE populated at class-definition time; see §7 for thread-safety note

    def __init__(self, value: int) -> None:
        object.__setattr__(self, "_value", value)

    def __setattr__(self, name, value):
        raise AttributeError("ConcSint is immutable")

    @property
    def value(self) -> int:
        return self._value

    # value accessors perform Java-style truncation:
    def short_val(self) -> int:    # (value + 32768) % 65536 - 32768
    def byte_val(self) -> int:     # (value + 128) % 256 - 128
    def char_val(self) -> str:     # chr(value & 0xFFFF)

    def __hash__(self) -> int:
        return self._value         # matches Java hashCode()

    def __eq__(self, other) -> "Sbool":
        if isinstance(other, ConcSint):
            return ConcSbool.TRUE if self._value == other._value else ConcSbool.FALSE
        if isinstance(other, int):
            return ConcSbool.TRUE if self._value == other else ConcSbool.FALSE
        if isinstance(other, Sint):
            # at least one side is symbolic — delegate
            se = _get_se_or_raise()
            return se.eq(self, other)
        return NotImplemented

    # Expression interface (Sint is also an Expression)
    @property
    def is_fp(self) -> bool:
        return False

    def __repr__(self) -> str:
        return str(self._value)
```

### SymSint — symbolic int wrapping an Expression

```python
class SymSint(Sint, SymSnumber):
    __slots__ = ("_represented_expression", "_hash")

    def __init__(self, represented_expression: Expression) -> None:
        object.__setattr__(self, "_represented_expression", represented_expression)
        object.__setattr__(self, "_hash", hash(represented_expression))

    def __setattr__(self, name, value):
        raise AttributeError("SymSint is immutable")

    @property
    def represented_expression(self) -> Expression:
        return self._represented_expression

    @property
    def is_fp(self) -> bool:
        return self._represented_expression.is_fp

    def __repr__(self) -> str:
        return f"SymSint{{{self._represented_expression!r}}}"

    def __eq__(self, other) -> "Sbool":
        # object-level equality: compare represented expressions
        # (used for deduplication in caches)
        if type(other) is not type(self):
            return NotImplemented
        # Return ConcSbool for pure structural identity
        same = self._represented_expression == other._represented_expression
        return ConcSbool.TRUE if same else ConcSbool.FALSE
        # NOTE: when used in SE context (if sym_a == sym_b:), the caller
        # should use Sint.__eq__ which goes through SE, not this method.

    def __hash__(self) -> int:
        return self._hash
```

**Important:** `SymSint.__eq__` above returns `ConcSbool` for *object identity* semantics
(two `SymSint`s are the same object if they wrap the same expression). The
symbolic equality constraint is produced by `Sint.__eq__` (the parent class
method) which calls through the SE.

### SymSintLeaf — named free variable

```python
class SymSintLeaf(SymSint, SymSprimitiveLeaf):
    _next_id: itertools.count = itertools.count(1)   # class-level; GIL protects in CPython

    def __init__(self, explicit_id: int | None = None) -> None:
        n = explicit_id if explicit_id is not None else next(SymSintLeaf._next_id)
        # Use the parent __init__ with self as the represented_expression
        # (the leaf IS its own expression, like Java's `representedExpression = this`)
        object.__setattr__(self, "_id", f"Sint{n}")
        object.__setattr__(self, "_represented_expression", self)
        object.__setattr__(self, "_hash", hash(f"Sint{n}"))

    @property
    def id(self) -> str:
        return self._id

    def __repr__(self) -> str:
        return self._id

    def __eq__(self, other) -> "Sbool":
        if type(other) is not SymSintLeaf:
            return NotImplemented
        same = self._id == other._id
        return ConcSbool.TRUE if same else ConcSbool.FALSE

    def __hash__(self) -> int:
        return self._hash
```

### ConcSbool — singleton concrete booleans

```python
class ConcSbool(Sbool, ConcSnumber):
    # Singletons created at class-definition end
    TRUE: "ConcSbool"
    FALSE: "ConcSbool"

    def __init__(self, value: bool) -> None:
        object.__setattr__(self, "_value", value)

    def __bool__(self) -> bool:
        return self._value    # No SE needed; no choice point created

    def __hash__(self) -> int:
        return int(self._value)   # 0 or 1
```

### SymSbool — symbolic bool wrapping a Constraint

```python
class SymSbool(Sbool, SymSnumber):
    __slots__ = ("_represented_constraint", "_hash")

    def __init__(self, represented_constraint: Constraint) -> None:
        object.__setattr__(self, "_represented_constraint", represented_constraint)
        object.__setattr__(self, "_hash", hash(represented_constraint))

    @property
    def represented_constraint(self) -> Constraint:
        return self._represented_constraint

    @property
    def represented_expression(self) -> Expression:
        return self   # SymSbool is both a Constraint and an Expression

    @property
    def is_fp(self) -> bool:
        return False

    def __bool__(self) -> bool:
        # Delegates to SE — see §5
        se = _get_se_or_raise()
        return se.bool_choice(self)
```

### SymSboolLeaf — named symbolic boolean

```python
class SymSboolLeaf(SymSbool, SymSprimitiveLeaf):
    _next_id: itertools.count = itertools.count(1)

    def __init__(self, explicit_id: int | None = None) -> None:
        n = explicit_id if explicit_id is not None else next(SymSboolLeaf._next_id)
        object.__setattr__(self, "_id", f"Sbool{n}")
        # The leaf's represented_constraint is itself
        object.__setattr__(self, "_represented_constraint", self)
        object.__setattr__(self, "_hash", hash(f"Sbool{n}"))
```

---

## 5. `__bool__` on `SymSbool` — Choice Points

### Design

Python's `if`, `while`, `assert`, and `not` all call `__bool__`. On a `SymSbool`, this must
create a **choice point** in the current symbolic execution path.

### Implementation of `_se_context.py`

```python
# mulib_python/substitutions/_se_context.py
"""Thread-local registry for the active SymbolicExecution instance."""

from __future__ import annotations
import threading
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from mulib_python.symbolic_execution import SymbolicExecution   # Phase 3

_local = threading.local()


def get_se() -> "SymbolicExecution | None":
    """Return the SymbolicExecution active in the current thread, or None."""
    return getattr(_local, "se", None)


def set_se(se: "SymbolicExecution") -> None:
    """Bind se as the active SymbolicExecution for the current thread."""
    _local.se = se


def clear_se() -> None:
    """Remove the active SymbolicExecution for the current thread."""
    _local.se = None


def _get_se_or_raise() -> "SymbolicExecution":
    """Return the active SE or raise MulibIllegalStateException."""
    se = get_se()
    if se is None:
        from mulib_python.exceptions import MulibIllegalStateException
        raise MulibIllegalStateException(
            "No active SymbolicExecution found in this thread. "
            "Symbolic operations must occur inside a search region."
        )
    return se
```

### `Sbool.__bool__` full implementation

```python
# In Sbool base class (sint.py)
def __bool__(self) -> bool:
    raise NotImplementedError("Sbool.__bool__ must be overridden by subclasses")

# In ConcSbool:
def __bool__(self) -> bool:
    return self._value     # trivially known; no choice point

# In SymSbool (covers both SymSbool and SymSboolLeaf):
def __bool__(self) -> bool:
    from mulib_python.substitutions._se_context import _get_se_or_raise
    se = _get_se_or_raise()
    return se.bool_choice(self)   # SE records path condition branch and returns True or False
```

### How Phase 3 (SymbolicExecution) will implement `bool_choice`

This is not implemented in Phase 2, but the contract is:

```python
# Phase 3 stub (for reference only; not to be implemented in Phase 2)
class SymbolicExecution:
    def bool_choice(self, sbool: Sbool) -> bool:
        """
        1. If sbool is ConcSbool: return sbool.value directly.
        2. Add constraint sbool == True to current path condition.
        3. Push the negated branch (sbool == False) onto the backtrack stack.
        4. Return True (the "taken" branch).
        On backtrack: pop the stack, add sbool == False, return False.
        """
```

### `Sint` choice methods

The `lt_choice`, `lte_choice`, etc. methods on `Sint` are convenience wrappers:

```python
# Sint.lt_choice(rhs=None) is shorthand for:
#   (self < rhs).__bool__()  if rhs is not None
#   (self < Sint.conc_sint(0)).__bool__()  if rhs is None
def lt_choice(self, rhs: Sint | None = None) -> bool:
    if rhs is None:
        rhs = Sint.conc_sint(0)
    return bool(self.__lt__(rhs))   # triggers SymSbool.__bool__ → choice point
```

---

## 6. `ValueFactory` — Python Design

### File: `mulib_python/value_factory.py`

The `ValueFactory` becomes an abstract base class. The `SymbolicValueFactory` is the default
concrete implementation for Phase 2 (concolic support deferred to Phase 4).

```python
# mulib_python/value_factory.py
from __future__ import annotations
import abc
import threading
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from mulib_python.substitutions.primitives.sint import (
        Sint, Sbool, Sbyte, Schar, Sshort
    )
    from mulib_python.substitutions.primitives.slong import Slong
    from mulib_python.substitutions.primitives.sdouble import Sdouble
    from mulib_python.substitutions.primitives.sfloat import Sfloat
    from mulib_python.substitutions._se_context import SymbolicExecution


class ValueFactory(abc.ABC):
    """Abstract factory for creating symbolic primitive values.

    All implementations must be thread-safe.  The counters maintained by
    SymbolicExecution (getNextNumberSymSintLeaf, etc.) are used to ensure that
    symbolic leaf variables created across different runs of the same path are
    identical (same id string), enabling solver result reuse.
    """

    # ── primitive leaf constructors ──
    @abc.abstractmethod
    def sym_sint(self, se: "SymbolicExecution") -> "Sint": ...

    @abc.abstractmethod
    def sym_sdouble(self, se: "SymbolicExecution") -> "Sdouble": ...

    @abc.abstractmethod
    def sym_sfloat(self, se: "SymbolicExecution") -> "Sfloat": ...

    @abc.abstractmethod
    def sym_sbool(self, se: "SymbolicExecution") -> "Sbool": ...

    @abc.abstractmethod
    def sym_slong(self, se: "SymbolicExecution") -> "Slong": ...

    @abc.abstractmethod
    def sym_sshort(self, se: "SymbolicExecution") -> "Sshort": ...

    @abc.abstractmethod
    def sym_sbyte(self, se: "SymbolicExecution") -> "Sbyte": ...

    @abc.abstractmethod
    def sym_schar(self, se: "SymbolicExecution") -> "Schar": ...

    # ── bounded leaf constructors ──
    @abc.abstractmethod
    def sym_sint_bounded(self, se: "SymbolicExecution", lb: "Sint", ub: "Sint") -> "Sint": ...

    @abc.abstractmethod
    def sym_sdouble_bounded(self, se: "SymbolicExecution", lb: "Sdouble", ub: "Sdouble") -> "Sdouble": ...

    @abc.abstractmethod
    def sym_sfloat_bounded(self, se: "SymbolicExecution", lb: "Sfloat", ub: "Sfloat") -> "Sfloat": ...

    @abc.abstractmethod
    def sym_slong_bounded(self, se: "SymbolicExecution", lb: "Slong", ub: "Slong") -> "Slong": ...

    @abc.abstractmethod
    def sym_sshort_bounded(self, se: "SymbolicExecution", lb: "Sshort", ub: "Sshort") -> "Sshort": ...

    @abc.abstractmethod
    def sym_sbyte_bounded(self, se: "SymbolicExecution", lb: "Sbyte", ub: "Sbyte") -> "Sbyte": ...

    @abc.abstractmethod
    def sym_schar_bounded(self, se: "SymbolicExecution", lb: "Schar", ub: "Schar") -> "Schar": ...

    # ── expression-wrapping constructors ──
    @abc.abstractmethod
    def wrapping_sym_sint(self, se: "SymbolicExecution", expression) -> "Sint": ...

    @abc.abstractmethod
    def wrapping_sym_sdouble(self, se: "SymbolicExecution", expression) -> "Sdouble": ...

    @abc.abstractmethod
    def wrapping_sym_sfloat(self, se: "SymbolicExecution", expression) -> "Sfloat": ...

    @abc.abstractmethod
    def wrapping_sym_slong(self, se: "SymbolicExecution", expression) -> "Slong": ...

    @abc.abstractmethod
    def wrapping_sym_sshort(self, se: "SymbolicExecution", expression) -> "Sshort": ...

    @abc.abstractmethod
    def wrapping_sym_sbyte(self, se: "SymbolicExecution", expression) -> "Sbyte": ...

    @abc.abstractmethod
    def wrapping_sym_schar(self, se: "SymbolicExecution", expression) -> "Schar": ...

    @abc.abstractmethod
    def wrapping_sym_sbool(self, se: "SymbolicExecution", constraint) -> "Sbool": ...

    # ── cmp (LCMP / FCMP / DCMP) ──
    @abc.abstractmethod
    def cmp(self, se: "SymbolicExecution", n0, n1) -> "Sint": ...


class SymbolicValueFactory(ValueFactory):
    """
    Thread-safe factory that caches SymSprimitiveLeaf instances.

    The Java implementation uses StampedLock per type.  Python uses
    per-list threading.Lock objects since Python's GIL does not guarantee
    atomicity of list index operations in all interpreters.

    Each type maintains a list of created leaves.  A leaf at index `n` has
    id-suffix `n+1` (ids are 1-based, matching Java's AtomicLong starting
    at 0 then incrementing).  When SE requests leaf number `n`, if index
    `n-1` exists in the list we return it; otherwise we create it and append.
    """

    def __init__(self) -> None:
        # per-type locks and leaf caches
        self._sint_lock = threading.Lock()
        self._sint_leaves: list[Sint.SymSintLeaf] = []

        self._sdouble_lock = threading.Lock()
        self._sdouble_leaves: list[Sdouble.SymSdoubleLeaf] = []

        self._sfloat_lock = threading.Lock()
        self._sfloat_leaves: list[Sfloat.SymSfloatLeaf] = []

        self._sbool_lock = threading.Lock()
        self._sbool_leaves: list[Sbool.SymSboolLeaf] = []

        self._slong_lock = threading.Lock()
        self._slong_leaves: list[Slong.SymSlongLeaf] = []

        self._sshort_lock = threading.Lock()
        self._sshort_leaves: list[Sshort.SymSshortLeaf] = []

        self._sbyte_lock = threading.Lock()
        self._sbyte_leaves: list[Sbyte.SymSbyteLeaf] = []

        self._schar_lock = threading.Lock()
        self._schar_leaves: list[Schar.SymScharLeaf] = []

    def _get_or_create(self, lock, cache, leaf_factory, idx):
        """Return cache[idx] if it exists, else create and append until we have it."""
        with lock:
            while len(cache) <= idx:
                cache.append(leaf_factory(len(cache) + 1))
            return cache[idx]

    def sym_sint(self, se: "SymbolicExecution") -> "Sint":
        idx = se.get_next_number_sym_sint_leaf()   # 0-based index
        return self._get_or_create(
            self._sint_lock,
            self._sint_leaves,
            lambda n: Sint.new_input_symbolic_sint(n),
            idx,
        )

    # ... identical pattern for sym_sdouble, sym_sfloat, sym_sbool, sym_slong,
    #     sym_sshort, sym_sbyte, sym_schar

    def sym_sint_bounded(self, se, lb, ub):
        leaf = self.sym_sint(se)
        se.add_new_constraint(lb.__le__(leaf).__and__(leaf.__le__(ub)))
        return leaf

    # ... identical bounded pattern for all types

    def wrapping_sym_sint(self, se, expression):
        return Sint.new_expression_symbolic_sint(expression)

    # ... identical for all wrapping_sym_* methods

    def wrapping_sym_sbool(self, se, constraint):
        return Sbool.new_constraint_sbool(constraint)

    def cmp(self, se, n0, n1):
        """
        Implements LCMP/FCMP/DCMP semantics:
          if n0 > n1: return Sint(1)
          if n0 == n1: return Sint(0)
          if n0 < n1: return Sint(-1)

        For symbolic: returns ExpressionIte(Lt(n1,n0), Sint(1),
                                  ExpressionIte(Eq(n0,n1), Sint(0), Sint(-1)))
        """
        ...
```

---

## 7. Python-Specific Design Decisions

### 7.1 Immutability

All `ConcSnumber` and `SymSnumber` instances are **immutable after construction**:
- Override `__setattr__` to raise `AttributeError`
- Use `__slots__` to prevent dynamic attribute addition
- Use `object.__setattr__(self, name, value)` only inside `__init__`

Rationale: Matches the Java design; allows safe sharing of instances across threads.

### 7.2 Thread Safety

| Java mechanism | Python equivalent |
|---|---|
| `AtomicLong nextId` per class | `itertools.count` per class (GIL-protected in CPython; use `threading.Lock`-wrapped counter for PyPy/Jython portability) |
| `StampedLock` per leaf-type list in `SymbolicValueFactory` | `threading.Lock` per leaf-type list |
| `ThreadLocal<SymbolicExecution>` | `threading.local()` in `_se_context.py` |

**Counter implementation** — for strict thread-safety across all Python implementations:

```python
import threading

class _AtomicCounter:
    """Thread-safe incrementing counter (portable across CPython, PyPy)."""
    __slots__ = ("_value", "_lock")

    def __init__(self, start: int = 1) -> None:
        self._value = start - 1
        self._lock = threading.Lock()

    def next(self) -> int:
        with self._lock:
            self._value += 1
            return self._value
```

Use `_AtomicCounter` for `SymSintLeaf._next_id`, etc.

### 7.3 `ConcSint` Cache

Matches Java's `[-128, 127]` range. Built once at class-definition time (no lock needed after import):

```python
class ConcSint(Sint, ConcSnumber):
    _LOW_CACHE: int = -128
    _HIGH_CACHE: int = 127
    _CACHE: tuple  # assigned after class body

# After class definition:
ConcSint._CACHE = tuple(ConcSint.__new__(ConcSint) ... for i in range(-128, 128))
# Populate _value via object.__setattr__ in a loop.

# Special singletons:
ConcSint.MINUS_ONE = ConcSint._CACHE[127]    # index -1 + 128 = 127
ConcSint.ZERO      = ConcSint._CACHE[128]    # index 0 + 128 = 128
ConcSint.ONE       = ConcSint._CACHE[129]    # index 1 + 128 = 129
```

### 7.4 `__eq__` Returning `Sbool` — Design Rationale

**Decision:** `Sint.__eq__` (and all other `Snumber.__eq__`) returns an `Sbool`.

Rationale:
- Makes `if sint_a == sint_b:` work naturally through `Sbool.__bool__` → choice point.
- Consistent with Z3 Python API conventions.
- Concrete `ConcSbool.TRUE/FALSE.__bool__()` returns plain `True`/`False`, so
  `ConcSint(3) == ConcSint(3)` evaluates to `True` in boolean contexts without SE.

**Consequence for containers:**
- `__hash__` for `ConcSint` uses `self.value` (so equal concrete ints have equal hashes).
- `__hash__` for `SymSint` uses `hash(self._represented_expression)` (structural).
- `__hash__` for `SymSintLeaf` uses `hash(self._id)` (identity by name).
- Using symbolic values as dict keys works correctly when you use the **same object** as key.
  Cross-object structural equality in dict lookup will call `__eq__` → `SymSbool.__bool__` →
  SE choice point, which is an unusual use case. Document this behaviour.

### 7.5 Java `float` vs. `double` in Python

Python has a single `float` type (IEEE-754 double precision). Despite this:
- `ConcSfloat` stores its value as a Python `float` but is a *distinct type* from `ConcSdouble`.
- `ConcSfloat.float_val()` and `ConcSdouble.double_val()` both return Python `float`.
- Type identity (`isinstance(x, Sfloat)` vs `isinstance(x, Sdouble)`) is preserved for
  solver backends that need to distinguish 32-bit vs 64-bit float.

### 7.6 Java `char` in Python

Java `char` is a 16-bit unsigned integer. `ConcSchar`:
- Stores an `int` in range `[0, 65535]`.
- `char_val()` returns `chr(self.value)` (a single-character Python str).
- `conc_schar` accepts either an `int` code point or a single-char `str`.

### 7.7 Java `byte`/`short` Overflow Semantics

Java arithmetic on `byte`/`short` promotes to `int` before the operation.
`ConcSbyte` and `ConcSshort` do NOT overflow on arithmetic — operations are
inherited from `Sint` and return `ConcSint` (or `SymSint`).  Truncation only
happens in the `byte_val()` / `short_val()` accessor methods.

### 7.8 `Sbool` Dual-Role as `Constraint`

`SymSbool` inherits from both `Sbool` (Sint subclass) and must satisfy the `Constraint`
ABC from Phase 1. Specifically, `Constraint` requires `__repr__`, `__eq__`, `__hash__`.
`SymSbool` satisfies these.

`ConcSbool` similarly implements `Constraint`: its `__repr__` returns `"true"` or `"false"`,
which means it can appear directly in constraint trees (e.g., `And(ConcSbool.TRUE, some_constraint)`).

`SymSboolLeaf.represented_constraint` returns `self` (the leaf IS the constraint), matching
the Java pattern where `SymSboolLeaf` calls `super()` with no arguments and the inner
`SymSbool` constructor sets `this.representedConstraint = this`.

### 7.9 `is_fp` on Snumber subclasses

| Class | `is_fp` |
|---|---|
| `Sint`, `Sbool`, `Sbyte`, `Schar`, `Sshort` | `False` (final in `Sint`) |
| `Slong` | `False` (set in `Slong`) |
| `Sfloat`, `Sdouble` | `True` (final in `Sfpnumber`) |
| `SymSint`, `SymSlong`, etc. | delegates to `represented_expression.is_fp` |
| `SymSintLeaf` | `False` (leaf's `is_fp` = its type's `is_fp`) |

### 7.10 `SymSbool.represented_expression` returns `self`

In Java, `SymSbool.getRepresentedExpression()` returns `this`. In Python:

```python
@property
def represented_expression(self) -> Expression:
    return self   # SymSbool satisfies both Expression and Constraint ABCs
```

This is valid because `SymSbool` inherits `Expression` via `Snumber`.

---

## 8. `__init__.py` Exports

### `mulib_python/substitutions/__init__.py`

```python
from mulib_python.substitutions.markers import Substituted, Sym, Conc
from mulib_python.substitutions._se_context import get_se, set_se, clear_se
from mulib_python.substitutions.primitives import (
    Sprimitive, SymSprimitive, SymSprimitiveLeaf,
    Snumber, ConcSnumber, SymSnumber, AbstractSnumber, Sfpnumber,
    Sint, ConcSint, SymSint, SymSintLeaf,
    Sbool, ConcSbool, SymSbool, SymSboolLeaf,
    Sbyte, ConcSbyte, SymSbyte, SymSbyteLeaf,
    Schar, ConcSchar, SymSchar, SymScharLeaf,
    Sshort, ConcSshort, SymSshort, SymSshortLeaf,
    Slong, ConcSlong, SymSlong, SymSlongLeaf,
    Sdouble, ConcSdouble, SymSdouble, SymSdoubleLeaf,
    Sfloat, ConcSfloat, SymSfloat, SymSfloatLeaf,
)
```

### `mulib_python/substitutions/primitives/__init__.py`

Re-exports all public names from each sub-module (see above).

### `mulib_python/value_factory.py`

```python
from mulib_python.value_factory import ValueFactory, SymbolicValueFactory
```

### Update `mulib_python/__init__.py`

Append:
```python
from mulib_python.substitutions import *     # or explicit names
from mulib_python.value_factory import ValueFactory, SymbolicValueFactory
```

---

## 9. Import Order & Circular Import Avoidance

The dependency graph (arrows = "imports from"):

```
_se_context.py      → exceptions.py
markers.py          → (nothing)
sprimitive.py       → markers.py
snumber.py          → sprimitive.py, expressions.py, constraints.py
sint.py             → snumber.py, _se_context.py, expressions.py, constraints.py
slong.py            → snumber.py, _se_context.py, expressions.py
sdouble.py          → snumber.py, _se_context.py, expressions.py, sint.py  (for Sint return types)
sfloat.py           → snumber.py, _se_context.py, expressions.py, sint.py, sdouble.py
value_factory.py    → sint.py, slong.py, sdouble.py, sfloat.py
```

**Critical:** `sint.py` must not import from `slong.py`, `sdouble.py`, or `sfloat.py` at module
level (they would create a cycle). Use `TYPE_CHECKING` guards and string annotations for all
forward references to those types in `sint.py`.

```python
# sint.py — safe pattern for type hints to sibling modules
from __future__ import annotations
from typing import TYPE_CHECKING
if TYPE_CHECKING:
    from mulib_python.substitutions.primitives.slong import Slong
    from mulib_python.substitutions.primitives.sdouble import Sdouble
    from mulib_python.substitutions.primitives.sfloat import Sfloat
```

`sdouble.py` and `sfloat.py` **do** import `Sint` at runtime (for `cmp()` return type and
type-conversion methods), so they import `sint.py` directly.

---

## 10. Unit Tests

Create `mulib_python/tests/test_substitutions.py` (or a `tests/` sub-package) with:

1. **`test_conc_sint_cache`** — verify `Sint.conc_sint(n) is Sint.conc_sint(n)` for n in [-128,127]
   and distinct objects outside that range.
2. **`test_conc_sint_arithmetic`** — `ConcSint(3) + ConcSint(4)` returns `ConcSint(7)` (no SE needed).
3. **`test_sym_sint_leaf_ids`** — sequential leaves get distinct, incrementing ids.
4. **`test_sym_sint_arithmetic_raises_without_se`** — `SymSintLeaf() + ConcSint(1)` raises
   `MulibIllegalStateException` when no SE is active.
5. **`test_conc_sbool_no_choice_point`** — `bool(ConcSbool.TRUE)` is `True` without SE.
6. **`test_sym_sbool_bool_raises_without_se`** — `bool(SymSboolLeaf())` raises
   `MulibIllegalStateException`.
7. **`test_conc_int_comparison`** — `ConcSint(3) < ConcSint(5)` is `ConcSbool.TRUE`;
   `bool(ConcSint(3) < ConcSint(5))` is `True`.
8. **`test_sbool_constraint_protocol`** — `SymSboolLeaf()` is an instance of `Constraint`.
9. **`test_byte_truncation`** — `ConcSint(200).byte_val() == -56`.
10. **`test_short_truncation`** — `ConcSint(40000).short_val() == -25536`.
11. **`test_char_val`** — `ConcSchar(65).char_val() == 'A'`.
12. **`test_sym_sbool_leaf_is_own_constraint`** — `leaf.represented_constraint is leaf`.
13. **`test_is_fp`** — `ConcSfloat(1.0).is_fp` is `True`; `ConcSint(1).is_fp` is `False`.
14. **`test_value_factory_caches_leaves`** — two calls to `sym_sint(se)` with the same SE
    counter value return the same object.
15. **`test_thread_safety_of_leaf_counter`** — 100 threads each create 100 `SymSintLeaf()`s;
    all 10,000 ids are distinct.

---

## 11. Summary of Deviations from Java

| Java feature | Python adaptation |
|---|---|
| Static nested classes (`Sint.ConcSint`) | Top-level classes in module; use module for namespacing |
| `AtomicLong nextId` | `_AtomicCounter` per class (threading.Lock-based) |
| `StampedLock` in SymbolicValueFactory | `threading.Lock` per type-list |
| Java `float` vs `double` types | Two distinct Python classes; both store Python `float` |
| Java `char` (16-bit unsigned int) | `ConcSchar` stores int code point; `char_val()` returns `str` |
| `equals()` returning Java `boolean` | `__eq__` returns `Sbool` (see §7.4) |
| `Sbool extends Sint` | `Sbool(Sint, Constraint)` — Python multiple inheritance |
| `SymSboolLeaf extends SymSbool implements SymSprimitiveLeaf` | Same via Python MRO |
| SE passed explicitly to every method | SE retrieved from `threading.local()` |
| `ValueFactory.getInstance(config, ...)` | `SymbolicValueFactory()` constructor; factory selection deferred to Phase 3/4 |
| Sarrays (`SintSarray`, etc.) | Out of scope for Phase 2; defined as stubs in Phase 4 |
| `PartnerClass`, `PartnerClassObject` | Out of scope for Phase 2 |
