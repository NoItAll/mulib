package de.wwu.mulib.constraints;

import de.wwu.mulib.substitutions.primitives.Sbool;

public class ConcolicConstraintContainer implements Constraint {
    private final Sbool.SymSbool sym;
    private Sbool.ConcSbool conc;

    public ConcolicConstraintContainer(Sbool.SymSbool sym, Sbool.ConcSbool conc) {
        this.sym = sym;
        this.conc = conc;
    }

    public Sbool.SymSbool getSym() {
        return sym;
    }

    public Sbool.ConcSbool getConc() {
        return conc;
    }

    public void labelAnew(Sbool.ConcSbool conc) {
        this.conc = conc;
    }

    public static Sbool tryGetSymFromConcolic(Sbool b) {
        if (b instanceof Sbool.SymSbool) {
            Sbool.SymSbool sym = (Sbool.SymSbool) b;
            if (sym.getRepresentedConstraint() instanceof ConcolicConstraintContainer) {
                return ((ConcolicConstraintContainer) sym.getRepresentedConstraint()).getSym();
            }
        }
        return b;
    }

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
}
