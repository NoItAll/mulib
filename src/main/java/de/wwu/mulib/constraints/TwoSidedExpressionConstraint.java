package de.wwu.mulib.constraints;

import de.wwu.mulib.expressions.NumericExpression;

public interface TwoSidedExpressionConstraint extends Constraint {

    NumericExpression getLhs();

    NumericExpression getRhs();

}
