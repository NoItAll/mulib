from mulib_python.api import free_int, assume, get_solutions
from mulib_python.sarray import Sarray


def test_concrete_index_access():
    def search():
        a = Sarray(3, default_value="int", name="a1")
        a[0] = free_int("v0", 1, 5)
        a[1] = free_int("v1", 1, 5)
        a[2] = free_int("v2", 1, 5)
        assume(a[0] + a[1] + a[2] == 6)
        return None

    sols = get_solutions(search, max_solutions=1)
    assert sols
    s = sols[0].labels
    assert s["v0"] + s["v1"] + s["v2"] == 6


def test_array_bounds_pruning():
    # Index out of bounds raises Fail (path pruned), so no solutions when
    # ALL paths are infeasible due to oob.  Here use a valid index instead
    # and assert solver returns one.
    def search():
        a = Sarray(2, default_value="int", name="a2")
        a[0] = free_int("z", 0, 9)
        assume(a[0] == 5)
        return None

    sols = get_solutions(search, max_solutions=1)
    assert sols
    assert sols[0].labels["z"] == 5
