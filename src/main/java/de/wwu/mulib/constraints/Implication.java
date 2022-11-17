package de.wwu.mulib.constraints;

import de.wwu.mulib.substitutions.primitives.Sbool;

public class Implication extends AbstractTwoSidedConstraint {

    protected Implication(Constraint lhs, Constraint rhs) {
        super(lhs, rhs);
    }

    public static Constraint newInstance(Constraint lhs, Constraint rhs) {
        if (lhs instanceof Sbool.ConcSbool) {
            return ((Sbool.ConcSbool) lhs).isTrue() ? rhs : Sbool.ConcSbool.TRUE;
        } else if (rhs instanceof Sbool.ConcSbool) {
            return ((Sbool.ConcSbool) rhs).isTrue() ? Sbool.ConcSbool.TRUE : Not.newInstance(lhs);
        }
        return new Implication(lhs, rhs);
    }

}
