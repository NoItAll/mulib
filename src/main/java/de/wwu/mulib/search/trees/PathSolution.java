package de.wwu.mulib.search.trees;

import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.solving.Labels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PathSolution extends TreeNode {

    private final Constraint[] pathConstraints;
    private final List<Solution> solutions;

    public PathSolution(Choice.ChoiceOption parent, Object value, Labels labels, Constraint[] pathConstraints) {
        super(parent);
        this.solutions = new ArrayList<>();
        this.pathConstraints = pathConstraints;
        Solution initialSolution = new Solution(value, labels);
        solutions.add(initialSolution);
    }

    public final Constraint[] getPathConstraints() {
        return pathConstraints;
    }

    public final List<Solution> getCurrentlyInitializedSolutions() {
        return Collections.unmodifiableList(solutions);
    }

    public final Solution getInitialSolution() {
        return solutions.get(0);
    }

    public final Solution getLatestSolution() {
        return solutions.get(solutions.size() - 1);
    }

    public final void addSolution(Solution solution) {
        solutions.add(solution);
    }

    @Override
    public String toString() {
        return "PathSolution{depth=" + depth + ",solutions=" + getCurrentlyInitializedSolutions() +"}";
    }
}
