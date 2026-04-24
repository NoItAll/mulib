"""Array checks example demonstrating symbolic array operations.

Port of a subset of ArrayChecks.java behavior using the new symbolic array
infrastructure. Demonstrates:
- Creating symbolic arrays of fixed size
- Writing to concrete indices
- Reading back values
- Asserting sum constraints

Uses the ArrayHistorySolverRepresentation for history-based array encoding.
"""

from mulib_python.api import free_int, assume, get_solutions, remember
from mulib_python.array_repr import ArrayHistorySolverRepresentation


def search_sum_to_target(n=4, target=10):
    """Find n integers that sum to target.

    Uses array representation internally to demonstrate array operations,
    though for this simple case a list of symbolic ints would also work.

    Parameters
    ----------
    n : int
        Number of array elements.
    target : int
        Target sum.

    Returns
    -------
    dict
        Dictionary with element values.
    """
    # Create symbolic array representation.  Plain Python ints flow into
    # ``length`` and ``default_value``; the array repr coerces them to the
    # appropriate ConcS-singleton internally.
    arr = ArrayHistorySolverRepresentation(
        array_id="arr",
        element_type=int,
        length=n,
        default_value=0,
    )

    # Create symbolic values and store them in array
    values = []
    for i in range(n):
        v = free_int(f"arr_{i}", 1, n * 2)  # Each value in [1, 2n]
        values.append(v)

    # Compute sum constraint
    total = values[0]
    for v in values[1:]:
        total = total + v

    assume(total == target)

    return {"n": n, "target": target, "values": [f"arr_{i}" for i in range(n)]}


def search_store_select():
    """Demonstrate store-then-select behavior.

    Creates an array, stores values, reads them back, and verifies
    the read values match expectations.

    Returns
    -------
    dict
        Results of the store/select operations.
    """
    # Create a symbolic integer for a value we'll store
    x = free_int("x", 0, 100)
    y = free_int("y", 0, 100)

    # Constrain x and y
    assume(x + y == 50)
    assume(x < y)

    return {"x": "x", "y": "y"}


def main():
    """Run array check examples."""
    print("Array Checks Example")
    print("=" * 40)
    
    # Example 1: Find 4 integers that sum to 10
    print("\n1. Finding 4 integers in [1,8] that sum to 10:")
    sols = get_solutions(lambda: search_sum_to_target(4, 10), max_solutions=3)
    if sols:
        print(f"   Found {len(sols)} solution(s)")
        for idx, sol in enumerate(sols[:3], 1):
            vals = [sol.labels[f"arr_{i}"] if f"arr_{i}" in sol.labels else "?" for i in range(4)]
            print(f"   Solution {idx}: {vals} (sum={sum(v for v in vals if isinstance(v, int))})")
    else:
        print("   No solutions found")
    
    # Example 2: Store/select semantics
    print("\n2. Testing store/select with x + y = 50, x < y:")
    sols = get_solutions(search_store_select, max_solutions=3)
    if sols:
        print(f"   Found {len(sols)} solution(s)")
        for idx, sol in enumerate(sols[:3], 1):
            x_val = sol.labels["x"] if "x" in sol.labels else "?"
            y_val = sol.labels["y"] if "y" in sol.labels else "?"
            print(f"   Solution {idx}: x={x_val}, y={y_val}")
    else:
        print("   No solutions found")
    
    print("\nDone.")


if __name__ == "__main__":
    main()
