package de.wwu.mulib.expressions;

/**
 * Represents the bit-wise OR operation of two numbers
 */
public class BitwiseOr extends AbstractOperatorMathematicalExpression implements NumericBitwiseOperation {
    protected BitwiseOr(Expression expr0, Expression expr1) {
        super(expr0, expr1);
    }

    /**
     * Returns either a simplified numeric expression, or an expression representing the bit-wise OR operation of two numbers
     * @param expr0 The first number
     * @param expr1 The second number
     * @return A numeric expression representing the bit-wise OR operation expr0 | expr1
     */
    public static Expression newInstance(Expression expr0, Expression expr1) {
        return new BitwiseOr(expr0, expr1);
    }
}
