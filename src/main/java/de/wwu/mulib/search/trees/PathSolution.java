package de.wwu.mulib.search.trees;

import de.wwu.mulib.constraints.ArrayConstraint;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.solving.Labels;

public class PathSolution extends TreeNode {

    private final Constraint[] pathConstraints;
    private final ArrayConstraint[] arrayConstraints;
    private final Solution solution;

    public PathSolution(Choice.ChoiceOption parent, Object value, Labels labels, Constraint[] pathConstraints, ArrayConstraint[] arrayConstraints) {
        super(parent);
        this.pathConstraints = pathConstraints;
        this.arrayConstraints = arrayConstraints;
        this.solution = new Solution(value, labels);
    }

    public final Constraint[] getPathConstraints() {
        return pathConstraints;
    }

    public final ArrayConstraint[] getArrayConstraints() {
        return arrayConstraints;
    }

    public final Solution getSolution() {
        return solution;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{depth=" + depth + ",solution=" + solution +"}";
    }
}
