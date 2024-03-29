package de.wwu.mulib.expressions;

/**
 * Represents the subtraction of one number from another
 */
public class Sub extends AbstractOperatorMathematicalExpression {

    private Sub(Expression expr0, Expression expr1) {
        super(expr0, expr1);
    }

    /**
     * Returns either a simplified numeric expression, or an expression representing the subtraction of two numbers
     * @param expr0 The first number
     * @param expr1 The second number
     * @return A numeric expression representing the subtraction expr0-expr1
     */
    public static Expression newInstance(Expression expr0, Expression expr1) {
        return new Sub(expr0, expr1);
    }

    @Override
    public String toString() {
        return "(" + expr0 + " - " + expr1 + ")";
    }
}
