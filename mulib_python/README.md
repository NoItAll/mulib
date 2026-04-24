# mulib_python

A complete Python 3 port of the [mulib](../README.md) symbolic execution
library, built on top of [Z3](https://github.com/Z3Prover/z3).

Mulib enables **constraint-based symbolic execution** for Python programs,
allowing you to explore all possible execution paths and find inputs that
satisfy complex conditions.

## Table of Contents

- [Installation](#installation)
- [Quick Start](#quick-start)
- [Core Concepts](#core-concepts)
- [API Reference](#api-reference)
- [Configuration](#configuration)
- [Search Strategies](#search-strategies)
- [Budget Management](#budget-management)
- [Test Case Generation](#test-case-generation)
- [Examples](#examples)
- [Architecture](#architecture)
- [Testing](#testing)
- [License](#license)

## Installation

```bash
pip install z3-solver
```

For development:
```bash
pip install z3-solver pytest
```

## Quick Start

### Basic Example

```python
from mulib_python import free_int, assume, get_solutions

def search():
    x = free_int("x", 0, 10)
    y = free_int("y", 0, 10)
    assume(x + y == 13)
    assume(x * y == 42)
    return x, y

solutions = get_solutions(search, max_solutions=1)
print(solutions[0].labels)
# {'x': 6, 'y': 7}
```

### SEND + MORE = MONEY Cryptarithm

```python
from mulib_python import free_int, assume, get_solutions

def send_more_money():
    # Each letter represents a unique digit
    s = free_int("S", 1, 9)  # S cannot be 0 (leading digit)
    e = free_int("E", 0, 9)
    n = free_int("N", 0, 9)
    d = free_int("D", 0, 9)
    m = free_int("M", 1, 9)  # M cannot be 0 (leading digit)
    o = free_int("O", 0, 9)
    r = free_int("R", 0, 9)
    y = free_int("Y", 0, 9)

    # All digits must be unique
    digits = [s, e, n, d, m, o, r, y]
    for i in range(len(digits)):
        for j in range(i + 1, len(digits)):
            assume(digits[i] != digits[j])

    # SEND + MORE = MONEY
    send  = s * 1000 + e * 100 + n * 10 + d
    more  = m * 1000 + o * 100 + r * 10 + e
    money = m * 10000 + o * 1000 + n * 100 + e * 10 + y
    assume(send + more == money)

    return {"S": s, "E": e, "N": n, "D": d, "M": m, "O": o, "R": r, "Y": y}

solution = get_solutions(send_more_money, max_solutions=1)[0]
print(solution.labels)
# {'S': 9, 'E': 5, 'N': 6, 'D': 7, 'M': 1, 'O': 0, 'R': 8, 'Y': 2}
```

## Core Concepts

### Symbolic Primitives

Mulib provides symbolic versions of Python primitive types:

| Type | Concrete | Symbolic Leaf | Description |
|------|----------|---------------|-------------|
| `Sint` | `ConcSint` | `SymSintLeaf` | 32-bit signed integer |
| `Slong` | `ConcSlong` | `SymSlongLeaf` | 64-bit signed integer |
| `Sbool` | `ConcSbool` | `SymSboolLeaf` | Boolean |
| `Sdouble` | `ConcSdouble` | `SymSdoubleLeaf` | 64-bit floating point |
| `Sfloat` | `ConcSfloat` | `SymSfloatLeaf` | 32-bit floating point |
| `Sbyte` | `ConcSbyte` | `SymSbyteLeaf` | 8-bit signed integer |
| `Sshort` | `ConcSshort` | `SymSshortLeaf` | 16-bit signed integer |
| `Schar` | `ConcSchar` | `SymScharLeaf` | Unicode character |

All symbolic types support Python operators:

```python
from mulib_python.substitutions.primitives import SymSintLeaf, ConcSint

x = SymSintLeaf("x")
y = SymSintLeaf("y")

# Arithmetic
result = x + y * ConcSint(2)

# Comparison (returns Sbool)
cond = x < y

# Bitwise
masked = x & ConcSint(0xFF)
```

### Constraints and Expressions

Symbolic operations build expression trees that are translated to Z3:

```python
from mulib_python.constraints import Lt, Eq, And, Or, Not
from mulib_python.expressions import Sum, Mul

# Build constraint: x < 10 AND y > 0
constraint = And(
    Lt(x, ConcSint(10)),
    Lt(ConcSint(0), y)
)
```

### Choice Points

When a symbolic boolean is evaluated as a Python `bool` (e.g., in an `if`
statement), mulib creates a **choice point** and explores both branches:

```python
x = free_int("x", 0, 100)

if x > 50:  # Choice point created here
    # Branch 1: solver adds constraint x > 50
    return "big"
else:
    # Branch 2: solver adds constraint x <= 50
    return "small"
```

### Path Solutions

Each explored path yields a `PathSolution` containing:
- `labels`: Mapping of symbolic variable names to concrete values
- `return_value`: The return value of the search function
- `path_constraints`: List of constraints along the path

## API Reference

### High-Level Functions

```python
from mulib_python import (
    # Create symbolic variables
    free_int,      # Create symbolic int in range [lo, hi]
    free_long,     # Create symbolic long
    free_bool,     # Create symbolic boolean
    free_double,   # Create symbolic double
    free_float,    # Create symbolic float
    free_byte,     # Create symbolic byte
    free_short,    # Create symbolic short
    free_char,     # Create symbolic char

    # Constraints
    assume,        # Add a constraint (path dies if unsatisfiable)
    fail,          # Kill current path unconditionally
    backtrack,     # Backtrack to try another path

    # State
    remember,      # Store value in solution labels

    # Execution
    get_solutions,       # Run search, return list of Solutions
    get_path_solutions,  # Run search, return list of PathSolutions
)
```

### Function Signatures

```python
def free_int(name: str, lo: int = MIN_INT, hi: int = MAX_INT) -> Sint:
    """Create a named symbolic integer constrained to [lo, hi]."""

def assume(condition: Sbool | bool) -> None:
    """Assert that condition must be true. Kills path if unsatisfiable."""

def remember(name: str, value: Any) -> None:
    """Store a value in the solution labels under the given name."""

def get_solutions(
    search_func: Callable,
    max_solutions: int = 1,
    config: MulibConfig | None = None
) -> List[Solution]:
    """Execute search_func symbolically and return solutions."""

def get_path_solutions(
    search_func: Callable,
    max_solutions: int = 1,
    config: MulibConfig | None = None
) -> List[PathSolution]:
    """Execute search_func and return detailed path solutions."""
```

### Mulib Class

For more control, use the `Mulib` class directly:

```python
from mulib_python import Mulib
from mulib_python.config import MulibConfig

config = (MulibConfig.builder()
    .set_search_strategy("BFS")
    .set_time_limit(60.0)
    .set_max_paths(1000)
    .build())

mulib = Mulib(search_func, config)
mulib.run()

for sol in mulib.solutions:
    print(sol.labels)
```

## Configuration

The `MulibConfig` class provides comprehensive configuration:

```python
from mulib_python.config import MulibConfig, MulibConfigBuilder

config = (MulibConfig.builder()
    # Search strategy
    .set_search_strategy("DFS")        # DFS, BFS, IDDFS, RANDOM

    # Budgets
    .set_time_limit(60.0)              # Max seconds
    .set_max_paths(1000)               # Max paths to explore
    .set_max_depth(100)                # Max path depth

    # Solver settings
    .set_solver_timeout(30.0)          # Z3 timeout per query
    .set_array_solver("HISTORY")       # HISTORY or DIRECT

    # Execution mode
    .set_execution_mode("SYMBOLIC")    # SYMBOLIC or CONCOLIC

    # Labeling
    .set_label_result(True)            # Label return values
    .set_label_everything(False)       # Label all variables

    .build())
```

### Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `search_strategy` | str | "DFS" | Search strategy (DFS, BFS, IDDFS, RANDOM) |
| `time_limit` | float | None | Maximum execution time in seconds |
| `max_paths` | int | None | Maximum number of paths to explore |
| `max_depth` | int | None | Maximum path depth |
| `solver_timeout` | float | 30.0 | Z3 solver timeout per query |
| `array_solver` | str | "HISTORY" | Array theory (HISTORY, DIRECT) |
| `execution_mode` | str | "SYMBOLIC" | Mode (SYMBOLIC, CONCOLIC) |
| `label_result` | bool | True | Include return value in labels |
| `label_everything` | bool | False | Label all symbolic variables |

## Search Strategies

Mulib supports multiple search strategies:

### DFS (Depth-First Search)
Explores paths deeply before backtracking. Good for finding any solution quickly.

```python
config = MulibConfig.builder().set_search_strategy("DFS").build()
```

### BFS (Breadth-First Search)
Explores paths level by level. Finds shortest paths first.

```python
config = MulibConfig.builder().set_search_strategy("BFS").build()
```

### IDDFS (Iterative Deepening DFS)
Combines DFS space efficiency with BFS completeness.

```python
config = MulibConfig.builder().set_search_strategy("IDDFS").build()
```

### RANDOM
Explores paths in random order. Good for sampling diverse solutions.

```python
config = MulibConfig.builder().set_search_strategy("RANDOM").build()
```

## Budget Management

Control resource consumption with budgets:

```python
from mulib_python.search.budget import (
    TimeBudget,
    PathBudget,
    DepthBudget,
    CompositeBudget,
)

# Time budget: 60 seconds max
time_budget = TimeBudget(60.0)

# Path budget: 1000 paths max
path_budget = PathBudget(1000)

# Depth budget: 50 levels max
depth_budget = DepthBudget(50)

# Combine multiple budgets
combined = CompositeBudget([time_budget, path_budget, depth_budget])
```

## Test Case Generation

Generate test cases from symbolic execution:

```python
from mulib_python.tcg import generate_tests, TestCaseGenerator, PythonTestRenderer

# Get solutions
solutions = get_solutions(my_func, max_solutions=10)

# Generate test cases
generator = TestCaseGenerator()
test_cases = generator.from_solutions(solutions, "my_func")

# Render as pytest tests
renderer = PythonTestRenderer(style="pytest")
code = renderer.render(test_cases, module_name="test_my_func")
print(code)
```

### Test Case Reducers

Reduce test case count while maintaining coverage:

```python
from mulib_python.tcg.testsetreducer import (
    UniqueInputReducer,      # Keep tests with unique inputs
    UniqueOutputReducer,     # Keep tests with unique outputs
    BoundaryValueReducer,    # Keep boundary value tests
    CompositeReducer,        # Combine reducers
)

reducer = UniqueInputReducer()
reduced_tests = reducer.reduce(test_cases)
```

### Test Case Sorters

Order test cases by priority:

```python
from mulib_python.tcg.testsetsorter import (
    DepthSorter,       # Sort by path depth
    InputSizeSorter,   # Sort by input size
    RandomSorter,      # Random order
)

sorter = DepthSorter()
sorted_tests = sorter.sort(test_cases)
```

## Examples

### N-Queens Problem

```python
from mulib_python import free_int, assume, get_solutions

def nqueens(n: int):
    queens = [free_int(f"q{i}", 0, n - 1) for i in range(n)]

    # All queens on different columns
    for i in range(n):
        for j in range(i + 1, n):
            assume(queens[i] != queens[j])

    # No diagonal attacks
    for i in range(n):
        for j in range(i + 1, n):
            diff = j - i
            assume(queens[i] - queens[j] != diff)
            assume(queens[j] - queens[i] != diff)

    return queens

solutions = get_solutions(lambda: nqueens(8), max_solutions=1)
print(solutions[0].labels)
```

### Graph Coloring

```python
from mulib_python import free_int, assume, get_solutions

def graph_coloring(edges, num_colors):
    nodes = set()
    for a, b in edges:
        nodes.add(a)
        nodes.add(b)

    colors = {n: free_int(f"color_{n}", 0, num_colors - 1) for n in nodes}

    for a, b in edges:
        assume(colors[a] != colors[b])

    return colors

edges = [(0, 1), (1, 2), (2, 0), (2, 3)]
solutions = get_solutions(lambda: graph_coloring(edges, 3), max_solutions=1)
print(solutions[0].labels)
```

## Architecture

```
mulib_python/
├── api.py                 # High-level API (free_int, assume, etc.)
├── config.py              # Configuration classes
├── solution.py            # Labels, Solution, PathSolution
├── constraints.py         # Constraint AST (Lt, Eq, And, Or, etc.)
├── expressions.py         # Expression AST (Sum, Mul, etc.)
├── substitutions/         # Symbolic primitive types
│   ├── primitives/
│   │   ├── sint.py        # Sint, Sbool, Sbyte, Schar, Sshort
│   │   ├── slong.py       # Slong
│   │   ├── sdouble.py     # Sdouble
│   │   └── sfloat.py      # Sfloat
│   └── _se_context.py     # Thread-local execution context
├── executor/              # Execution engine
│   ├── symbolic_execution.py
│   ├── mulib_executor.py
│   └── calculation_factory.py
├── search/                # Search infrastructure
│   ├── strategy.py        # SearchStrategy enum
│   ├── trees/             # Search tree management
│   ├── budget/            # Budget classes
│   └── choice_points/     # Choice point factories
├── z3_adapter.py          # AST to Z3 translation
├── z3_solver_manager.py   # Z3 solver management
├── tcg/                   # Test case generation
│   ├── tcg.py
│   ├── testsetreducer/
│   └── testsetsorter/
└── examples/              # Example programs
```

## Testing

Run the test suite:

```bash
# All tests
pytest mulib_python/tests/ -v

# Specific test file
pytest mulib_python/tests/test_primitives.py -v

# With coverage
pytest mulib_python/tests/ --cov=mulib_python --cov-report=html
```

Test breakdown:
- `test_primitives.py`: Symbolic primitive types (31 tests)
- `test_solver.py`: Z3 solver integration (22 tests)
- `test_search.py`: Search infrastructure (36 tests)
- `test_executor.py`: Execution engine (18 tests)
- `test_tcg.py`: Test case generation (20 tests)
- `test_constraints.py`: Constraints and expressions (25 tests)
- `test_arrays.py`: Array representations (22 tests)
- `test_examples.py`: Example programs (3 tests)

## License

This project is licensed under the same terms as the parent mulib project.
See [LICENSE.txt](../LICENSE.txt) for details.

## Acknowledgments

This Python port is based on the original Java [mulib](https://github.com/NoItAll/mulib)
symbolic execution library.

---

For more information, see the [main mulib documentation](../README.md).
