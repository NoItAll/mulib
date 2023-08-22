package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.expressions.NumericExpression;

public interface SymSnumber extends Snumber, SymSprimitive {

    NumericExpression getRepresentedExpression();

}
