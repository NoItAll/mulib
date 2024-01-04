package de.wwu.mulib.constraints;

import de.wwu.mulib.expressions.Expression;
import de.wwu.mulib.search.NumberUtil;
import de.wwu.mulib.substitutions.primitives.ConcSnumber;
import de.wwu.mulib.substitutions.primitives.Sbool;

/**
 * Represents the less-than relationship between two numeric expressions, i.e., n0 < n1
 */
public class Lt extends AbstractTwoSidedMathematicalConstraint {

    private Lt(Expression lhs, Expression rhs) {
        super(lhs, rhs);
    }

    /**
     * Creates a new constraint, possibly returning true, if both numeric expressions are concrete
     * @param lhs The left-hand side
     * @param rhs The right-hand side
     * @return If both lhs and rhs are concrete Sbool.ConcSbool.TRUE or Sbool.ConcSbool.FALSE are returned.
     * Otherwise lhs <= rhs is returned.
     */
    public static Constraint newInstance(Expression lhs, Expression rhs) {
        if (bothExprAreConcrete(lhs, rhs)) {
            return Sbool.concSbool(NumberUtil.lt((ConcSnumber) lhs, (ConcSnumber) rhs));
        } else {
            return new Lt(lhs, rhs);
        }
    }

    @Override
    public String toString() {
        return "(" + lhsExpr + " < " + rhsExpr + ")";
    }
}
