package de.wwu.mulib.expressions;

/**
 * Represents the bit-wise OR operation of two numbers
 */
public class NumericOr extends AbstractOperatorNumericExpression implements NumericBitwiseOperation {
    protected NumericOr(NumericExpression expr0, NumericExpression expr1) {
        super(expr0, expr1);
    }

    /**
     * Returns either a simplified numeric expression, or an expression representing the bit-wise OR operation of two numbers
     * @param expr0 The first number
     * @param expr1 The second number
     * @return A numeric expression representing the bit-wise OR operation expr0 | expr1
     */
    public static NumericExpression newInstance(NumericExpression expr0, NumericExpression expr1) {
        return new NumericOr(expr0, expr1);
    }
}
