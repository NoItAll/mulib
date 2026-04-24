"""Absolute value multiplication example.

Demonstrates symbolic computation with conditional logic.
Takes two symbolic integers, constrains one to be negative and one non-negative,
then computes abs(i0) * abs(i1).

This example shows how to use constraints rather than explicit branching.
"""

from mulib_python.api import free_int, assume, get_solutions


def search_abs_mul():
    """Find inputs for abs(i0) * abs(i1) = target.
    
    We explore different sign combinations by setting up constraints.
    
    Returns
    -------
    dict
        Dictionary with input values and result.
    """
    from mulib_python.substitutions.primitives.sint import ConcSint
    
    # Create symbolic inputs with bounded ranges
    i0 = free_int("i0", -10, 10)
    i1 = free_int("i1", -10, 10)
    
    # Constrain i0 to be negative and i1 to be positive for this path
    assume(i0 < ConcSint(0))
    assume(i1 > ConcSint(0))
    
    # Compute abs - since i0 < 0 and i1 > 0:
    abs_i0 = -i0
    abs_i1 = i1
    
    # Compute product and constrain it
    product = abs_i0 * abs_i1
    assume(product == ConcSint(12))  # Find values where |i0| * |i1| = 12
    
    return {"i0": "i0", "i1": "i1"}


def main():
    """Run symbolic execution and show solutions."""
    print("Finding i0 < 0, i1 > 0 such that |i0| * |i1| = 12...")
    print()
    
    sols = get_solutions(search_abs_mul, max_solutions=10)
    
    # Deduplicate
    seen = set()
    unique_sols = []
    for sol in sols:
        key = (sol.labels.get("i0"), sol.labels.get("i1"))
        if key not in seen:
            seen.add(key)
            unique_sols.append(sol)
    
    print(f"Found {len(unique_sols)} solution(s):")
    for idx, sol in enumerate(unique_sols, 1):
        i0_val = sol.labels.get("i0", "?")
        i1_val = sol.labels.get("i1", "?")
        product = abs(i0_val) * abs(i1_val) if isinstance(i0_val, int) and isinstance(i1_val, int) else "?"
        print(f"  Solution {idx}: i0={i0_val}, i1={i1_val}, |i0|*|i1|={product}")


if __name__ == "__main__":
    main()


if __name__ == "__main__":
    main()


if __name__ == "__main__":
    main()


if __name__ == "__main__":
    main()
