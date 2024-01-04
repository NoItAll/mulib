package de.wwu.mulib.constraints;

import de.wwu.mulib.expressions.Expression;
import de.wwu.mulib.search.NumberUtil;
import de.wwu.mulib.substitutions.primitives.ConcSnumber;
import de.wwu.mulib.substitutions.primitives.Sbool;

/**
 * Represents the equality between two numeric expressions, i.e., n0 == n1
 */
public class Eq extends AbstractTwoSidedMathematicalConstraint {

    private Eq(Expression lhs, Expression rhs) {
        super(lhs, rhs);
    }

    /**
     * Creates a new constraint, possibly simplifying the overall constraint
     * @param lhs The left-hand side
     * @param rhs The right-hand side
     * @return A constraint that is either simplified or lhs == rhs
     */
    public static Constraint newInstance(Expression lhs, Expression rhs) {
        if (bothExprAreConcrete(lhs, rhs)) {
            return Sbool.concSbool(NumberUtil.eq((ConcSnumber) lhs, (ConcSnumber) rhs));
        } else if (lhs == rhs) {
            return Sbool.ConcSbool.TRUE;
        } else {
            return new Eq(lhs, rhs);
        }
    }

    @Override
    public String toString() {
        return "(" + lhsExpr + " == " + rhsExpr + ")";
    }
}
