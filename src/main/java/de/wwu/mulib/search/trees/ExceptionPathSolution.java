package de.wwu.mulib.search.trees;

import de.wwu.mulib.constraints.ArrayConstraint;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.solving.Labels;

import java.util.List;

public final class ExceptionPathSolution extends PathSolution {
    public ExceptionPathSolution(Choice.ChoiceOption choiceOption, Throwable value, Labels l, Constraint[] constraints, List<ArrayConstraint> arrayConstraints) {
        super(choiceOption, value, l, constraints, arrayConstraints);
    }

    @Override
    public String toString() {
        return "ExceptionPathSolution{depth=" + depth + ",solutions=" + getCurrentlyInitializedSolutions() +"}";
    }
}
