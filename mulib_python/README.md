# mulib_python

A minimal Python rewrite of the [mulib](../README.md) symbolic-execution
library, built on top of [Z3](https://github.com/Z3Prover/z3).

## Installation

```bash
pip install z3-solver
```

## Quick example

```python
from mulib_python import free_int, assume, get_solutions

def search():
    x = free_int("x", 0, 10)
    y = free_int("y", 0, 10)
    assume(x + y == 13)
    assume(x * y == 42)
    return x, y

print(get_solutions(search, max_solutions=1)[0].labels)
# {'x': 6, 'y': 7}
```

## Features

- **Symbolic primitives** `Sint`, `Sbool`, `Sdouble` with Python operator overloading
- **Symbolic arrays** (`Sarray`) using Z3 array theory
- **DFS search engine** with choice points triggered by symbolic boolean evaluation
- **High-level API**: `free_int`, `free_bool`, `free_float`, `assume`, `fail`,
  `get_solutions`, `get_path_solutions`

## Examples

```bash
python -m mulib_python.examples.send_more_money
python -m mulib_python.examples.nqueens
python -m mulib_python.examples.array_example
```

## Tests

```bash
pytest mulib_python/tests/
```
