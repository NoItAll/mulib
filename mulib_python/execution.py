"""SymbolicExecution: thread-local state and bool_choice mechanism."""

from __future__ import annotations

import threading
from collections import deque

import z3

from .exceptions import Fail, MulibIllegalStateException
from .primitives import Sbool, ConcSbool, SymSbool
from .search import ChoiceOption, PathSolution, SearchTree


class Backtrack(Exception):
    """Raised when the current path is infeasible — caught by the search loop."""


_LOCAL = threading.local()


class SymbolicExecution:
    """Per-execution state: tracks current path, solver, choice tree."""

    def __init__(self, solver, tree: SearchTree, predetermined: list[ChoiceOption]):
        self.solver = solver
        self.tree = tree
        # FIFO of choice options we are forced to follow on this run.
        self.predetermined: deque[ChoiceOption] = deque(predetermined)
        # Track current option in the tree (deepest visited so far).
        self.current_option = predetermined[-1] if predetermined else tree.root
        # Pushes we've accumulated so we can pop them at the end.
        self._push_count = 0

    # --- thread-local registry ---------------------------------------------
    @staticmethod
    def get():
        return getattr(_LOCAL, "se", None)

    @staticmethod
    def _set(se):
        _LOCAL.se = se

    @staticmethod
    def _remove():
        if hasattr(_LOCAL, "se"):
            del _LOCAL.se

    # ------------------------------------------------------------------
    def push_constraint(self, z3_constraint):
        """Permanently add a constraint at the current node (used by `assume`)."""
        self.solver.add(z3_constraint)

    def bool_choice(self, sbool: Sbool) -> bool:
        if isinstance(sbool, ConcSbool):
            return sbool.value
        constraint = sbool.z3_expr()

        # Predetermined replay
        if self.predetermined:
            forced = self.predetermined.popleft()
            self.current_option = forced
            return bool(forced.taken_value)

        # New choice point: create both branches.
        true_opt = ChoiceOption(self.current_option, constraint, True)
        false_opt = ChoiceOption(self.current_option, z3.Not(constraint), False)
        self.current_option.set_children(true_opt, false_opt)

        # Try TRUE branch first
        self.solver.push()
        self._push_count += 1
        self.solver.add(constraint)
        if self.solver.check():
            true_opt.set_sat()
            self.tree.queue(false_opt)
            self.current_option = true_opt
            return True
        # TRUE infeasible — pop and try FALSE
        self.solver.pop()
        self._push_count -= 1
        true_opt.set_unsat()

        self.solver.push()
        self._push_count += 1
        self.solver.add(z3.Not(constraint))
        if self.solver.check():
            false_opt.set_sat()
            self.current_option = false_opt
            return False
        self.solver.pop()
        self._push_count -= 1
        false_opt.set_unsat()
        raise Backtrack()


# ---------------------------------------------------------------------------
# Search driver
# ---------------------------------------------------------------------------

def run_search(func, args, kwargs, *, max_solutions: int = 100, max_paths: int = 10000):
    from .solver import Z3SolverManager

    solver = Z3SolverManager()
    tree = SearchTree()

    solutions: list[PathSolution] = []
    # First run starts with no predetermined path.
    next_path: list[ChoiceOption] | None = []
    # Extra blocking clauses (z3 BoolRefs) injected to enumerate more models
    # along the same physical execution path.
    blocking: list = []
    paths_explored = 0

    while True:
        if next_path is None:
            co = tree.poll_next()
            if co is None:
                break
            next_path = SearchTree.path_to(co)
            blocking = []  # different path → blocking clauses don't apply

        # Reset solver and replay predetermined constraints.
        solver.reset()
        # Clear labels from prior runs
        solver._labels = {}
        for opt in next_path:
            if opt.z3_constraint is not None:
                solver.add(opt.z3_constraint)
        for b in blocking:
            solver.add(b)
        if not solver.check():
            if next_path:
                next_path[-1].set_unsat()
            next_path = None
            blocking = []
            continue

        se = SymbolicExecution(solver, tree, next_path)
        SymbolicExecution._set(se)
        had_branch = False
        try:
            try:
                ret = func(*args, **kwargs)
                # Final feasibility check: constraints added during func may
                # make the path infeasible (or blocking clauses may have ruled
                # out remaining models).
                if not solver.check():
                    raise Fail()
                labels = solver.labels()
                # Did this run create any new choice options at the leaf?
                had_branch = bool(se.current_option.children)
                solutions.append(PathSolution(ret, labels, se.current_option))
                if len(solutions) >= max_solutions:
                    break
                if not had_branch:
                    # No more branches; enumerate further models on same path
                    # by adding a blocking clause for the labelled values.
                    if labels and solver._labels:
                        eqs = []
                        for name, sym in solver._labels.items():
                            v = labels.get(name)
                            if v is None:
                                continue
                            e = sym.z3_expr() if hasattr(sym, "z3_expr") else sym
                            if isinstance(v, bool):
                                eqs.append(e == z3.BoolVal(v))
                            elif isinstance(v, int):
                                eqs.append(e == z3.IntVal(v))
                            elif isinstance(v, float):
                                eqs.append(e == z3.RealVal(v))
                        if eqs:
                            blocking.append(z3.Not(z3.And(*eqs)) if len(eqs) > 1
                                            else z3.Not(eqs[0]))
                            paths_explored += 1
                            if paths_explored < max_paths:
                                continue  # rerun same predetermined path with blocker
            except Fail:
                pass
            except Backtrack:
                pass
        finally:
            SymbolicExecution._remove()

        next_path = None
        blocking = []
        paths_explored += 1
        if paths_explored >= max_paths:
            break

    return solutions
