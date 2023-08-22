package de.wwu.mulib.expressions;

/**
 * Represents the bit-wise XOR operation of two numbers
 */
public class NumericalXor extends AbstractOperatorNumericalExpression implements NumericBitwiseOperation {
    protected NumericalXor(NumericalExpression expr0, NumericalExpression expr1) {
        super(expr0, expr1);
    }

    /**
     * Returns either a simplified numeric expression, or an expression representing the bit-wise XOR operation of two numbers
     * @param expr0 The first number
     * @param expr1 The second number
     * @return A numeric expression representing the bit-wise XOR operation  expr0 ^ expr1
     */
    public static NumericalExpression newInstance(NumericalExpression expr0, NumericalExpression expr1) {
        return new NumericalXor(expr0, expr1);
    }
}
