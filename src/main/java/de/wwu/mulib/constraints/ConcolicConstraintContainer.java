package de.wwu.mulib.constraints;

import de.wwu.mulib.substitutions.primitives.Sbool;

public class ConcolicConstraintContainer implements Constraint {
    private final Constraint sym;
    private Sbool.ConcSbool conc;

    public ConcolicConstraintContainer(Constraint sym, Sbool.ConcSbool conc) {
        assert !(sym instanceof ConcolicConstraintContainer);
        this.sym = sym instanceof Sbool.SymSbool ? ((Sbool.SymSbool) sym).getRepresentedConstraint() : sym;
        this.conc = conc;
    }

    public Constraint getSym() {
        return sym;
    }

    public Sbool.ConcSbool getConc() {
        return conc;
    }

    public void labelAnew(Sbool.ConcSbool conc) {
        this.conc = conc;
    }
}
