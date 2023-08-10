package de.wwu.mulib.constraints;

import de.wwu.mulib.substitutions.primitives.Sbool;

import java.util.Arrays;
import java.util.List;

/**
 * Represents a logical AND
 */
public class And extends AbstractTwoSidedConstraint {

    private And(Constraint lhs, Constraint rhs) {
        super(lhs, rhs);
    }

    /**
     * Creates a new constraint, possibly simplifying the overall constraint
     * @param lhs The left-hand side
     * @param rhs The right-hand side
     * @return A constraint that is either simplified or AND(lhs, rhs)
     */
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
            return Sbool.ConcSbool.FALSE;
        } else {
            return nonConcrete;
        }
    }

    public static Constraint newInstance(Constraint... constraints) {
        return newInstance(Arrays.asList(constraints));
    }

    public static Constraint newInstance(List<Constraint> constraints) {
        if (constraints.isEmpty()) {
            return Sbool.ConcSbool.TRUE;
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
