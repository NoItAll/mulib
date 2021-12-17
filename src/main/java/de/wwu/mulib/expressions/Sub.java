package de.wwu.mulib.expressions;

public class Sub extends AbstractOperatorNumericExpression {

    private Sub(NumericExpression expr0, NumericExpression expr1) {
        super(expr0, expr1);
    }

    public static NumericExpression newInstance(NumericExpression expr0, NumericExpression expr1) {
        return new Sub(expr0, expr1);
    }

    @Override
    public String toString() {
        return "(" + expr0 + " - " + expr1 + ")";
    }
}
