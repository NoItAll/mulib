"""mulib_python — symbolic execution library backed by Z3.

This is a comprehensive Python port of the Java mulib library.
It provides symbolic execution capabilities for exploring all paths
through a program and generating test cases.

Example
-------
>>> from mulib_python import free_int, assume, get_solutions
>>> def my_func():
...     x = free_int("x")
...     if x > 5:
...         return x * 2
...     return x
>>> solutions = get_solutions(my_func)
"""

# Exceptions
from mulib_python.exceptions import (
    MulibException,
    MulibRuntimeException,
    MulibIllegalStateException,
    MisconfigurationException,
    NotYetImplementedException,
    ExceededBudgetException,
    Fail,
    Backtrack,
    ChoicePointExceededBudget,
    IllegalTreeModificationException,
    IllegalTreeAccessException,
)

# Symbolic types from substitutions
from mulib_python.substitutions.primitives.sint import (
    Sint, ConcSint, SymSint, SymSintLeaf,
    Sbool, ConcSbool, SymSbool, SymSboolLeaf,
    Sbyte, ConcSbyte, SymSbyte, SymSbyteLeaf,
    Schar, ConcSchar, SymSchar, SymScharLeaf,
    Sshort, ConcSshort, SymSshort, SymSshortLeaf,
)
from mulib_python.substitutions.primitives.slong import (
    Slong, ConcSlong, SymSlong, SymSlongLeaf,
)
from mulib_python.substitutions.primitives.sdouble import (
    Sdouble, ConcSdouble, SymSdouble, SymSdoubleLeaf,
)
from mulib_python.substitutions.primitives.sfloat import (
    Sfloat, ConcSfloat, SymSfloat, SymSfloatLeaf,
)

# Constraints and expressions
from mulib_python.constraints import (
    Constraint, And, Or, Not, Xor, Implication, Equivalence,
    Lt, Lte, Eq, In, BoolIte, TRUE, FALSE,
)
from mulib_python.expressions import (
    Expression, Sum, Sub, Mul, Div, Mod, Neg,
    BitwiseAnd, BitwiseOr, BitwiseXor,
    ShiftLeft, ShiftRight, LogicalShiftRight,
    ExpressionIte,
)

# Solution types
from mulib_python.solution import Labels, Solution, PathSolution

# Configuration
from mulib_python.config import (
    MulibConfig, MulibConfigBuilder,
    SolverType, ArraySolverType, ExecutionMode, LabelOption,
)

# High-level API
from mulib_python.api import (
    free_int, free_long, free_bool, free_double, free_float,
    free_byte, free_char, free_short,
    assume, fail, backtrack, remember,
    get_solutions, get_path_solutions,
    Mulib, get_mulib,
)

# Execution engine
from mulib_python.executor import (
    SymbolicExecution,
    MulibExecutor,
    run_mulib,
    run_mulib_iter,
    CalculationFactory,
    SymbolicCalculationFactory,
    ConcolicCalculationFactory,
)

# Search infrastructure
from mulib_python.search import (
    SearchStrategy,
    SearchTree,
    TreeNode, Choice, ChoiceOption, Fail as FailNode, ExceededBudget as ExceededBudgetNode,
    Budget, NullBudget, TimeBudget, CountingBudget, PathBudget, DepthBudget,
    BudgetManager,
    ChoicePointFactory, SymbolicChoicePointFactory,
)

# Solver
from mulib_python.solver_manager import SolverManager
from mulib_python.z3_solver_manager import Z3IncrementalSolverManager, Z3GlobalLearningSolverManager

# Test case generation
from mulib_python.tcg import (
    TestCase, TestCaseGenerator, PythonTestRenderer, generate_tests,
    TestSetReducer, UniqueInputReducer, UniqueOutputReducer,
    TestSetSorter, DepthSorter, RandomSorter,
)

__version__ = "0.1.0"

__all__ = [
    # Exceptions
    "MulibException", "MulibRuntimeException", "MulibIllegalStateException",
    "MisconfigurationException", "NotYetImplementedException",
    "ExceededBudgetException", "Fail", "Backtrack",
    "ChoicePointExceededBudget", "IllegalTreeModificationException",
    "IllegalTreeAccessException",
    
    # Symbolic types
    "Sint", "ConcSint", "SymSint", "SymSintLeaf",
    "Sbool", "ConcSbool", "SymSbool", "SymSboolLeaf",
    "Sbyte", "ConcSbyte", "SymSbyte", "SymSbyteLeaf",
    "Schar", "ConcSchar", "SymSchar", "SymScharLeaf",
    "Sshort", "ConcSshort", "SymSshort", "SymSshortLeaf",
    "Slong", "ConcSlong", "SymSlong", "SymSlongLeaf",
    "Sdouble", "ConcSdouble", "SymSdouble", "SymSdoubleLeaf",
    "Sfloat", "ConcSfloat", "SymSfloat", "SymSfloatLeaf",
    
    # Constraints
    "Constraint", "And", "Or", "Not", "Xor", "Implication", "Equivalence",
    "Lt", "Lte", "Eq", "In", "BoolIte", "TRUE", "FALSE",
    
    # Expressions
    "Expression", "Sum", "Sub", "Mul", "Div", "Mod", "Neg",
    "BitwiseAnd", "BitwiseOr", "BitwiseXor",
    "ShiftLeft", "ShiftRight", "LogicalShiftRight", "ExpressionIte",
    
    # Solutions
    "Labels", "Solution", "PathSolution",
    
    # Configuration
    "MulibConfig", "MulibConfigBuilder",
    "SolverType", "ArraySolverType", "ExecutionMode", "LabelOption",
    
    # API
    "free_int", "free_long", "free_bool", "free_double", "free_float",
    "free_byte", "free_char", "free_short",
    "assume", "fail", "backtrack", "remember",
    "get_solutions", "get_path_solutions",
    "Mulib", "get_mulib",
    
    # Execution
    "SymbolicExecution", "MulibExecutor", "run_mulib", "run_mulib_iter",
    "CalculationFactory", "SymbolicCalculationFactory", "ConcolicCalculationFactory",
    
    # Search
    "SearchStrategy", "SearchTree",
    "TreeNode", "Choice", "ChoiceOption", "FailNode", "ExceededBudgetNode",
    "Budget", "NullBudget", "TimeBudget", "CountingBudget", "PathBudget", "DepthBudget",
    "BudgetManager",
    "ChoicePointFactory", "SymbolicChoicePointFactory",
    
    # Solver
    "SolverManager", "Z3IncrementalSolverManager", "Z3GlobalLearningSolverManager",
    
    # TCG
    "TestCase", "TestCaseGenerator", "PythonTestRenderer", "generate_tests",
    "TestSetReducer", "UniqueInputReducer", "UniqueOutputReducer",
    "TestSetSorter", "DepthSorter", "RandomSorter",
]
