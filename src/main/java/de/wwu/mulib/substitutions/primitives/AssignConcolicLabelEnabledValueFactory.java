package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.search.executors.SymbolicExecution;

public interface AssignConcolicLabelEnabledValueFactory extends ValueFactory {

    Sbool.SymSbool assignLabel(SymbolicExecution se, Sbool.SymSbool sym);

    Sshort.SymSshort assignLabel(SymbolicExecution se, Sshort.SymSshort sym);

    Sbyte.SymSbyte assignLabel(SymbolicExecution se, Sbyte.SymSbyte sym);

    Sint.SymSint assignLabel(SymbolicExecution se, Sint.SymSint sym);

    Slong.SymSlong assignLabel(SymbolicExecution se, Slong.SymSlong sym);

    Sdouble.SymSdouble assignLabel(SymbolicExecution se, Sdouble.SymSdouble sym);

    Sfloat.SymSfloat assignLabel(SymbolicExecution se, Sfloat.SymSfloat sym);

    default Snumber assignLabel(SymbolicExecution se, SymNumericExpressionSprimitive n) {
        if (n instanceof Sshort.SymSshort) {
            return assignLabel(se, (Sshort.SymSshort) n);
        } else if (n instanceof Sbyte.SymSbyte) {
            return assignLabel(se, (Sbyte.SymSbyte) n);
        } else if (n instanceof Sint.SymSint) {
            return assignLabel(se, (Sint.SymSint) n);
        } else if (n instanceof Slong.SymSlong) {
            return assignLabel(se, (Slong.SymSlong) n);
        } else if (n instanceof Sdouble.SymSdouble) {
            return assignLabel(se, (Sdouble.SymSdouble) n);
        } else if (n instanceof Sfloat.SymSfloat) {
            return assignLabel(se, (Sfloat.SymSfloat) n);
        } else {
            throw new NotYetImplementedException(n.getClass().toString());
        }
    }
}
