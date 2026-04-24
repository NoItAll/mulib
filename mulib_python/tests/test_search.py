"""Tests for search infrastructure."""

import pytest
import time
from mulib_python.search.strategy import SearchStrategy
from mulib_python.search.trees.choice import (
    TreeNode, Choice, ChoiceOption, PathSolutionNode, Fail, ExceededBudget,
    NodeState, TreeNodeVisitor,
)
from mulib_python.search.trees.deques import (
    DFSDeque, BFSDeque, RandomDeque, IDDFSDeque,
    create_deque_for_strategy,
)
from mulib_python.search.trees.search_tree import (
    SearchTree, TreeStatisticsVisitor, collect_statistics,
)
from mulib_python.search.budget.budget import (
    Budget, NullBudget, TimeBudget, CountingBudget,
    PathBudget, DepthBudget, CompositeBudget,
)
from mulib_python.search.budget.budget_manager import BudgetManager


# =============================================================================
# SearchStrategy Tests
# =============================================================================

def test_search_strategy_enum():
    assert SearchStrategy.DFS.is_depth_first()
    assert SearchStrategy.BFS.is_breadth_first()
    assert SearchStrategy.RANDOM.is_depth_first() is False


# =============================================================================
# TreeNode Tests
# =============================================================================

def test_choice_creation():
    choice = Choice(choice_type="bool", depth=0)
    assert choice.choice_type == "bool"
    assert choice.depth == 0
    assert choice.state == NodeState.OPEN


def test_choice_option_creation():
    choice = Choice(choice_type="bool", depth=0)
    option_true = ChoiceOption(option_index=0, value=True, parent=choice, depth=1)
    option_false = ChoiceOption(option_index=1, value=False, parent=choice, depth=1)
    
    choice.add_child(option_true)
    choice.add_child(option_false)
    
    assert len(choice.children) == 2
    assert choice.get_option(0).value is True
    assert choice.get_option(1).value is False


def test_node_states():
    choice = Choice(choice_type="bool")
    assert choice.state == NodeState.OPEN
    
    choice.set_state(NodeState.ACTIVE)
    assert choice.state == NodeState.ACTIVE
    
    choice.set_state(NodeState.CLOSED)
    assert choice.state == NodeState.CLOSED


def test_path_solution_node():
    sol = PathSolutionNode(return_value=42, path_constraints=())
    assert sol.return_value == 42
    assert sol.state == NodeState.CLOSED


def test_fail_node():
    fail = Fail(reason="test failure")
    assert fail.reason == "test failure"
    assert fail.state == NodeState.CLOSED


def test_exceeded_budget_node():
    exceeded = ExceededBudget(budget_type="time")
    assert exceeded.budget_type == "time"
    assert exceeded.state == NodeState.EXCEEDED


# =============================================================================
# Deque Tests
# =============================================================================

def test_dfs_deque_lifo():
    dq = DFSDeque()
    
    opt1 = ChoiceOption(option_index=0, value=1, depth=1)
    opt2 = ChoiceOption(option_index=1, value=2, depth=2)
    opt3 = ChoiceOption(option_index=2, value=3, depth=3)
    
    dq.push(opt1)
    dq.push(opt2)
    dq.push(opt3)
    
    # LIFO: should get opt3 first
    assert dq.pop().value == 3
    assert dq.pop().value == 2
    assert dq.pop().value == 1
    assert dq.pop() is None


def test_bfs_deque_fifo():
    dq = BFSDeque()
    
    opt1 = ChoiceOption(option_index=0, value=1, depth=1)
    opt2 = ChoiceOption(option_index=1, value=2, depth=2)
    opt3 = ChoiceOption(option_index=2, value=3, depth=3)
    
    dq.push(opt1)
    dq.push(opt2)
    dq.push(opt3)
    
    # FIFO: should get opt1 first
    assert dq.pop().value == 1
    assert dq.pop().value == 2
    assert dq.pop().value == 3
    assert dq.pop() is None


def test_random_deque():
    dq = RandomDeque(seed=42)
    
    for i in range(10):
        opt = ChoiceOption(option_index=i, value=i, depth=i)
        dq.push(opt)
    
    assert len(dq) == 10
    
    values = []
    while not dq.is_empty():
        values.append(dq.pop().value)
    
    # Should have all values, but not in order
    assert sorted(values) == list(range(10))


def test_iddfs_deque():
    dq = IDDFSDeque(initial_depth_limit=2)
    
    opt_shallow = ChoiceOption(option_index=0, value="shallow", depth=1)
    opt_deep = ChoiceOption(option_index=1, value="deep", depth=5)
    
    dq.push(opt_shallow)
    dq.push(opt_deep)
    
    # Should get shallow first (within depth limit)
    first = dq.pop()
    assert first.value == "shallow"
    
    # Deep option is deferred, will appear after depth limit increases
    second = dq.pop()
    assert second.value == "deep"


def test_create_deque_for_strategy():
    assert isinstance(create_deque_for_strategy(SearchStrategy.DFS), DFSDeque)
    assert isinstance(create_deque_for_strategy(SearchStrategy.BFS), BFSDeque)
    assert isinstance(create_deque_for_strategy(SearchStrategy.RANDOM), RandomDeque)
    assert isinstance(create_deque_for_strategy(SearchStrategy.IDDFS), IDDFSDeque)


# =============================================================================
# SearchTree Tests
# =============================================================================

def test_search_tree_creation():
    tree = SearchTree(strategy=SearchStrategy.DFS)
    assert tree.strategy == SearchStrategy.DFS
    assert tree.is_empty()


def test_search_tree_create_choice():
    tree = SearchTree(strategy=SearchStrategy.DFS)
    
    choice = tree.create_choice("bool", [True, False])
    
    assert tree.choice_count == 1
    assert tree.pending_count() == 2


def test_search_tree_next_option():
    tree = SearchTree(strategy=SearchStrategy.DFS)
    
    tree.create_choice("bool", [True, False])
    
    opt = tree.next_option()
    assert opt is not None
    assert opt.state == NodeState.ACTIVE


def test_search_tree_mark_solution():
    tree = SearchTree(strategy=SearchStrategy.DFS)
    
    tree.create_choice("bool", [True, False])
    tree.next_option()
    
    sol_node = tree.mark_solution(return_value=42)
    
    assert isinstance(sol_node, PathSolutionNode)
    assert sol_node.return_value == 42


def test_search_tree_mark_fail():
    tree = SearchTree(strategy=SearchStrategy.DFS)
    
    tree.create_choice("bool", [True, False])
    tree.next_option()
    
    fail_node = tree.mark_fail("test failure")
    
    assert isinstance(fail_node, Fail)
    assert fail_node.reason == "test failure"


def test_search_tree_statistics():
    tree = SearchTree(strategy=SearchStrategy.DFS)
    
    tree.create_choice("bool", [True, False])
    tree.next_option()
    tree.mark_solution(42)
    
    tree.next_option()
    tree.mark_fail("fail")
    
    stats = collect_statistics(tree)
    
    assert stats.choice_count == 1
    assert stats.option_count == 2
    assert stats.solution_count == 1
    assert stats.fail_count == 1


def test_search_tree_get_path_to_current():
    tree = SearchTree(strategy=SearchStrategy.DFS)
    
    choice = tree.create_choice("bool", [True, False])
    opt = tree.next_option()
    
    path = tree.get_path_to_current()
    
    assert len(path) == 2  # choice and option
    assert path[0] == choice
    assert path[1] == opt


# =============================================================================
# Budget Tests
# =============================================================================

def test_null_budget():
    budget = NullBudget()
    assert not budget.is_exceeded()
    assert not budget.is_incurred()


def test_time_budget():
    budget = TimeBudget(1.0)  # 1 second
    
    assert not budget.is_exceeded()
    assert budget.remaining > 0
    
    time.sleep(0.1)
    assert budget.is_incurred()
    
    # Won't be exceeded yet
    assert not budget.is_exceeded()


def test_time_budget_exceeded():
    budget = TimeBudget(0.01)  # 10ms
    time.sleep(0.02)
    assert budget.is_exceeded()


def test_counting_budget():
    budget = CountingBudget(5)
    
    assert not budget.is_exceeded()
    assert budget.remaining == 5
    
    budget.increment()
    assert budget.count == 1
    assert budget.remaining == 4
    
    budget.increment(4)
    assert budget.is_exceeded()


def test_path_budget():
    budget = PathBudget(3)
    
    budget.increment()
    budget.increment()
    assert not budget.is_exceeded()
    
    budget.increment()
    assert budget.is_exceeded()


def test_depth_budget():
    budget = DepthBudget(10)
    
    budget.set_depth(5)
    assert not budget.is_exceeded()
    
    budget.set_depth(11)
    assert budget.is_exceeded()


def test_composite_budget():
    time_budget = TimeBudget(10.0)
    path_budget = PathBudget(100)
    
    composite = CompositeBudget(time_budget, path_budget)
    
    assert not composite.is_exceeded()
    
    for _ in range(100):
        path_budget.increment()
    
    assert composite.is_exceeded()
    assert composite.get_exceeded_budget() is path_budget


def test_budget_reset():
    budget = CountingBudget(5)
    
    budget.increment(5)
    assert budget.is_exceeded()
    
    budget.reset()
    assert not budget.is_exceeded()
    assert budget.count == 0


def test_budget_copy():
    budget = CountingBudget(10)
    budget.increment(3)
    
    copy = budget.copy()
    
    assert copy.count == 3
    assert copy.limit == 10
    
    copy.increment()
    assert copy.count == 4
    assert budget.count == 3  # Original unchanged


# =============================================================================
# BudgetManager Tests
# =============================================================================

def test_budget_manager_creation():
    manager = BudgetManager()
    assert not manager.is_any_exceeded()


def test_budget_manager_time_budget():
    manager = BudgetManager()
    manager.set_global_time_budget(10.0)
    
    assert not manager.is_any_exceeded()


def test_budget_manager_path_budget():
    manager = BudgetManager()
    manager.set_path_budget(5)
    
    for _ in range(5):
        manager.increment_path_count()
    
    assert manager.is_any_exceeded()


def test_budget_manager_depth_budget():
    manager = BudgetManager()
    manager.set_depth_budget(3)
    
    manager.set_current_depth(2)
    assert not manager.is_any_exceeded()
    
    manager.set_current_depth(4)
    assert manager.is_any_exceeded()


def test_budget_manager_chaining():
    manager = (BudgetManager()
        .set_global_time_budget(60.0)
        .set_path_budget(1000)
        .set_depth_budget(50))
    
    assert not manager.is_any_exceeded()


# =============================================================================
# Visitor Pattern Tests
# =============================================================================

class CountingVisitor(TreeNodeVisitor):
    def __init__(self):
        self.counts = {"choice": 0, "option": 0, "solution": 0, "fail": 0, "exceeded": 0}
    
    def visit_choice(self, node: Choice):
        self.counts["choice"] += 1
    
    def visit_choice_option(self, node: ChoiceOption):
        self.counts["option"] += 1
    
    def visit_path_solution(self, node: PathSolutionNode):
        self.counts["solution"] += 1
    
    def visit_fail(self, node: Fail):
        self.counts["fail"] += 1
    
    def visit_exceeded_budget(self, node: ExceededBudget):
        self.counts["exceeded"] += 1


def test_visitor_pattern():
    visitor = CountingVisitor()
    
    choice = Choice(choice_type="bool")
    choice.accept(visitor)
    assert visitor.counts["choice"] == 1
    
    option = ChoiceOption(option_index=0, value=True)
    option.accept(visitor)
    assert visitor.counts["option"] == 1
    
    sol = PathSolutionNode(return_value=42)
    sol.accept(visitor)
    assert visitor.counts["solution"] == 1


# =============================================================================
# Edge Cases
# =============================================================================

def test_empty_tree_all_nodes():
    tree = SearchTree()
    nodes = list(tree.all_nodes())
    assert nodes == []


def test_tree_clear():
    tree = SearchTree()
    tree.create_choice("bool", [True, False])
    
    assert not tree.is_empty()
    
    tree.clear()
    
    assert tree.is_empty()
    assert tree.choice_count == 0
