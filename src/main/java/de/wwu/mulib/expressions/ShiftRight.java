package de.wwu.mulib.expressions;

public class ShiftRight extends AbstractOperatorNumericExpression implements NumericBitwiseOperation {
    protected ShiftRight(NumericExpression expr0, NumericExpression expr1) {
        super(expr0, expr1);
    }

    public static NumericExpression newInstance(NumericExpression expr0, NumericExpression expr1) {
        return new ShiftRight(expr0, expr1);
    }
}
