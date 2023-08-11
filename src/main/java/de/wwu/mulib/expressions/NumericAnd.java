package de.wwu.mulib.expressions;

/**
 * Represents the bit-wise AND-operation of two numbers
 */
public class NumericAnd extends AbstractOperatorNumericExpression implements NumericBitwiseOperation {
    protected NumericAnd(NumericExpression expr0, NumericExpression expr1) {
        super(expr0, expr1);
    }

    /**
     * Returns either a simplified numeric expression, or an expression representing the bit-wise AND operation of two numbers
     * @param expr0 The first number
     * @param expr1 The second number
     * @return A numeric expression representing the bit-wise AND operation expr0 & expr1
     */
    public static NumericExpression newInstance(NumericExpression expr0, NumericExpression expr1) {
        return new NumericAnd(expr0, expr1);
    }
}
