package de.wwu.mulib.expressions;

import de.wwu.mulib.search.NumberUtil;
import de.wwu.mulib.substitutions.primitives.ConcSnumber;

public class Abs extends AbstractExpressionWrappingExpression {

    protected Abs(NumericExpression toWrap) {
        super(toWrap);
    }

    public static NumericExpression abs(NumericExpression numericExpression) {
        if (isConcrete(numericExpression)) {
            return NumberUtil.abs((ConcSnumber) numericExpression);
        } else {
            return new Abs(numericExpression);
        }
    }
}
