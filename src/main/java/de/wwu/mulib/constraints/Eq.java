package de.wwu.mulib.constraints;

import de.wwu.mulib.expressions.NumericExpression;
import de.wwu.mulib.search.NumberUtil;
import de.wwu.mulib.substitutions.primitives.ConcSnumber;
import de.wwu.mulib.substitutions.primitives.Sbool;

public class Eq extends AbstractTwoSidedNumericConstraint {

    public Eq(NumericExpression lhs, NumericExpression rhs) {
        super(lhs, rhs);
    }

    public static Constraint newInstance(NumericExpression lhs, NumericExpression rhs) {
        if (bothExprAreConcrete(lhs, rhs)) {
            return Sbool.concSbool(NumberUtil.eq((ConcSnumber) lhs, (ConcSnumber) rhs));
        } else {
            return new Eq(lhs, rhs);
        }
    }

    @Override
    public String toString() {
        return "(" + lhsExpr + " == " + rhsExpr + ")";
    }
}
