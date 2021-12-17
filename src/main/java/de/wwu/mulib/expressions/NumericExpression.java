package de.wwu.mulib.expressions;

public interface NumericExpression {

    default boolean isPrimitive() {
        return false;
    }

    boolean isFp();
}
