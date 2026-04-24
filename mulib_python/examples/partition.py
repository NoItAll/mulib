"""Partition problem: Split integers into two subsets of equal sum.

Given a list of integers, find a way to partition them into two subsets
such that both subsets have the same sum. Uses symbolic integers as
selectors (0 or 1 indicating membership in the first subset).
"""

from mulib_python.api import free_int, assume, get_solutions


def search(numbers=None):
    """Search for a valid partition of numbers into two equal-sum subsets.
    
    Parameters
    ----------
    numbers : list[int], optional
        List of integers to partition. Defaults to [2, 4, 6, 8, 10, 10].
    
    Returns
    -------
    dict
        Dictionary mapping element indices to their subset.
    """
    if numbers is None:
        numbers = [2, 4, 6, 8, 10, 10]  # sum=40, target=20
    
    total = sum(numbers)
    if total % 2 != 0:
        # Odd sum cannot be partitioned equally
        assume(False)
        return None
    
    target = total // 2

    # Use symbolic integers constrained to 0 or 1 as selectors
    # selector[i] = 1 means numbers[i] goes to subset1
    selectors = [free_int(f"sel_{i}", 0, 1) for i in range(len(numbers))]

    # Build the sum: sum of numbers[i] where selector[i] == 1.
    # Plain Python ints are auto-coerced when arithmetic involves a Sint.
    first_sum = selectors[0] * numbers[0]
    for i in range(1, len(numbers)):
        first_sum = first_sum + selectors[i] * numbers[i]

    assume(first_sum == target)

    return {"numbers": numbers, "target": target}


def main():
    """Run the search and print all solutions."""
    # Use numbers with even sum so partition is possible
    numbers = [2, 4, 6, 8, 10, 10]  # sum=40, target=20
    print(f"Partitioning {numbers} into two equal-sum subsets...")
    print(f"Total sum = {sum(numbers)}, target for each subset = {sum(numbers)//2}")
    
    sols = get_solutions(lambda: search(numbers), max_solutions=20)
    
    if not sols:
        print("No valid partition exists!")
        return
    
    # Deduplicate by partition membership
    seen = set()
    unique_sols = []
    for sol in sols:
        key = tuple(sol.labels.get(f"sel_{i}", 0) for i in range(len(numbers)))
        if key not in seen:
            seen.add(key)
            unique_sols.append(sol)
    
    print(f"\nFound {len(unique_sols)} unique partition(s):")
    for idx, sol in enumerate(unique_sols, 1):
        subset1 = []
        subset2 = []
        for i, num in enumerate(numbers):
            if sol.labels.get(f"sel_{i}", 0) == 1:
                subset1.append(num)
            else:
                subset2.append(num)
        print(f"  Solution {idx}: {subset1} (sum={sum(subset1)}) | {subset2} (sum={sum(subset2)})")


if __name__ == "__main__":
    main()


if __name__ == "__main__":
    main()


if __name__ == "__main__":
    main()
