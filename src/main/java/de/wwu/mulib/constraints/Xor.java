package de.wwu.mulib.constraints;

import de.wwu.mulib.substitutions.primitives.Sbool;

public class Xor extends AbstractTwoSidedConstraint {
    protected Xor(Constraint lhs, Constraint rhs) {
        super(lhs, rhs);
    }

    public static Constraint newInstance(Constraint lhs, Constraint rhs) {
        if (bothConstraintsAreConcrete(lhs, rhs)) {
            boolean lhsIsTrue = ((Sbool.ConcSbool) lhs).isTrue();
            boolean rhsIsTrue = ((Sbool.ConcSbool) rhs).isTrue();
            return Sbool.ConcSbool.newConcSbool((lhsIsTrue && !rhsIsTrue) || (!lhsIsTrue && rhsIsTrue));
        } else {
            return new Xor(lhs, rhs);
        }
    }

    @Override
    public String toString() {
        return "(" + lhs + " XOR " + rhs + ")";
    }
}
