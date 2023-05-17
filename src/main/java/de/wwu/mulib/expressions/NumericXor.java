package de.wwu.mulib.expressions;

public class NumericXor extends AbstractOperatorNumericExpression implements NumericBitwiseOperation {
    protected NumericXor(NumericExpression expr0, NumericExpression expr1) {
        super(expr0, expr1);
    }

    public static NumericExpression newInstance(NumericExpression expr0, NumericExpression expr1) {
        return new NumericXor(expr0, expr1);
    }
}
