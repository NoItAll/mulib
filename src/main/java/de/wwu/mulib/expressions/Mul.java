package de.wwu.mulib.expressions;

import de.wwu.mulib.search.NumberUtil;
import de.wwu.mulib.substitutions.primitives.ConcSnumber;

public final class Mul extends AbstractOperatorNumericExpression {

    private Mul(NumericExpression expr0, NumericExpression expr1) {
        super(expr0, expr1);
    }

    public static NumericExpression mul(NumericExpression expr0, NumericExpression expr1) {
        if (bothExprAreConcrete(expr0, expr1)) {
            return NumberUtil.mulConcSnumber((ConcSnumber) expr0, (ConcSnumber) expr1);
        } else {
            return new Mul(expr0, expr1);
        }
    }

    @Override
    public String toString() {
        return "(" + expr0 + " * " + expr1 + ")";
    }

}
