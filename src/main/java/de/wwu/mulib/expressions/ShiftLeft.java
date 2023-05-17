package de.wwu.mulib.expressions;

public class ShiftLeft extends AbstractOperatorNumericExpression implements NumericBitwiseOperation {
    protected ShiftLeft(NumericExpression expr0, NumericExpression expr1) {
        super(expr0, expr1);
    }

    public static NumericExpression newInstance(NumericExpression expr0, NumericExpression expr1) {
        return new ShiftLeft(expr0, expr1);
    }
}
