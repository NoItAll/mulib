package de.wwu.mulib.expressions;

/**
 * Represents the divison of two numbers
 */
public class Div extends AbstractOperatorMathematicalExpression {

    private Div(Expression lhsExpr, Expression rhsExpr) {
        super(lhsExpr, rhsExpr);
    }

    /**
     * Returns either a simplified numeric expression, or an expression representing the division of two numbers
     * @param expr0 The first number
     * @param expr1 The second number
     * @return A numeric expression representing the division expr0/expr1
     */
    public static Expression newInstance(Expression expr0, Expression expr1) {
        return new Div(expr0, expr1);
    }

    @Override
    public String toString() {
        return "(" + expr0 + " / " + expr1 + ")";
    }
}
