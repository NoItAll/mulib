package de.wwu.mulib.constraints;

import de.wwu.mulib.substitutions.primitives.Sbool;

public class Or extends AbstractTwoSidedConstraint {

    private Or(Constraint lhs, Constraint rhs) {
        super(lhs, rhs);
    }

    public static Constraint newInstance(Constraint lhs, Constraint rhs) {
        if (lhs instanceof Sbool.ConcSbool) {
            return evaluateConcrete((Sbool.ConcSbool) lhs, rhs);
        } else if (rhs instanceof Sbool.ConcSbool) {
            return evaluateConcrete((Sbool.ConcSbool) rhs, lhs);
        } else {
            return new Or(lhs, rhs);
        }
    }

    private static Constraint evaluateConcrete(Sbool.ConcSbool concrete, Constraint nonConcrete) {
        if (concrete.isTrue()) {
            return Sbool.ConcSbool.TRUE;
        } else {
            return nonConcrete;
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
