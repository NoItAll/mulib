"""Symbolic-array example: find 4 positive ints in [1, 9] summing to 20."""

from mulib_python.api import free_int, assume, get_solutions
from mulib_python.sarray import Sarray


def search():
    n = 4
    arr = Sarray(n, default_value="int", name="arr")
    # populate with named free ints
    for i in range(n):
        arr[i] = free_int(f"a{i}", 1, 9)
    total = arr[0] + arr[1] + arr[2] + arr[3]
    assume(total == 20)
    return [arr[i] for i in range(n)]


def main():
    sols = get_solutions(search, max_solutions=1)
    if not sols:
        print("no solution")
        return
    print(sols[0].labels)


if __name__ == "__main__":
    main()
