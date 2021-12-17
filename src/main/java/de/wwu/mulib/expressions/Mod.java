package de.wwu.mulib.expressions;

public class Mod extends AbstractOperatorNumericExpression {

    private Mod(NumericExpression expr0, NumericExpression expr1) {
        super(expr0, expr1);
    }

    public static NumericExpression newInstance(NumericExpression expr0, NumericExpression expr1) {
        return new Mod(expr0, expr1);
    }

    @Override
    public String toString() {
        return "(" + expr0 + " % " + expr1 + ")";
    }
}
