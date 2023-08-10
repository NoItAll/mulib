package de.wwu.mulib.constraints;

import de.wwu.mulib.substitutions.primitives.Sbool;

/**
 * Represents the logical equivalence between two constraints, i.e., lhs <-> rhs
 */
public class Equivalence extends AbstractTwoSidedConstraint {

    protected Equivalence(Constraint lhs, Constraint rhs) {
        super(lhs, rhs);
    }

    /**
     * Creates a new constraint, possibly simplifying the overall constraint
     * @param lhs The left-hand side
     * @param rhs The right-hand side
     * @return A constraint that is either simplified or lhs <-> rhs
     */
    public static Constraint newInstance(Constraint lhs, Constraint rhs) {
        if (lhs instanceof Sbool.ConcSbool) {
            return ((Sbool.ConcSbool) lhs).isTrue() ? rhs : Not.newInstance(rhs);
        } else if (rhs instanceof Sbool.ConcSbool) {
            return ((Sbool.ConcSbool) rhs).isTrue() ? lhs : Not.newInstance(lhs);
        }
        return new Equivalence(lhs, rhs);
    }
}
