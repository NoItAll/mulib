package de.wwu.mulib.constraints;

import de.wwu.mulib.substitutions.primitives.Sbool;

/**
 * A container mapping a symbolic value to its concrete value for concolic execution.
 */
public class ConcolicConstraintContainer implements Constraint {
    private final Sbool.SymSbool sym;
    private Sbool.ConcSbool conc;

    /**
     * Constructs a new container
     * @param sym The symbolic value
     * @param conc The concrete value that is a valid label of sym
     */
    public ConcolicConstraintContainer(Sbool.SymSbool sym, Sbool.ConcSbool conc) {
        this.sym = sym;
        this.conc = conc;
    }

    /**
     * @return The symbolic value
     */
    public Sbool.SymSbool getSym() {
        return sym;
    }

    /**
     * @return The label of {@link ConcolicConstraintContainer#getSym()}.
     */
    public Sbool.ConcSbool getConc() {
        return conc;
    }

    /**
     * Checks whether b contains a ConcolicConstraintContainer. If this is the case, returns the represented symbolic value.
     * @param b The Sbool potentially carrying a ConcolicConstraintContainer.
     * @return The represented symbolic expression, if any.
     */
    public static Sbool tryGetSymFromConcolic(Sbool b) {
        if (b instanceof Sbool.SymSbool) {
            Sbool.SymSbool sym = (Sbool.SymSbool) b;
            if (sym.getRepresentedConstraint() instanceof ConcolicConstraintContainer) {
                return ((ConcolicConstraintContainer) sym.getRepresentedConstraint()).getSym();
            }
        }
        return b;
    }

    /**
     * Checks whether c contains a ConcolicConstraintContainer. If this is the case, returns the labeled value.
     * @param c The constraint potentially carrying a concolic constraint container
     * @return A concrete Sbool
     */
    public static Sbool.ConcSbool getConcSboolFromConcolic(Constraint c) {
        return c instanceof Sbool.SymSbool ?
                ((ConcolicConstraintContainer) ((Sbool.SymSbool) c).getRepresentedConstraint()).getConc()
                :
                (Sbool.ConcSbool) c;
    }

    @Override
    public String toString() {
        return "ConcolicConstraintContainer{conc=" + conc + ",sym=" + sym + "}";
    }

    @Override
    public int hashCode() {
        return sym.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ConcolicConstraintContainer && ((ConcolicConstraintContainer) o).getSym() == sym;
    }
}
