from mulib_python.api import free_int, assume, get_path_solutions, get_solutions


def test_simple_range_finds_two_solutions():
    def search():
        x = free_int("x", 1, 2)
        return x

    sols = get_path_solutions(search, max_solutions=10)
    xs = sorted({s.labels["x"] for s in sols})
    assert xs == [1, 2]


def test_branching_with_bool_choice():
    def search():
        x = free_int("x", 0, 5)
        if x > 2:  # triggers bool_choice
            assume(x < 4)
            return ("hi", x)
        else:
            assume(x >= 0)
            return ("lo", x)

    sols = get_path_solutions(search, max_solutions=20)
    tags = {s.return_value[0] for s in sols}
    assert tags == {"hi", "lo"}


def test_assume_prunes():
    def search():
        x = free_int("x", 0, 10)
        assume(x == 7)
        return x

    sols = get_solutions(search, max_solutions=5)
    assert len(sols) == 1
    assert sols[0].labels["x"] == 7
