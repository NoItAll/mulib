package de.wwu.mulib.constraints;

import de.wwu.mulib.substitutions.primitives.Sbool;

public class Or extends AbstractTwoSidedConstraint {

    private Or(Constraint lhs, Constraint rhs) {
        super(lhs, rhs);
    }

    public static Constraint newInstance(Constraint lhs, Constraint rhs) {
        if (bothConstraintsAreConcrete(lhs, rhs)) {
            return Sbool.concSbool(((Sbool.ConcSbool) lhs).isTrue() || ((Sbool.ConcSbool) rhs).isTrue());
        } else {
            return new Or(lhs, rhs);
        }
    }

    public static Constraint newInstance(Constraint... constraints) {
        if (constraints.length == 0) {
            throw new IllegalArgumentException("There must be at least one constraint.");
        } else if (constraints.length == 1) {
            return constraints[0];
        }

        Constraint result = constraints[0];
        for (int i = 1; i < constraints.length; i++) {
            result = newInstance(result, constraints[i]);
        }

        return result;
    }

    @Override
    public String toString() {
        return "(" + lhs + " || " + rhs + ")";
    }
}
