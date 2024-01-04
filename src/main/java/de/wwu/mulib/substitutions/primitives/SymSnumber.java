package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.expressions.Expression;

public interface SymSnumber extends Snumber, SymSprimitive {

    Expression getRepresentedExpression();

}
