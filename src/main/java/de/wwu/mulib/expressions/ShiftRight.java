package de.wwu.mulib.expressions;

/**
 * Represents an arithmetic bit-shift to the right
 */
public class ShiftRight extends AbstractOperatorMathematicalExpression implements NumericBitwiseOperation {
    protected ShiftRight(Expression expr0, Expression expr1) {
        super(expr0, expr1);
    }

    /**
     * Returns either a simplified numeric expression, or an expression representing the shift of a number to the right
     * @param expr0 The first number
     * @param expr1 The second number
     * @return A numeric expression representing the division expr0 >> expr1
     */
    public static Expression newInstance(Expression expr0, Expression expr1) {
        return new ShiftRight(expr0, expr1);
    }
}
