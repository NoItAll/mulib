package de.wwu.mulib.search.trees;

import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.constraints.PartnerClassObjectConstraint;

public class PathSolution extends TreeNode {

    private final Constraint[] pathConstraints;
    private final PartnerClassObjectConstraint[] partnerClassObjectConstraints;
    private final Solution solution;

    public PathSolution(Choice.ChoiceOption parent, Solution s, Constraint[] pathConstraints, PartnerClassObjectConstraint[] partnerClassObjectConstraints) {
        super(parent);
        this.pathConstraints = pathConstraints;
        this.partnerClassObjectConstraints = partnerClassObjectConstraints;
        this.solution = s;
    }

    public final Constraint[] getPathConstraints() {
        return pathConstraints;
    }

    public final PartnerClassObjectConstraint[] getPartnerClassObjectConstraints() {
        return partnerClassObjectConstraints;
    }

    public final Solution getSolution() {
        return solution;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{depth=" + depth + ",solution=" + solution +"}";
    }
}
