package de.wwu.mulib.constraints;

import de.wwu.mulib.expressions.NumericExpression;
import de.wwu.mulib.search.NumberUtil;
import de.wwu.mulib.substitutions.primitives.ConcSnumber;
import de.wwu.mulib.substitutions.primitives.Sbool;

public class Gte extends AbstractTwoSidedNumericConstraint {

    private Gte(NumericExpression lhs, NumericExpression rhs) {
        super(lhs, rhs);
    }

    public static Constraint newInstance(NumericExpression lhs, NumericExpression rhs) {
        if (bothExprAreConcrete(lhs, rhs)) {
            return Sbool.concSbool(NumberUtil.gte((ConcSnumber) lhs, (ConcSnumber) rhs));
        } else {
            return new Gte(lhs, rhs);
        }
    }

    @Override
    public String toString() {
        return "(" + lhsExpr + " >= " + rhsExpr + ")";
    }
}
