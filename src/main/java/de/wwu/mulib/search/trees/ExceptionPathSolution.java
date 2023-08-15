package de.wwu.mulib.search.trees;

import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.constraints.PartnerClassObjectConstraint;
import de.wwu.mulib.solving.Solution;

/**
 * A tree node representing the result of exploring a path through the {@link SearchTree}.
 * This node indicates that the search region was left via a thrown exception which can be retrieved via
 * getting the return value of {@link PathSolution#getSolution()}
 */
public class ExceptionPathSolution extends PathSolution {
    ExceptionPathSolution(Choice.ChoiceOption choiceOption, Solution s, Constraint[] constraints, PartnerClassObjectConstraint[] partnerClassObjectConstraints) {
        super(choiceOption, s, constraints, partnerClassObjectConstraints);
    }
}
