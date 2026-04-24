"""Symbolic substitutions package for mulib.

This package provides symbolic primitive types that serve as drop-in replacements
for Python built-in types during symbolic execution.
"""

from __future__ import annotations

from mulib_python.substitutions.markers import Substituted, Sym, Conc
from mulib_python.substitutions.primitives.sprimitive import (
    Sprimitive, SymSprimitive, SymSprimitiveLeaf
)
from mulib_python.substitutions.primitives.snumber import (
    Snumber, ConcSnumber, SymSnumber, ConcSprimitive, AbstractSnumber, Sfpnumber
)
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
from mulib_python.substitutions._se_context import (
    get_se, set_se, clear_se, _get_se_or_raise
)

__all__ = [
    # Markers
    'Substituted', 'Sym', 'Conc',
    # Sprimitive hierarchy
    'Sprimitive', 'SymSprimitive', 'SymSprimitiveLeaf',
    # Snumber hierarchy
    'Snumber', 'ConcSnumber', 'SymSnumber', 'ConcSprimitive', 'AbstractSnumber', 'Sfpnumber',
    # Sint family
    'Sint', 'ConcSint', 'SymSint', 'SymSintLeaf',
    'Sbool', 'ConcSbool', 'SymSbool', 'SymSboolLeaf',
    'Sbyte', 'ConcSbyte', 'SymSbyte', 'SymSbyteLeaf',
    'Schar', 'ConcSchar', 'SymSchar', 'SymScharLeaf',
    'Sshort', 'ConcSshort', 'SymSshort', 'SymSshortLeaf',
    # Slong family
    'Slong', 'ConcSlong', 'SymSlong', 'SymSlongLeaf',
    # Sdouble family
    'Sdouble', 'ConcSdouble', 'SymSdouble', 'SymSdoubleLeaf',
    # Sfloat family
    'Sfloat', 'ConcSfloat', 'SymSfloat', 'SymSfloatLeaf',
    # SE context
    'get_se', 'set_se', 'clear_se',
]
