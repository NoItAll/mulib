package de.wwu.mulib.expressions;

public class NumericAnd extends AbstractOperatorNumericExpression implements NumericBitwiseOperation {
    protected NumericAnd(NumericExpression expr0, NumericExpression expr1) {
        super(expr0, expr1);
    }

    public static NumericExpression newInstance(NumericExpression expr0, NumericExpression expr1) {
        return new NumericAnd(expr0, expr1);
    }
}
