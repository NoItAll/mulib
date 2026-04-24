"""Exception hierarchy for mulib symbolic execution engine."""


class MulibException(Exception):
    """Base exception for all mulib exceptions."""


class MulibRuntimeException(MulibException):
    """Raised for runtime errors during symbolic execution."""


class MulibRuntimeError(MulibException):
    """Raised for unrecoverable errors during symbolic execution."""


class MulibIllegalStateException(MulibRuntimeException):
    """Raised when the engine enters an illegal state."""


class MisconfigurationException(MulibRuntimeException):
    """Raised when the engine is misconfigured."""


class NotYetImplementedException(MulibRuntimeException):
    """Raised when a code path has not yet been implemented."""


class ExceededBudgetException(MulibRuntimeException):
    """Raised when a configured execution budget (e.g. path depth, time) is exceeded."""


class Fail(MulibException):
    """Thrown explicitly inside a search region to prune the current execution path.

    Equivalent to calling ``Mulib.fail()`` in the Java library.
    """
