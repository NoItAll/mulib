package de.wwu.mulib.constraints;

import de.wwu.mulib.substitutions.primitives.Sbool;

import java.util.List;
import java.util.RandomAccess;

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
        return newInstance(List.of(constraints));
    }

    public static Constraint newInstance(List<Constraint> constraints) {
        assert constraints instanceof RandomAccess;
        if (constraints.size() == 0) {
            throw new IllegalArgumentException("There must be at least one constraint.");
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
        return "(" + lhs + " || " + rhs + ")";
    }
}
