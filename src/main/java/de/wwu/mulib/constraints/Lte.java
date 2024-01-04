package de.wwu.mulib.constraints;

import de.wwu.mulib.expressions.Expression;
import de.wwu.mulib.search.NumberUtil;
import de.wwu.mulib.substitutions.primitives.ConcSnumber;
import de.wwu.mulib.substitutions.primitives.Sbool;

/**
 * Represents the less-than-equals relationship between two numeric expressions, i.e., n0 <= n1
 */
public class Lte extends AbstractTwoSidedMathematicalConstraint {

    private Lte(Expression lhs, Expression rhs) {
        super(lhs, rhs);
    }

    public static Constraint newInstance(Expression lhs, Expression rhs) {
        if (bothExprAreConcrete(lhs, rhs)) {
            return Sbool.concSbool(NumberUtil.lte((ConcSnumber) lhs, (ConcSnumber) rhs));
        } else {
            return new Lte(lhs, rhs);
        }
    }
    @Override
    public String toString() {
        return "(" + lhsExpr + " <= " + rhsExpr + ")";
    }
}
