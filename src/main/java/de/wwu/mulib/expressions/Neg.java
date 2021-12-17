package de.wwu.mulib.expressions;

import de.wwu.mulib.search.NumberUtil;
import de.wwu.mulib.substitutions.primitives.ConcSnumber;

public class Neg extends AbstractExpressionWrappingExpression {

    private Neg(NumericExpression wrapped) {
        super(wrapped);
    }

    public static NumericExpression neg(NumericExpression numericExpression) {
        if (isConcrete(numericExpression)) {
            return NumberUtil.neg((ConcSnumber) numericExpression);
        } else {
            return new Neg(numericExpression);
        }
    }
    @Override
    public String toString() {
        return "-(" + wrapped + ")";
    }
}
