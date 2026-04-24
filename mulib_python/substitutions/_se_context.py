"""Thread-local registry for the active SymbolicExecution instance."""

from __future__ import annotations

import threading
from typing import TYPE_CHECKING, Optional

if TYPE_CHECKING:
    from mulib_python.executor.symbolic_execution import SymbolicExecution

_local = threading.local()


def get_se() -> Optional["SymbolicExecution"]:
    """Return the SymbolicExecution active in the current thread, or None."""
    return getattr(_local, "se", None)


# Alias for backward compatibility
_get_se = get_se


def set_se(se: "SymbolicExecution") -> None:
    """Bind se as the active SymbolicExecution for the current thread."""
    _local.se = se


# Alias for internal use
_set_se = set_se


def clear_se() -> None:
    """Remove the active SymbolicExecution for the current thread."""
    try:
        del _local.se
    except AttributeError:
        pass


# Alias for internal use
_clear_se = clear_se


def _get_se_or_raise() -> "SymbolicExecution":
    """Return the active SE or raise MulibIllegalStateException."""
    se = get_se()
    if se is None:
        from mulib_python.exceptions import MulibIllegalStateException
        raise MulibIllegalStateException(
            "No active SymbolicExecution found in this thread. "
            "Symbolic operations must occur inside a search region."
        )
    return se
