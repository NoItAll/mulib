package de.wwu.mulib.constraints;

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

}
