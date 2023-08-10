package de.wwu.mulib.constraints;

import de.wwu.mulib.substitutions.primitives.Sbool;

/**
 * Represents a logical implication, i.e., a -> b
 */
public class Implication extends AbstractTwoSidedConstraint {

    protected Implication(Constraint lhs, Constraint rhs) {
        super(lhs, rhs);
    }

    /**
     * Creates a new constraint, possibly simplifying the overall constraint
     * @param lhs The antecedent
     * @param rhs The consequent
     * @return A constraint that is either simplified or lhs -> rhs
     */
    public static Constraint newInstance(Constraint lhs, Constraint rhs) {
        if (lhs instanceof Sbool.ConcSbool) {
            return ((Sbool.ConcSbool) lhs).isTrue() ? rhs : Sbool.ConcSbool.TRUE;
        } else if (rhs instanceof Sbool.ConcSbool) {
            return ((Sbool.ConcSbool) rhs).isTrue() ? Sbool.ConcSbool.TRUE : Not.newInstance(lhs);
        }
        return new Implication(lhs, rhs);
    }

    @Override
    public String toString() {
        return String.format("(%s -> %s)", lhs.toString(), rhs.toString());
    }

}
