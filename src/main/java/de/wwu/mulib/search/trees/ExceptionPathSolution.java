package de.wwu.mulib.search.trees;

import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.constraints.PartnerClassObjectConstraint;

public class ExceptionPathSolution extends PathSolution {
    public ExceptionPathSolution(Choice.ChoiceOption choiceOption, Solution s, Constraint[] constraints, PartnerClassObjectConstraint[] partnerClassObjectConstraints) {
        super(choiceOption, s, constraints, partnerClassObjectConstraints);
    }
}
