package de.wwu.mulib.expressions;

public class LogicalShiftRight extends AbstractOperatorNumericExpression implements NumericBitwiseOperation {
    protected LogicalShiftRight(NumericExpression expr0, NumericExpression expr1) {
        super(expr0, expr1);
    }

    public static NumericExpression newInstance(NumericExpression expr0, NumericExpression expr1) {
        return new LogicalShiftRight(expr0, expr1);
    }
}
