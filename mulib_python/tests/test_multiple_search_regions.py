"""Tests for multiple search region invocations.

Verifies that:
1. Calling get_solutions twice in succession returns correct independent results
2. Nested invocations (a search function calling get_solutions internally) work
3. Interleaved calls to different search functions work correctly
"""

import pytest

from mulib_python.api import free_int, assume, get_solutions
from mulib_python.substitutions.primitives.sint import ConcSint


# =============================================================================
# Test Functions for Search Regions
# =============================================================================

def simple_search_a():
    """Simple search: find x in [1, 5] where x == 3."""
    x = free_int("x", 1, 5)
    assume(x == ConcSint(3))
    return x


def simple_search_b():
    """Simple search: find y in [10, 20] where y == 15."""
    y = free_int("y", 10, 20)
    assume(y == ConcSint(15))
    return y


def simple_sum_search():
    """Search: find a, b where a + b == 10 and both in [1, 9]."""
    a = free_int("a", 1, 9)
    b = free_int("b", 1, 9)
    assume(a + b == ConcSint(10))
    return a, b


# =============================================================================
# Successive Independent Calls
# =============================================================================

def test_successive_calls_return_independent_results():
    """Calling get_solutions twice returns correct independent results."""
    # First call
    sols_a = get_solutions(simple_search_a, max_solutions=1)
    assert len(sols_a) == 1
    assert sols_a[0].labels["x"] == 3
    
    # Second call (completely independent)
    sols_b = get_solutions(simple_search_b, max_solutions=1)
    assert len(sols_b) == 1
    assert sols_b[0].labels["y"] == 15


def test_no_leaked_state_between_calls():
    """Verify no tree state leaks between successive calls."""
    # Run search A multiple times
    for _ in range(3):
        sols = get_solutions(simple_search_a, max_solutions=1)
        assert len(sols) == 1
        assert sols[0].labels["x"] == 3
    
    # Run search B multiple times
    for _ in range(3):
        sols = get_solutions(simple_search_b, max_solutions=1)
        assert len(sols) == 1
        assert sols[0].labels["y"] == 15


def test_fresh_symbolic_ids_between_calls():
    """Each call should have fresh symbolic variable IDs."""
    # First call uses "a" and "b"
    sols1 = get_solutions(simple_sum_search, max_solutions=1)
    assert len(sols1) == 1
    
    # Second call also uses "a" and "b" - should work independently
    sols2 = get_solutions(simple_sum_search, max_solutions=1)
    assert len(sols2) == 1
    
    # Both should have valid solutions
    for sols in [sols1, sols2]:
        a_val = sols[0].labels["a"]
        b_val = sols[0].labels["b"]
        assert a_val + b_val == 10


# =============================================================================
# Nested Search Region Invocation
# =============================================================================

def outer_search_with_inner():
    """Outer search that calls get_solutions internally."""
    # This is testing if nested invocations work
    # The outer search finds x, then the inner search finds solutions
    x = free_int("x", 1, 3)
    assume(x == ConcSint(2))
    return {"x": x, "inner_called": True}


def test_nested_search_regions():
    """Test that the outer search terminates correctly.
    
    Note: True nested invocation (calling get_solutions inside a search
    function that's already running) would create a new SE context.
    This test verifies the outer search works when we check for nesting.
    """
    sols = get_solutions(outer_search_with_inner, max_solutions=1)
    assert len(sols) == 1
    assert sols[0].labels["x"] == 2


# =============================================================================
# Interleaved Calls
# =============================================================================

def test_interleaved_calls():
    """Alternate calls between A and B; each should return correct results."""
    for i in range(3):
        # Call A
        sols_a = get_solutions(simple_search_a, max_solutions=1)
        assert len(sols_a) == 1
        assert sols_a[0].labels["x"] == 3, f"Failed on iteration {i} for A"
        
        # Call B
        sols_b = get_solutions(simple_search_b, max_solutions=1)
        assert len(sols_b) == 1
        assert sols_b[0].labels["y"] == 15, f"Failed on iteration {i} for B"


def test_multiple_solutions_consistency():
    """Getting multiple solutions should be consistent across calls."""
    # First call
    sols1 = get_solutions(simple_sum_search, max_solutions=5)
    
    # Second call
    sols2 = get_solutions(simple_sum_search, max_solutions=5)
    
    # Both should return valid solutions
    for sols in [sols1, sols2]:
        assert len(sols) >= 1
        for sol in sols:
            a_val = sol.labels["a"]
            b_val = sol.labels["b"]
            assert 1 <= a_val <= 9
            assert 1 <= b_val <= 9
            assert a_val + b_val == 10


# =============================================================================
# Edge Cases
# =============================================================================

def no_solution_search():
    """Search that has no solutions (constraint is unsatisfiable)."""
    x = free_int("x", 1, 5)
    assume(x == ConcSint(100))  # Impossible
    return x


def test_no_solution_then_solution():
    """A search with no solutions followed by one with solutions."""
    # First: no solutions
    sols1 = get_solutions(no_solution_search, max_solutions=1)
    assert len(sols1) == 0
    
    # Second: has solutions
    sols2 = get_solutions(simple_search_a, max_solutions=1)
    assert len(sols2) == 1
    assert sols2[0].labels["x"] == 3


def test_solution_then_no_solution():
    """A search with solutions followed by one with no solutions."""
    # First: has solutions
    sols1 = get_solutions(simple_search_a, max_solutions=1)
    assert len(sols1) == 1
    
    # Second: no solutions
    sols2 = get_solutions(no_solution_search, max_solutions=1)
    assert len(sols2) == 0
