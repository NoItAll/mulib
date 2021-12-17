package de.wwu.mulib.constraints;

public interface TwoSidedConstraint extends Constraint {

    Constraint getLhs();
    Constraint getRhs();

}
