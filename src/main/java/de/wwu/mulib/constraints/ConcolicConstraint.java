package de.wwu.mulib.constraints;

import de.wwu.mulib.substitutions.primitives.Sbool;

public class ConcolicConstraint implements Constraint {
    private final Sbool.SymSbool sym;
    private Sbool.ConcSbool conc;

    public ConcolicConstraint(Sbool.SymSbool sym, Sbool.ConcSbool conc) {
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
}
