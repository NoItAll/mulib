"""Symbolic-array example: find 4 positive ints in [1, 9] summing to 20.

This example demonstrates using symbolic integers with constraints.
We create an array of symbolic integers, constrain each to [1, 9],
and find values that sum to 20.
"""

from mulib_python.api import free_int, assume, get_solutions


def search():
    """Search for 4 integers in [1, 9] summing to 20."""
    n = 4
    arr = [free_int(f"a{i}", 1, 9) for i in range(n)]
    total = arr[0] + arr[1] + arr[2] + arr[3]
    assume(total == 20)
    return arr


def main():
    """Run the search and print results."""
    print("Finding 4 integers in [1, 9] that sum to 20...")
    sols = get_solutions(search, max_solutions=5)
    if not sols:
        print("No solution found")
        return
    print(f"Found {len(sols)} solution(s)")
    for i, sol in enumerate(sols):
        vals = [sol.labels[f"a{j}"] for j in range(4)]
        print(f"  Solution {i+1}: {vals} (sum={sum(vals)})")


if __name__ == "__main__":
    main()
