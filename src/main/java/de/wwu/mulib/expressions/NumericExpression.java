package de.wwu.mulib.expressions;

public interface NumericExpression extends Expression {

    default boolean isPrimitive() {
        return false;
    }

    boolean isFp();
}
