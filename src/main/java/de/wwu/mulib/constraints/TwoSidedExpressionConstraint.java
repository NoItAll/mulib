package de.wwu.mulib.constraints;

import de.wwu.mulib.expressions.Expression;

public interface TwoSidedExpressionConstraint extends Constraint {

    Expression getLhs();

    Expression getRhs();

}
