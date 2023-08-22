package de.wwu.mulib.expressions;

import de.wwu.mulib.substitutions.primitives.ConcSnumber;
import de.wwu.mulib.substitutions.primitives.Snumber;
import de.wwu.mulib.substitutions.primitives.SymSnumber;

/**
 * A container mapping a symbolic value to its concrete value for concolic execution.
 */
public final class ConcolicNumericContainer implements NumericExpression {

    private final SymSnumber sym;
    private ConcSnumber conc;

    /**
     * Constructs a new container
     * @param sym The symbolic value
     * @param conc The concrete value that is a valid label of sym
     */
    public ConcolicNumericContainer(SymSnumber sym, ConcSnumber conc) {
        this.sym = sym;
        this.conc = conc;
    }

    @Override
    public boolean isFp() {
        return sym.isFp();
    }

    /**
     * @return The symbolic value
     */
    public SymSnumber getSym() {
        return sym;
    }

    /**
     * @return The label of {@link ConcolicNumericContainer#getSym()}.
     */
    public ConcSnumber getConc() {
        return conc;
    }

    /**
     * Checks whether ne contains a ConcolicNumericContainer. If this is the case, returns the represented symbolic value.
     * @param ne The Snumber potentially carrying a ConcolicNumericContainer.
     * @return The represented symbolic expression, if any.
     */
    public static Snumber tryGetSymFromConcolic(Snumber ne) {
        if (ne instanceof SymSnumber) {
            SymSnumber sym = (SymSnumber) ne;
            if (sym.getRepresentedExpression() instanceof ConcolicNumericContainer) {
                return ((ConcolicNumericContainer) sym.getRepresentedExpression()).getSym();
            }
        }
        return ne;
    }

    /**
     * Checks whether ne contains a ConcolicNumericContainer. If this is the case, returns the labeled value.
     * @param ne The NumericExpression potentially carrying a concolic constraint container
     * @return A ConcSnumber
     */
    public static ConcSnumber getConcNumericFromConcolic(NumericExpression ne) {
        return ne instanceof SymSnumber ?
                ((ConcolicNumericContainer) ((SymSnumber) ne).getRepresentedExpression()).getConc()
                :
                (ConcSnumber) ne;
    }

    @Override
    public String toString() {
        return "ConcolicNumericContainer{conc=" + conc + ",sym=" + sym + "}";
    }

    @Override
    public int hashCode() {
        return sym.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ConcolicNumericContainer && ((ConcolicNumericContainer) o).getSym() == sym;
    }
}
