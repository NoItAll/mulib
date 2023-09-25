package de.wwu.mulib.constraints;

import java.util.List;

/**
 * Type representing constraints that consist of two subconstraints, such as AND(c0, c1), or OR(c0, c1),
 * or XOR(c0, c1), ...
 */
public interface TwoSidedConstraint extends Constraint {

    /**
     * @return The left-hand side of the composed constraint
     */
    Constraint getLhs();
    /**
     * @return The right-hand side of the composed constraint
     */
    Constraint getRhs();

    /**
     * Checks for nested repetitions of the same type.
     * For instance, if {@link And} is unrolled, and there is an expression such as
     * ((b0 && b1) && (b2 || b3)), [b0, b1, (b2 || b3)] is returned.
     * @return A list of constraints
     */
    List<Constraint> unrollSameType();

}
