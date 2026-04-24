"""SEND + MORE = MONEY classic cryptarithm."""

from mulib_python.api import free_int, assume, get_solutions


def search():
    s = free_int("S", 1, 9)
    e = free_int("E", 0, 9)
    n = free_int("N", 0, 9)
    d = free_int("D", 0, 9)
    m = free_int("M", 1, 9)
    o = free_int("O", 0, 9)
    r = free_int("R", 0, 9)
    y = free_int("Y", 0, 9)
    vs = [s, e, n, d, m, o, r, y]
    for i in range(len(vs)):
        for j in range(i):
            assume(vs[i] != vs[j])
    send  = s * 1000 + e * 100 + n * 10 + d
    more  = m * 1000 + o * 100 + r * 10 + e
    money = m * 10000 + o * 1000 + n * 100 + e * 10 + y
    assume(send + more == money)
    return {"S": s, "E": e, "N": n, "D": d, "M": m, "O": o, "R": r, "Y": y}


def main():
    sols = get_solutions(search, max_solutions=1)
    if not sols:
        print("no solution")
        return
    print(sols[0].labels)


if __name__ == "__main__":
    main()
