"""Tests for execution engine."""

import pytest
from mulib_python.executor.calculation_factory import (
    CalculationFactory,
    SymbolicCalculationFactory,
    ConcolicCalculationFactory,
)
from mulib_python.executor.symbolic_execution import SymbolicExecution
from mulib_python.executor.mulib_executor import MulibExecutor, run_mulib
from mulib_python.z3_solver_manager import Z3IncrementalSolverManager
from mulib_python.search.trees.search_tree import SearchTree
from mulib_python.search.strategy import SearchStrategy
from mulib_python.search.budget.budget_manager import BudgetManager
from mulib_python.search.choice_points.choice_point_factory import SymbolicChoicePointFactory
from mulib_python.substitutions.primitives.sint import SymSintLeaf, ConcSint
from mulib_python.config import MulibConfig, MulibConfigBuilder


# =============================================================================
# CalculationFactory Tests
# =============================================================================

def test_symbolic_calculation_factory_int():
    factory = SymbolicCalculationFactory()
    
    x = factory.create_symbolic_int("x")
    assert isinstance(x, SymSintLeaf)
    assert x._id == "x"


def test_symbolic_calculation_factory_auto_name():
    factory = SymbolicCalculationFactory()
    
    x = factory.create_symbolic_int()
    y = factory.create_symbolic_int()
    
    assert x._id != y._id


def test_symbolic_calculation_factory_all_types():
    factory = SymbolicCalculationFactory()
    
    si = factory.create_symbolic_int("i")
    sl = factory.create_symbolic_long("l")
    sd = factory.create_symbolic_double("d")
    sf = factory.create_symbolic_float("f")
    sb = factory.create_symbolic_bool("b")
    sbt = factory.create_symbolic_byte("bt")
    sc = factory.create_symbolic_char("c")
    ss = factory.create_symbolic_short("s")
    
    assert si._id == "i"
    assert sl._id == "l"
    assert sd._id == "d"
    assert sf._id == "f"
    assert sb._id == "b"


def test_concolic_calculation_factory():
    factory = ConcolicCalculationFactory()
    
    x = factory.create_symbolic_int("x")
    assert isinstance(x, SymSintLeaf)


# =============================================================================
# SymbolicExecution Tests
# =============================================================================

def create_test_se():
    """Helper to create a SymbolicExecution for testing."""
    solver = Z3IncrementalSolverManager()
    tree = SearchTree(strategy=SearchStrategy.DFS)
    calc_factory = SymbolicCalculationFactory()
    choice_factory = SymbolicChoicePointFactory(solver, tree)
    budget_manager = BudgetManager()
    
    return SymbolicExecution(
        solver=solver,
        tree=tree,
        calculation_factory=calc_factory,
        choice_point_factory=choice_factory,
        budget_manager=budget_manager,
    )


def test_symbolic_execution_creation():
    se = create_test_se()
    
    assert not se.is_active
    assert se.depth == 0


def test_symbolic_execution_activation():
    se = create_test_se()
    
    se.activate()
    assert se.is_active
    
    se.deactivate()
    assert not se.is_active


def test_symbolic_execution_context_manager():
    se = create_test_se()
    
    with se:
        assert se.is_active
    
    assert not se.is_active


def test_symbolic_execution_sym_int():
    se = create_test_se()
    
    x = se.sym_int("x")
    assert isinstance(x, SymSintLeaf)
    assert x._id == "x"


def test_symbolic_execution_remember():
    se = create_test_se()
    
    se.remember("x", 42)
    
    remembered = se.get_remembered()
    assert "x" in remembered
    assert remembered["x"] == 42


def test_symbolic_execution_forget():
    se = create_test_se()
    
    se.remember("x", 42)
    se.forget("x")
    
    assert "x" not in se.get_remembered()


def test_symbolic_execution_add_constraint():
    se = create_test_se()
    
    x = se.sym_int("x")
    from mulib_python.constraints import Lt
    
    se.add_constraint(Lt(x, ConcSint(10)))
    
    # Should be able to get a label now
    assert se.is_satisfiable()


# =============================================================================
# MulibExecutor Tests
# =============================================================================

def test_executor_simple():
    config = MulibConfig.default()
    executor = MulibExecutor(config)
    
    def search_fn(se):
        return 42
    
    solutions = executor.run(search_fn)
    
    assert len(solutions) >= 1
    assert solutions[0].return_value == 42


def test_executor_with_symbolic():
    config = MulibConfigBuilder().set_max_paths(10).build()
    executor = MulibExecutor(config)
    
    def search_fn(se):
        x = se.sym_int("x")
        from mulib_python.constraints import Eq
        se.add_constraint(Eq(x, ConcSint(5)))
        se.remember("x", x)
        return x
    
    solutions = executor.run(search_fn)
    
    assert len(solutions) >= 1
    # The return value should be labeled to 5
    assert solutions[0].labels.get_label_for_id("x") == 5


def test_run_mulib_convenience():
    def search_fn(se):
        return "hello"
    
    solutions = run_mulib(search_fn)
    
    assert len(solutions) >= 1
    assert solutions[0].return_value == "hello"


# =============================================================================
# Configuration Tests
# =============================================================================

def test_config_default():
    config = MulibConfig.default()
    assert config.search_strategy == SearchStrategy.DFS


def test_config_builder():
    config = (MulibConfigBuilder()
        .set_search_strategy(SearchStrategy.BFS)
        .set_max_paths(100)
        .set_max_depth(20)
        .set_solver_timeout(5000)
        .build())
    
    assert config.search_strategy == SearchStrategy.BFS
    assert config.budget_max_paths == 100
    assert config.budget_max_depth == 20
    assert config.solver_timeout_ms == 5000


def test_config_execution_mode():
    from mulib_python.config import ExecutionMode
    
    config = (MulibConfigBuilder()
        .set_execution_mode(ExecutionMode.CONCOLIC)
        .build())
    
    assert config.execution_mode == ExecutionMode.CONCOLIC


# =============================================================================
# Budget Integration Tests
# =============================================================================

def test_executor_with_path_budget():
    config = (MulibConfigBuilder()
        .set_max_paths(2)
        .build())
    
    executor = MulibExecutor(config)
    
    def search_fn(se):
        return 42
    
    solutions = executor.run(search_fn)
    
    # Should not exceed path budget
    assert len(solutions) <= 2


# =============================================================================
# Error Handling Tests
# =============================================================================

def test_executor_handles_backtrack():
    from mulib_python.exceptions import Backtrack
    
    config = MulibConfig.default()
    executor = MulibExecutor(config)
    
    def search_fn(se):
        raise Backtrack("forced")
    
    # Should not crash
    solutions = executor.run(search_fn)
    assert len(solutions) == 0


def test_executor_handles_exception():
    config = MulibConfigBuilder().set_allow_exceptions(True).build()
    executor = MulibExecutor(config)
    
    def search_fn(se):
        raise ValueError("test error")
    
    # Should not crash (exception is caught)
    solutions = executor.run(search_fn)
    # Path should fail but not crash


# =============================================================================
# Thread Safety Tests
# =============================================================================

def test_se_context_thread_local():
    """Verify that SE context is thread-local."""
    import threading
    
    results = {}
    
    def thread_fn(thread_id):
        se = create_test_se()
        with se:
            se.remember("thread", thread_id)
            results[thread_id] = se.get_remembered().get("thread")
    
    threads = []
    for i in range(3):
        t = threading.Thread(target=thread_fn, args=(i,))
        threads.append(t)
        t.start()
    
    for t in threads:
        t.join()
    
    # Each thread should have its own remembered value
    assert results == {0: 0, 1: 1, 2: 2}
