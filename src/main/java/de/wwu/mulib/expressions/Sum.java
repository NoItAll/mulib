package de.wwu.mulib.expressions;

/**
 * Represents the addition of two numbers
 */
public class Sum extends AbstractOperatorNumericalExpression {

    private Sum(NumericalExpression expr0, NumericalExpression expr1) {
        super(expr0, expr1);
    }

    /**
     * Returns either a simplified numeric expression, or an expression representing the addition of two numbers
     * @param expr0 The first number
     * @param expr1 The second number
     * @return A numeric expression representing the addition expr0+expr1
     */
    public static NumericalExpression newInstance(NumericalExpression expr0, NumericalExpression expr1) {
        return new Sum(expr0, expr1);
    }

    @Override
    public String toString() {
        return "(" + expr0 + " + " + expr1 + ")";
    }
}
