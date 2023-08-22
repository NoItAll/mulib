package de.wwu.mulib.expressions;

/**
 * Supertype of all numerical expressions, such as {@link de.wwu.mulib.substitutions.primitives.Snumber} and
 * composed {@link AbstractOperatorNumericalExpression}.
 */
public interface NumericalExpression {

    /**
     * @return Whether this NumericExpression is a floating-point number
     */
    boolean isFp();
}
