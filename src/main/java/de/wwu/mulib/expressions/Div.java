package de.wwu.mulib.expressions;

import de.wwu.mulib.search.NumberUtil;
import de.wwu.mulib.substitutions.primitives.ConcSnumber;

public class Div extends AbstractOperatorNumericExpression {
    protected Div(NumericExpression lhsExpr, NumericExpression rhsExpr) {
        super(lhsExpr, rhsExpr);
    }

    public static NumericExpression div(NumericExpression expr0, NumericExpression expr1) {
        if (bothExprAreConcrete(expr0, expr1)) {
            return NumberUtil.divConcSnumber((ConcSnumber) expr0, (ConcSnumber) expr1);
        } else {
            return new Div(expr0, expr1);
        }
    }
    @Override
    public String toString() {
        return "(" + expr0 + " / " + expr1 + ")";
    }
}
