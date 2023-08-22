package de.wwu.mulib.expressions;

/**
 * Represents an arithmetic bit-shift to the right
 */
public class ShiftRight extends AbstractOperatorNumericalExpression implements NumericBitwiseOperation {
    protected ShiftRight(NumericalExpression expr0, NumericalExpression expr1) {
        super(expr0, expr1);
    }

    /**
     * Returns either a simplified numeric expression, or an expression representing the shift of a number to the right
     * @param expr0 The first number
     * @param expr1 The second number
     * @return A numeric expression representing the division expr0 >> expr1
     */
    public static NumericalExpression newInstance(NumericalExpression expr0, NumericalExpression expr1) {
        return new ShiftRight(expr0, expr1);
    }
}
