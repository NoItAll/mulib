"""High-level user-facing API for mulib_python."""

from __future__ import annotations

import itertools
import z3

from .exceptions import Fail, MulibIllegalStateException
from .primitives import (
    Sint, ConcSint, SymSint,
    Sbool, ConcSbool, SymSbool,
    Sdouble, ConcSdouble, SymSdouble,
)
from .execution import SymbolicExecution, run_search, Backtrack
from .search import PathSolution

_AUTO_NAMES = itertools.count()


def _se_or_raise():
    se = SymbolicExecution.get()
    if se is None:
        raise MulibIllegalStateException("No active SymbolicExecution; use get_solutions/get_path_solutions")
    return se


# ---------------------------------------------------------------------------
# Free-variable factories
# ---------------------------------------------------------------------------

def free_int(name: str | None = None, lo: int | None = None, hi: int | None = None) -> SymSint:
    se = _se_or_raise()
    if name is None:
        name = f"_int_{next(_AUTO_NAMES)}"
    z = z3.Int(name)
    sym = SymSint(z, name=name)
    if lo is not None:
        se.solver.add(z >= int(lo))
    if hi is not None:
        se.solver.add(z <= int(hi))
    se.solver.label(name, sym)
    return sym


def free_bool(name: str | None = None) -> SymSbool:
    se = _se_or_raise()
    if name is None:
        name = f"_bool_{next(_AUTO_NAMES)}"
    z = z3.Bool(name)
    sym = SymSbool(z, name=name)
    se.solver.label(name, sym)
    return sym


def free_double(name: str | None = None, lo: float | None = None, hi: float | None = None) -> SymSdouble:
    se = _se_or_raise()
    if name is None:
        name = f"_dbl_{next(_AUTO_NAMES)}"
    z = z3.Real(name)
    sym = SymSdouble(z, name=name)
    if lo is not None:
        se.solver.add(z >= float(lo))
    if hi is not None:
        se.solver.add(z <= float(hi))
    se.solver.label(name, sym)
    return sym


free_float = free_double


# ---------------------------------------------------------------------------
# Path control
# ---------------------------------------------------------------------------

def assume(constraint) -> None:
    """Force *constraint* to hold; prune the path if it cannot."""
    se = _se_or_raise()
    if isinstance(constraint, ConcSbool):
        if not constraint.value:
            raise Fail()
        return
    if isinstance(constraint, bool):
        if not constraint:
            raise Fail()
        return
    if isinstance(constraint, Sbool):
        z = constraint.z3_expr()
    else:
        z = constraint
    se.solver.push()
    se.solver.add(z)
    if not se.solver.check():
        se.solver.pop()
        raise Fail()
    # Keep the constraint permanently — but to make replay deterministic we
    # don't pop.  The push/pop above served only as a feasibility check.


def fail() -> None:
    raise Fail()


# ---------------------------------------------------------------------------
# Search drivers
# ---------------------------------------------------------------------------

def get_path_solutions(func, *args, max_solutions: int = 100, max_paths: int = 10000, **kwargs):
    """Run *func* under symbolic execution; return all PathSolution results."""
    return run_search(func, args, kwargs, max_solutions=max_solutions, max_paths=max_paths)


def get_solutions(func, *args, max_solutions: int = 1, **kwargs):
    """Convenience: same as get_path_solutions but with a smaller default."""
    return run_search(func, args, kwargs, max_solutions=max_solutions)
