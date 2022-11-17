package de.wwu.mulib.constraints;

import de.wwu.mulib.substitutions.primitives.Sbool;

public class Equivalence extends AbstractTwoSidedConstraint {

    protected Equivalence(Constraint lhs, Constraint rhs) {
        super(lhs, rhs);
    }

    public static Constraint newInstance(Constraint lhs, Constraint rhs) {
        if (lhs instanceof Sbool.ConcSbool) {
            return ((Sbool.ConcSbool) lhs).isTrue() ? rhs : Not.newInstance(rhs);
        } else if (rhs instanceof Sbool.ConcSbool) {
            return ((Sbool.ConcSbool) rhs).isTrue() ? lhs : Not.newInstance(lhs);
        }
        return new Equivalence(lhs, rhs);
    }
}
