package de.wwu.mulib.expressions;

public class Div extends AbstractOperatorNumericExpression {

    private Div(NumericExpression lhsExpr, NumericExpression rhsExpr) {
        super(lhsExpr, rhsExpr);
    }

    public static NumericExpression newInstance(NumericExpression expr0, NumericExpression expr1) {
        return new Div(expr0, expr1);
    }

    @Override
    public String toString() {
        return "(" + expr0 + " / " + expr1 + ")";
    }
}
