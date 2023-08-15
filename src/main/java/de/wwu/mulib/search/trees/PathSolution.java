package de.wwu.mulib.search.trees;

import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.constraints.PartnerClassObjectConstraint;
import de.wwu.mulib.solving.Solution;

/**
 * A tree node representing the result of exploring a path through the {@link SearchTree}.
 * This node indicates that the search region was left by returning a value from it
 */
public class PathSolution extends TreeNode {

    private final Constraint[] pathConstraints;
    private final PartnerClassObjectConstraint[] partnerClassObjectConstraints;
    private final Solution solution;

    PathSolution(Choice.ChoiceOption parent, Solution s, Constraint[] pathConstraints, PartnerClassObjectConstraint[] partnerClassObjectConstraints) {
        super(parent);
        this.pathConstraints = pathConstraints;
        this.partnerClassObjectConstraints = partnerClassObjectConstraints;
        this.solution = s;
    }

    /**
     * @return The constraints of the path of this path solution
     */
    public final Constraint[] getPathConstraints() {
        return pathConstraints;
    }

    /**
     * @return The partner class constraints of the path of this path solution
     */
    public final PartnerClassObjectConstraint[] getPartnerClassObjectConstraints() {
        return partnerClassObjectConstraints;
    }

    /**
     * @return The object wrapping the remembered values and the return value of this path solution
     */
    public final Solution getSolution() {
        return solution;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{depth=" + depth + ",solution=" + solution +"}";
    }
}
