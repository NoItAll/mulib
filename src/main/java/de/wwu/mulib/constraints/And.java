package de.wwu.mulib.constraints;

import de.wwu.mulib.substitutions.primitives.Sbool;

import java.util.Arrays;
import java.util.List;

public class And extends AbstractTwoSidedConstraint {

    private And(Constraint lhs, Constraint rhs) {
        super(lhs, rhs);
    }

    public static Constraint newInstance(Constraint lhs, Constraint rhs) {
        if (lhs instanceof Sbool.ConcSbool) {
            return evaluateConcrete((Sbool.ConcSbool) lhs, rhs);
        } else if (rhs instanceof Sbool.ConcSbool) {
            return evaluateConcrete((Sbool.ConcSbool) rhs, lhs);
        } else {
            return new And(lhs, rhs);
        }
    }

    private static Constraint evaluateConcrete(Sbool.ConcSbool concrete, Constraint nonConcrete) {
        if (concrete.isFalse()) {
            return Sbool.FALSE;
        } else {
            return nonConcrete;
        }
    }

    public static Constraint newInstance(Constraint... constraints) {
        return newInstance(Arrays.asList(constraints));
    }

    public static Constraint newInstance(List<Constraint> constraints) {
        if (constraints.isEmpty()) {
            return Sbool.TRUE;
        } else if (constraints.size() == 1) {
            return constraints.get(0);
        }

        Constraint result = constraints.get(0);
        for (int i = 1; i < constraints.size(); i++) {
            result = newInstance(result, constraints.get(i));
        }

        return result;
    }

    @Override
    public String toString() {
        return "(" + lhs + " && " + rhs + ")";
    }
}
