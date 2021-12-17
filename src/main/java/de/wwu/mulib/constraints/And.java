package de.wwu.mulib.constraints;

import de.wwu.mulib.substitutions.primitives.Sbool;

import java.util.Arrays;
import java.util.List;

public class And extends AbstractTwoSidedConstraint {

    private And(Constraint lhs, Constraint rhs) {
        super(lhs, rhs);
    }

    public static Constraint newInstance(Constraint lhs, Constraint rhs) {
        if (bothConstraintsAreConcrete(lhs, rhs)) {
            return Sbool.ConcSbool.newConcSbool(((Sbool.ConcSbool) lhs).isTrue() && ((Sbool.ConcSbool) rhs).isTrue());
        } else {
            return new And(lhs, rhs);
        }
    }

    public static Constraint newInstance(Constraint... constraints) {
        return newInstance(Arrays.asList(constraints));
    }

    public static Constraint newInstance(List<Constraint> constraints) {
        if (constraints.isEmpty()) {
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
        return "(" + lhs + " && " + rhs + ")";
    }
}
