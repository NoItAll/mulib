package de.wwu.mulib.search.trees;

import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.constraints.PartnerClassObjectConstraint;
import de.wwu.mulib.solving.Solution;

import java.util.BitSet;

/**
 * Adds to {@link PathSolution} by also storing a cover
 */
public class PathSolutionWithCover extends PathSolution implements IPathSolutionWithBitSetCover {

    private final BitSet cover;

    PathSolutionWithCover(
            Choice.ChoiceOption parent,
            Solution s,
            Constraint[] pathConstraints,
            PartnerClassObjectConstraint[] partnerClassObjectConstraints,
            BitSet cover) {
        super(parent, s, pathConstraints, partnerClassObjectConstraints);
        this.cover = cover;
    }

    /**
     * @return The cover of the path solution in the form of a bit set
     */
    public BitSet getCover() {
        return cover;
    }
}
