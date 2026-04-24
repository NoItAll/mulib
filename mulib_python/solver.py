"""Z3 solver wrapper with incremental push/pop and labelling."""

from __future__ import annotations

import z3

from .primitives import Sint, Sbool, Sdouble, SymSint, SymSbool, SymSdouble


class Z3SolverManager:
    def __init__(self, config=None):
        self.config = config
        self.solver = z3.Solver()
        # Track labelled (named) free variables for model extraction.
        self._labels: dict[str, object] = {}

    def push(self):
        self.solver.push()

    def pop(self, n: int = 1):
        self.solver.pop(n)

    def reset(self):
        self.solver.reset()

    def add(self, constraint):
        """Add a constraint expressed as a Sbool / z3 BoolRef."""
        if isinstance(constraint, Sbool):
            constraint = constraint.z3_expr()
        self.solver.add(constraint)

    def check(self) -> bool:
        return self.solver.check() == z3.sat

    def model(self):
        return self.solver.model()

    def label(self, name: str, sym):
        """Register a symbolic value for label-based model extraction."""
        self._labels[name] = sym

    def labels(self) -> dict[str, object]:
        if self.solver.check() != z3.sat:
            return {}
        m = self.solver.model()
        out: dict[str, object] = {}
        for name, sym in self._labels.items():
            out[name] = self._eval(m, sym)
        return out

    @staticmethod
    def _eval(m, sym):
        e = sym.z3_expr() if hasattr(sym, "z3_expr") else sym
        v = m.eval(e, model_completion=True)
        if z3.is_int_value(v):
            return v.as_long()
        if z3.is_bool(v):
            return z3.is_true(v)
        if z3.is_rational_value(v):
            num = v.numerator_as_long()
            den = v.denominator_as_long()
            return num / den
        try:
            return v.as_long()
        except Exception:  # pragma: no cover
            return str(v)
