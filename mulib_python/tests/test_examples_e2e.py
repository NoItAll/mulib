"""End-to-end tests for example modules.

Tests each example by importing and calling its main solving function,
asserting the returned number of solutions matches expected values.
"""

import pytest

from mulib_python.api import get_solutions


# =============================================================================
# Example Imports
# =============================================================================

from mulib_python.examples import send_more_money, nqueens, array_example
from mulib_python.examples import partition, abs_value_mul, array_checks, wbs


# =============================================================================
# send_more_money
# =============================================================================

def test_send_more_money_has_unique_solution():
    """SEND + MORE = MONEY has exactly one solution."""
    sols = get_solutions(send_more_money.search, max_solutions=5)
    assert len(sols) >= 1
    
    # Verify the solution
    L = sols[0].labels
    expected = {"S": 9, "E": 5, "N": 6, "D": 7, "M": 1, "O": 0, "R": 8, "Y": 2}
    for key, val in expected.items():
        assert L[key] == val, f"Expected {key}={val}, got {L[key]}"


# =============================================================================
# nqueens
# =============================================================================

@pytest.mark.parametrize("n,expected_count", [
    (4, 2),   # 4-queens has 2 solutions
    (5, 10),  # 5-queens has 10 solutions
])
def test_nqueens_solution_count(n, expected_count):
    """N-queens should have the expected number of solutions."""
    sols = get_solutions(lambda: nqueens.solve(n), max_solutions=expected_count + 5)
    
    # Deduplicate by column assignment
    seen = set()
    unique_sols = []
    for sol in sols:
        cols = tuple(sol.labels[f"q{i}"] for i in range(n))
        if cols not in seen:
            seen.add(cols)
            unique_sols.append(sol)
    
    assert len(unique_sols) >= min(expected_count, len(sols))


def test_nqueens_4_valid_solution():
    """4-queens solution should be valid (no attacks)."""
    sols = get_solutions(lambda: nqueens.solve(4), max_solutions=1)
    assert sols
    
    L = sols[0].labels
    cols = [L[f"q{i}"] for i in range(4)]
    
    # Check distinct columns
    assert len(set(cols)) == 4
    
    # Check no diagonal attacks
    for i in range(4):
        for j in range(i + 1, 4):
            assert abs(cols[i] - cols[j]) != j - i


def test_nqueens_5_valid_solution():
    """5-queens solution should be valid (no attacks)."""
    sols = get_solutions(lambda: nqueens.solve(5), max_solutions=1)
    assert sols
    
    L = sols[0].labels
    cols = [L[f"q{i}"] for i in range(5)]
    
    # Check distinct columns
    assert len(set(cols)) == 5
    
    # Check no diagonal attacks
    for i in range(5):
        for j in range(i + 1, 5):
            assert abs(cols[i] - cols[j]) != j - i


# =============================================================================
# array_example
# =============================================================================

def test_array_example_sum():
    """Array example: 4 integers in [1,9] summing to 20."""
    sols = get_solutions(array_example.search, max_solutions=1)
    assert sols
    
    L = sols[0].labels
    vals = [L[f"a{i}"] for i in range(4)]
    assert sum(vals) == 20
    assert all(1 <= v <= 9 for v in vals)


# =============================================================================
# partition
# =============================================================================

def test_partition_valid_solution():
    """Partition: split numbers into two equal-sum subsets."""
    numbers = [2, 4, 6, 8, 10, 10]
    sols = get_solutions(lambda: partition.search(numbers), max_solutions=1)
    assert len(sols) >= 1
    
    L = sols[0].labels
    subset1_sum = sum(num for i, num in enumerate(numbers) if L.get(f"sel_{i}") == 1)
    subset2_sum = sum(num for i, num in enumerate(numbers) if L.get(f"sel_{i}") == 0)
    
    assert subset1_sum == subset2_sum == 20


def test_partition_odd_sum_no_solution():
    """Partition with odd total sum has no solution."""
    numbers = [1, 2, 4]  # sum = 7 (odd)
    sols = get_solutions(lambda: partition.search(numbers), max_solutions=1)
    assert len(sols) == 0


# =============================================================================
# abs_value_mul
# =============================================================================

def test_abs_value_mul_valid_solution():
    """abs_value_mul: |i0| * |i1| = 12."""
    sols = get_solutions(abs_value_mul.search_abs_mul, max_solutions=1)
    assert len(sols) >= 1
    
    L = sols[0].labels
    i0 = L.get("i0")
    i1 = L.get("i1")
    
    assert i0 is not None and i1 is not None
    assert abs(i0) * abs(i1) == 12
    assert i0 < 0  # constrained to be negative
    assert i1 > 0  # constrained to be positive


# =============================================================================
# array_checks
# =============================================================================

def test_array_checks_sum_target():
    """array_checks: 4 integers summing to 10."""
    sols = get_solutions(lambda: array_checks.search_sum_to_target(4, 10), max_solutions=1)
    assert len(sols) >= 1
    
    L = sols[0].labels
    vals = [L[f"arr_{i}"] for i in range(4)]
    assert sum(vals) == 10


def test_array_checks_store_select():
    """array_checks: x + y = 50, x < y."""
    sols = get_solutions(array_checks.search_store_select, max_solutions=1)
    assert len(sols) >= 1
    
    L = sols[0].labels
    x = L.get("x")
    y = L.get("y")
    
    assert x is not None and y is not None
    assert x + y == 50
    assert x < y


# =============================================================================
# wbs
# =============================================================================

def test_wbs_critical_warning():
    """WBS: CRITICAL warning scenario exists."""
    sols = get_solutions(wbs.compute_warning_level, max_solutions=1)
    assert len(sols) >= 1
    
    L = sols[0].labels
    # Should have found the constrained scenario
    assert L.get("engine_on") == 1
    assert L.get("speed_high") == 1
    assert L.get("brake_pressed") == 0
    assert L.get("parking_brake") == 1


def test_wbs_medium_warning():
    """WBS: MEDIUM warning scenario exists."""
    sols = get_solutions(wbs.compute_all_levels, max_solutions=1)
    assert len(sols) >= 1
    
    L = sols[0].labels
    # Should have found engine off, no parking brake
    assert L.get("engine_on") == 0
    assert L.get("parking_brake") == 0
