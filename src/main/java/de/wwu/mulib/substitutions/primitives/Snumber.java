package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.expressions.NumericExpression;

public interface Snumber extends Sprimitive, NumericExpression {

    @Override
    boolean isPrimitive();

}
