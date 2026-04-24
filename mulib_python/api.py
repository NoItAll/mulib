"""High-level user-facing API for mulib_python.

This module provides the main entry points for using mulib_python:
- Free variable factories: free_int, free_bool, free_double, etc.
- Path control: assume, fail
- Search drivers: get_solutions, get_path_solutions, Mulib class
"""

from __future__ import annotations

import itertools
from typing import Any, Callable, Iterator, List, Optional, TYPE_CHECKING

from mulib_python.exceptions import Fail, MulibIllegalStateException, Backtrack
from mulib_python.substitutions._se_context import _get_se, _get_se_or_raise
from mulib_python.constraints import Constraint, Lte, Lt
from mulib_python.solution import Solution, PathSolution

if TYPE_CHECKING:
    from mulib_python.executor.symbolic_execution import SymbolicExecution
    from mulib_python.config import MulibConfig
    from mulib_python.substitutions.primitives.sint import (
        Sint, Sbool, Sbyte, Schar, Sshort
    )
    from mulib_python.substitutions.primitives.slong import Slong
    from mulib_python.substitutions.primitives.sdouble import Sdouble
    from mulib_python.substitutions.primitives.sfloat import Sfloat


_AUTO_NAMES = itertools.count()


def _se_or_raise() -> "SymbolicExecution":
    """Get the current SymbolicExecution context or raise an error."""
    return _get_se_or_raise()


# ---------------------------------------------------------------------------
# Free-variable factories
# ---------------------------------------------------------------------------

def free_int(
    name: Optional[str] = None,
    lo: Optional[int] = None,
    hi: Optional[int] = None,
) -> "Sint":
    """Create a free (unconstrained) symbolic integer.

    Parameters
    ----------
    name : str, optional
        Name for the symbolic variable. If None, auto-generated.
    lo : int, optional
        Lower bound constraint.
    hi : int, optional
        Upper bound constraint.

    Returns
    -------
    Sint
        A fresh symbolic integer.
    """
    se = _se_or_raise()
    if name is None:
        name = f"_int_{next(_AUTO_NAMES)}"
    
    sym = se.sym_int(name)
    
    # Add bounds as constraints
    if lo is not None:
        from mulib_python.substitutions.primitives.sint import ConcSint
        se.add_constraint(Lte(ConcSint(lo), sym))
    if hi is not None:
        from mulib_python.substitutions.primitives.sint import ConcSint
        se.add_constraint(Lte(sym, ConcSint(hi)))
    
    # Remember for labeling
    se.remember(name, sym)
    return sym


def free_long(
    name: Optional[str] = None,
    lo: Optional[int] = None,
    hi: Optional[int] = None,
) -> "Slong":
    """Create a free symbolic long."""
    se = _se_or_raise()
    if name is None:
        name = f"_long_{next(_AUTO_NAMES)}"
    
    sym = se.sym_long(name)
    
    if lo is not None:
        from mulib_python.substitutions.primitives.slong import ConcSlong
        se.add_constraint(Lte(ConcSlong(lo), sym))
    if hi is not None:
        from mulib_python.substitutions.primitives.slong import ConcSlong
        se.add_constraint(Lte(sym, ConcSlong(hi)))
    
    se.remember(name, sym)
    return sym


def free_bool(name: Optional[str] = None) -> "Sbool":
    """Create a free symbolic boolean.

    Parameters
    ----------
    name : str, optional
        Name for the symbolic variable.

    Returns
    -------
    Sbool
        A fresh symbolic boolean.
    """
    se = _se_or_raise()
    if name is None:
        name = f"_bool_{next(_AUTO_NAMES)}"
    
    sym = se.sym_bool(name)
    se.remember(name, sym)
    return sym


def free_double(
    name: Optional[str] = None,
    lo: Optional[float] = None,
    hi: Optional[float] = None,
) -> "Sdouble":
    """Create a free symbolic double.

    Parameters
    ----------
    name : str, optional
        Name for the symbolic variable.
    lo : float, optional
        Lower bound constraint.
    hi : float, optional
        Upper bound constraint.

    Returns
    -------
    Sdouble
        A fresh symbolic double.
    """
    se = _se_or_raise()
    if name is None:
        name = f"_dbl_{next(_AUTO_NAMES)}"
    
    sym = se.sym_double(name)
    
    if lo is not None:
        from mulib_python.substitutions.primitives.sdouble import ConcSdouble
        se.add_constraint(Lte(ConcSdouble(lo), sym))
    if hi is not None:
        from mulib_python.substitutions.primitives.sdouble import ConcSdouble
        se.add_constraint(Lte(sym, ConcSdouble(hi)))
    
    se.remember(name, sym)
    return sym


def free_float(
    name: Optional[str] = None,
    lo: Optional[float] = None,
    hi: Optional[float] = None,
) -> "Sfloat":
    """Create a free symbolic float."""
    se = _se_or_raise()
    if name is None:
        name = f"_flt_{next(_AUTO_NAMES)}"
    
    sym = se.sym_float(name)
    
    if lo is not None:
        from mulib_python.substitutions.primitives.sfloat import ConcSfloat
        se.add_constraint(Lte(ConcSfloat(lo), sym))
    if hi is not None:
        from mulib_python.substitutions.primitives.sfloat import ConcSfloat
        se.add_constraint(Lte(sym, ConcSfloat(hi)))
    
    se.remember(name, sym)
    return sym


def free_byte(name: Optional[str] = None) -> "Sbyte":
    """Create a free symbolic byte (bounded to [-128, 127])."""
    se = _se_or_raise()
    if name is None:
        name = f"_byte_{next(_AUTO_NAMES)}"
    
    sym = se.sym_byte(name)
    se.remember(name, sym)
    return sym


def free_char(name: Optional[str] = None) -> "Schar":
    """Create a free symbolic char (bounded to [0, 65535])."""
    se = _se_or_raise()
    if name is None:
        name = f"_char_{next(_AUTO_NAMES)}"
    
    sym = se.sym_char(name)
    se.remember(name, sym)
    return sym


def free_short(name: Optional[str] = None) -> "Sshort":
    """Create a free symbolic short (bounded to [-32768, 32767])."""
    se = _se_or_raise()
    if name is None:
        name = f"_short_{next(_AUTO_NAMES)}"
    
    sym = se.sym_short(name)
    se.remember(name, sym)
    return sym


# ---------------------------------------------------------------------------
# Path control
# ---------------------------------------------------------------------------

def assume(constraint: Any) -> None:
    """Assume a constraint holds on the current path.

    If the constraint cannot hold, the path is pruned (Fail is raised).

    Parameters
    ----------
    constraint : Constraint or Sbool or bool
        The constraint to assume.
    """
    from mulib_python.substitutions.primitives.sint import ConcSbool, SymSbool
    
    se = _se_or_raise()
    
    # Handle concrete booleans
    if isinstance(constraint, ConcSbool):
        if not constraint._value:
            raise Fail()
        return
    if isinstance(constraint, bool):
        if not constraint:
            raise Fail()
        return
    
    # Handle symbolic constraints
    if isinstance(constraint, SymSbool):
        actual_constraint = constraint._represented_constraint
    elif isinstance(constraint, Constraint):
        actual_constraint = constraint
    else:
        raise TypeError(f"Cannot assume {type(constraint).__name__}")
    
    # Check feasibility
    if not se.check(actual_constraint):
        raise Fail()
    
    # Add the constraint
    se.add_constraint(actual_constraint)


def fail() -> None:
    """Immediately prune the current path."""
    raise Fail()


def backtrack() -> None:
    """Force backtracking on the current path."""
    raise Backtrack("User-initiated backtrack")


def remember(name: str, value: Any) -> None:
    """Remember a value for later labeling in solutions.

    Parameters
    ----------
    name : str
        The name to associate with the value.
    value : Any
        The value to remember (can be symbolic).
    """
    se = _se_or_raise()
    se.remember(name, value)


# ---------------------------------------------------------------------------
# Search drivers
# ---------------------------------------------------------------------------

def get_solutions(
    func: Callable,
    *args,
    config: Optional["MulibConfig"] = None,
    max_solutions: int = 1,
    **kwargs
) -> List[Solution]:
    """Run symbolic execution and return solutions.

    This is the main entry point for running a search region.

    Parameters
    ----------
    func : callable
        The search region function to execute.
    *args : tuple
        Positional arguments for the function.
    config : MulibConfig, optional
        Configuration for the execution.
    max_solutions : int
        Maximum number of solutions to find.
    **kwargs : dict
        Keyword arguments for the function.

    Returns
    -------
    list[Solution]
        List of solutions found.
    """
    from mulib_python.executor.mulib_executor import MulibExecutor
    from mulib_python.config import MulibConfig
    
    if config is None:
        config = MulibConfig.builder().set_max_paths(max_solutions * 10).build()
    
    def search_fn(se: "SymbolicExecution") -> Any:
        return func(*args, **kwargs)
    
    executor = MulibExecutor(config)
    solutions = executor.run(search_fn)
    
    return solutions[:max_solutions]


def get_path_solutions(
    func: Callable,
    *args,
    config: Optional["MulibConfig"] = None,
    max_solutions: int = 100,
    max_paths: int = 10000,
    **kwargs
) -> List[PathSolution]:
    """Run symbolic execution and return path solutions with metadata.

    Parameters
    ----------
    func : callable
        The search region function.
    *args : tuple
        Positional arguments.
    config : MulibConfig, optional
        Configuration.
    max_solutions : int
        Maximum solutions to return.
    max_paths : int
        Maximum paths to explore.
    **kwargs : dict
        Keyword arguments.

    Returns
    -------
    list[PathSolution]
        Solutions with path metadata.
    """
    from mulib_python.executor.mulib_executor import MulibExecutor
    from mulib_python.config import MulibConfig
    
    if config is None:
        config = (MulibConfig.builder()
            .set_max_paths(max_paths)
            .build())
    
    def search_fn(se: "SymbolicExecution") -> Any:
        return func(*args, **kwargs)
    
    executor = MulibExecutor(config)
    solutions = executor.run(search_fn)
    
    # Convert to PathSolution if needed
    path_solutions = []
    for sol in solutions[:max_solutions]:
        if isinstance(sol, PathSolution):
            path_solutions.append(sol)
        else:
            path_solutions.append(PathSolution(
                return_value=sol.return_value,
                labels=sol.labels,
                path_constraints=(),
                depth=0,
            ))
    
    return path_solutions


class Mulib:
    """High-level class interface for symbolic execution.

    Example
    -------
    >>> mulib = Mulib()
    >>> def my_func():
    ...     x = mulib.free_int("x")
    ...     if x > 5:
    ...         return x * 2
    ...     return x
    >>> solutions = mulib.run(my_func)
    """

    def __init__(self, config: Optional["MulibConfig"] = None) -> None:
        from mulib_python.config import MulibConfig
        self._config = config or MulibConfig.default()
        self._se: Optional["SymbolicExecution"] = None

    @property
    def config(self) -> "MulibConfig":
        return self._config

    def run(self, func: Callable, *args, **kwargs) -> List[Solution]:
        """Run symbolic execution on the given function."""
        return get_solutions(func, *args, config=self._config, **kwargs)

    def run_iter(self, func: Callable, *args, **kwargs) -> Iterator[Solution]:
        """Run symbolic execution, yielding solutions lazily."""
        from mulib_python.executor.mulib_executor import run_mulib_iter
        
        def search_fn(se: "SymbolicExecution") -> Any:
            return func(*args, **kwargs)
        
        yield from run_mulib_iter(search_fn, self._config)

    # Convenience methods (require active SE context)
    def free_int(self, name: Optional[str] = None, lo: Optional[int] = None, hi: Optional[int] = None) -> "Sint":
        return free_int(name, lo, hi)

    def free_bool(self, name: Optional[str] = None) -> "Sbool":
        return free_bool(name)

    def free_double(self, name: Optional[str] = None, lo: Optional[float] = None, hi: Optional[float] = None) -> "Sdouble":
        return free_double(name, lo, hi)

    def assume(self, constraint: Any) -> None:
        assume(constraint)

    def fail(self) -> None:
        fail()


# Convenience singleton
_default_mulib: Optional[Mulib] = None


def get_mulib(config: Optional["MulibConfig"] = None) -> Mulib:
    """Get or create the default Mulib instance."""
    global _default_mulib
    if _default_mulib is None or config is not None:
        _default_mulib = Mulib(config)
    return _default_mulib
