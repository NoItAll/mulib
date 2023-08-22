package de.wwu.mulib.expressions;

/**
 * Represents the multiplication of two numbers
 */
public class Mul extends AbstractOperatorNumericalExpression {

    private Mul(NumericalExpression expr0, NumericalExpression expr1) {
        super(expr0, expr1);
    }

    /**
     * Returns either a simplified numeric expression, or an expression representing the multiplication of two numbers
     * @param expr0 The first number
     * @param expr1 The second number
     * @return A numeric expression representing the multiplication expr0 * expr1
     */
    public static NumericalExpression newInstance(NumericalExpression expr0, NumericalExpression expr1) {
        if (expr0 instanceof Neg && expr1 instanceof Neg) {
            expr0 = ((Neg) expr0).getWrapped();
            expr1 = ((Neg) expr1).getWrapped();
        }
        return new Mul(expr0, expr1);
    }

    @Override
    public String toString() {
        return "(" + expr0 + " * " + expr1 + ")";
    }
}
