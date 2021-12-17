package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.expressions.NumericExpression;

public interface SymNumericExpressionSprimitive extends Snumber, SymSprimitive {

    NumericExpression getRepresentedExpression();

}
