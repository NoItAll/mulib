import pytest

from mulib_python.examples import send_more_money, nqueens, array_example
from mulib_python.api import get_solutions


def test_send_more_money():
    sols = get_solutions(send_more_money.search, max_solutions=1)
    assert sols, "expected at least one SEND+MORE=MONEY solution"
    L = sols[0].labels
    # The Labels object contains 'return' (the returned dict) plus the labeled values
    # We check the labeled values for S, E, N, D, M, O, R, Y
    expected = {"S": 9, "E": 5, "N": 6, "D": 7, "M": 1, "O": 0, "R": 8, "Y": 2}
    for key, val in expected.items():
        assert L[key] == val, f"Expected {key}={val}, got {L[key]}"


def test_nqueens_4():
    sols = get_solutions(lambda: nqueens.solve(4), max_solutions=1)
    assert sols
    qs = sols[0].labels
    cols = [qs[f"q{i}"] for i in range(4)]
    # check distinct columns and no diag attacks
    assert len(set(cols)) == 4
    for i in range(4):
        for j in range(i + 1, 4):
            assert abs(cols[i] - cols[j]) != j - i


@pytest.mark.skip(reason="Sarray integration with search not yet complete")
def test_array_example():
    sols = get_solutions(array_example.search, max_solutions=1)
    assert sols
    L = sols[0].labels
    vals = [L[f"a{i}"] for i in range(4)]
    assert sum(vals) == 20
    assert all(1 <= v <= 9 for v in vals)
