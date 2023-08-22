package de.wwu.mulib.expressions;

/**
 * Represents the modulo operation of two numbers
 */
public class Mod extends AbstractOperatorNumericalExpression {

    private Mod(NumericalExpression expr0, NumericalExpression expr1) {
        super(expr0, expr1);
    }

    /**
     * Returns either a simplified numeric expression, or an expression representing the modulo of two numbers
     * @param expr0 The first number
     * @param expr1 The second number
     * @return A numeric expression representing the modulo expr0%expr1
     */
    public static NumericalExpression newInstance(NumericalExpression expr0, NumericalExpression expr1) {
        return new Mod(expr0, expr1);
    }

    @Override
    public String toString() {
        return "(" + expr0 + " % " + expr1 + ")";
    }
}
