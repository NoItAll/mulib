package de.wwu.mulib.expressions;

/**
 * Represents the addition of two numbers
 */
public class Sum extends AbstractOperatorMathematicalExpression {

    private Sum(Expression expr0, Expression expr1) {
        super(expr0, expr1);
    }

    /**
     * Returns either a simplified numeric expression, or an expression representing the addition of two numbers
     * @param expr0 The first number
     * @param expr1 The second number
     * @return A numeric expression representing the addition expr0+expr1
     */
    public static Expression newInstance(Expression expr0, Expression expr1) {
        return new Sum(expr0, expr1);
    }

    @Override
    public String toString() {
        return "(" + expr0 + " + " + expr1 + ")";
    }
}
