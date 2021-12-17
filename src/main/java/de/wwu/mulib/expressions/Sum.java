package de.wwu.mulib.expressions;

import de.wwu.mulib.search.NumberUtil;
import de.wwu.mulib.substitutions.primitives.ConcSnumber;

public final class Sum extends AbstractOperatorNumericExpression {

    private Sum(NumericExpression expr0, NumericExpression expr1) {
        super(expr0, expr1);
    }

    public static NumericExpression sum(NumericExpression expr0, NumericExpression expr1) {
        if (bothExprAreConcrete(expr0, expr1)) {
            return NumberUtil.addConcSnumber((ConcSnumber) expr0, (ConcSnumber) expr1);
        } else {
            return new Sum(expr0, expr1);
        }
    }

    @Override
    public String toString() {
        return "(" + expr0 + " + " + expr1 + ")";
    }

}
