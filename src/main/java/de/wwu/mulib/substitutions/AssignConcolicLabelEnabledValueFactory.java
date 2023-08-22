package de.wwu.mulib.substitutions;

import de.wwu.mulib.throwables.NotYetImplementedException;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.substitutions.primitives.*;

/**
 * Marker interface for those value factories that are concolic
 */
public interface AssignConcolicLabelEnabledValueFactory extends ValueFactory {

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param sym The symbolic value to label
     * @return A symbolic value that wraps a {@link de.wwu.mulib.constraints.ConcolicConstraintContainer} with a label
     * and sym
     */
    Sbool.SymSbool assignLabel(SymbolicExecution se, Sbool.SymSbool sym);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param sym The symbolic value to label
     * @return A symbolic value that wraps a {@link de.wwu.mulib.expressions.ConcolicNumericContainer} with a label
     * and sym
     */
    Sshort.SymSshort assignLabel(SymbolicExecution se, Sshort.SymSshort sym);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param sym The symbolic value to label
     * @return A symbolic value that wraps a {@link de.wwu.mulib.expressions.ConcolicNumericContainer} with a label
     * and sym
     */
    Sbyte.SymSbyte assignLabel(SymbolicExecution se, Sbyte.SymSbyte sym);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param sym The symbolic value to label
     * @return A symbolic value that wraps a {@link de.wwu.mulib.expressions.ConcolicNumericContainer} with a label
     * and sym
     */
    Sint.SymSint assignLabel(SymbolicExecution se, Sint.SymSint sym);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param sym The symbolic value to label
     * @return A symbolic value that wraps a {@link de.wwu.mulib.expressions.ConcolicNumericContainer} with a label
     * and sym
     */
    Slong.SymSlong assignLabel(SymbolicExecution se, Slong.SymSlong sym);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param sym The symbolic value to label
     * @return A symbolic value that wraps a {@link de.wwu.mulib.expressions.ConcolicNumericContainer} with a label
     * and sym
     */
    Sdouble.SymSdouble assignLabel(SymbolicExecution se, Sdouble.SymSdouble sym);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param sym The symbolic value to label
     * @return A symbolic value that wraps a {@link de.wwu.mulib.expressions.ConcolicNumericContainer} with a label
     * and sym
     */
    Sfloat.SymSfloat assignLabel(SymbolicExecution se, Sfloat.SymSfloat sym);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param sym The symbolic value to label
     * @return A symbolic value that wraps a {@link de.wwu.mulib.expressions.ConcolicNumericContainer} with a label
     * and sym
     */
    Schar.SymSchar assignLabel(SymbolicExecution se, Schar.SymSchar sym);

    /**
     * Simplifies accessing the other methods by providing a case disctinction
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param n The symbolic value to label
     * @return A symbolic value that wraps a {@link de.wwu.mulib.expressions.ConcolicNumericContainer} with a label
     * and sym
     */
    default Snumber assignLabel(SymbolicExecution se, SymSnumber n) {
        if (n instanceof Sshort.SymSshort) {
            return assignLabel(se, (Sshort.SymSshort) n);
        } else if (n instanceof Sbyte.SymSbyte) {
            return assignLabel(se, (Sbyte.SymSbyte) n);
        } else if (n instanceof Schar.SymSchar) {
            return assignLabel(se, (Schar.SymSchar) n);
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
