package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.expressions.NumericalExpression;

public interface SymSnumber extends Snumber, SymSprimitive {

    NumericalExpression getRepresentedExpression();

}
