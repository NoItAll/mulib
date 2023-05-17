package de.wwu.mulib.expressions;

public class NumericOr extends AbstractOperatorNumericExpression implements NumericBitwiseOperation {
    protected NumericOr(NumericExpression expr0, NumericExpression expr1) {
        super(expr0, expr1);
    }

    public static NumericExpression newInstance(NumericExpression expr0, NumericExpression expr1) {
        return new NumericOr(expr0, expr1);
    }
}
