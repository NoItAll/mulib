package de.wwu.mulib.search.trees;

import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.solving.Labels;

public final class ExceptionPathSolution extends PathSolution {
    public ExceptionPathSolution(Choice.ChoiceOption choiceOption, Throwable value, Labels l, Constraint[] constraints) {
        super(choiceOption, value, l, constraints);
    }

    @Override
    public String toString() {
        return "ExceptionPathSolution{depth=" + depth + ",solutions=" + getCurrentlyInitializedSolutions() +"}";
    }
}
