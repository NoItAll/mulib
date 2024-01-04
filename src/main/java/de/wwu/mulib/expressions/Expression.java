package de.wwu.mulib.expressions;

/**
 * Supertype of all mathematical expressions, such as {@link de.wwu.mulib.substitutions.primitives.Snumber} and
 * composed {@link AbstractOperatorMathematicalExpression}.
 */
public interface Expression {

    /**
     * @return Whether this NumericExpression is a floating-point number
     */
    boolean isFp();
}
