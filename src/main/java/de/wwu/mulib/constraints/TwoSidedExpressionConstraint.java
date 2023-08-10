package de.wwu.mulib.constraints;

import de.wwu.mulib.expressions.NumericExpression;

/**
 * Type representing constraints that consist of comparing two numeric expressions, such as n0 < n1, or n0 <= n1,
 * or n0 == n1, ...
 */
public interface TwoSidedExpressionConstraint extends Constraint {

    NumericExpression getLhs();

    NumericExpression getRhs();

}
