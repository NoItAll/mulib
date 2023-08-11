package de.wwu.mulib.expressions;

/**
 * Supertype of all numeric expressions, such as {@link de.wwu.mulib.substitutions.primitives.Snumber} and
 * composed {@link AbstractOperatorNumericExpression}.
 */
public interface NumericExpression {

    /**
     * @return Whether this NumericExpression is a floating-point number
     */
    boolean isFp();
}
