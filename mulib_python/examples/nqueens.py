"""N-Queens for small N (default 4)."""

from mulib_python.api import free_int, assume, get_solutions


def solve(n: int = 4):
    qs = [free_int(f"q{i}", 0, n - 1) for i in range(n)]
    for i in range(n):
        for j in range(i + 1, n):
            assume(qs[i] != qs[j])
            assume(qs[i] - qs[j] != i - j)
            assume(qs[i] - qs[j] != j - i)
    return qs


def main():
    sols = get_solutions(lambda: solve(4), max_solutions=1)
    if not sols:
        print("no solution")
        return
    print(sols[0].labels)


if __name__ == "__main__":
    main()
