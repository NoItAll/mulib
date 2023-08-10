package de.wwu.mulib.constraints;

import de.wwu.mulib.substitutions.primitives.Sbool;

/**
 * Represents a logical XOR
 */
public class Xor extends AbstractTwoSidedConstraint {
    private Xor(Constraint lhs, Constraint rhs) {
        super(lhs, rhs);
    }

    /**
     * Creates a new constraint, possibly simplifying the overall constraint
     * @param lhs The left-hand side
     * @param rhs The right-hand side
     * @return A constraint that is either simplified or XOR(lhs, rhs)
     */
    public static Constraint newInstance(Constraint lhs, Constraint rhs) {
        if (bothConstraintsAreConcrete(lhs, rhs)) {
            boolean lhsIsTrue = ((Sbool.ConcSbool) lhs).isTrue();
            boolean rhsIsTrue = ((Sbool.ConcSbool) rhs).isTrue();
            return Sbool.concSbool((lhsIsTrue && !rhsIsTrue) || (!lhsIsTrue && rhsIsTrue));
        } else {
            return new Xor(lhs, rhs);
        }
    }

    @Override
    public String toString() {
        return "(" + lhs + " XOR " + rhs + ")";
    }
}
