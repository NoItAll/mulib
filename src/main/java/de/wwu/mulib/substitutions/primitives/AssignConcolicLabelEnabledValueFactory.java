package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.search.executors.SymbolicExecution;

public interface AssignConcolicLabelEnabledValueFactory extends ValueFactory {

    Sbool.SymSbool assignLabel(SymbolicExecution se, Sbool.SymSbool sym);

    Sshort.SymSshort assignLabel(SymbolicExecution se, Sshort.SymSshort sym);

    Sbyte.SymSbyte assignLabel(SymbolicExecution se, Sbyte.SymSbyte sym);

    Sint.SymSint assignLabel(SymbolicExecution se, Sint.SymSint sym);

    Slong.SymSlong assignLabel(SymbolicExecution se, Slong.SymSlong sym);

    Sdouble.SymSdouble assignLabel(SymbolicExecution se, Sdouble.SymSdouble sym);

    Sfloat.SymSfloat assignLabel(SymbolicExecution se, Sfloat.SymSfloat sym);
}
