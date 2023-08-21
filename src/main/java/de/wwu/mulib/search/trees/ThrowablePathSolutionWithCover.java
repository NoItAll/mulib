package de.wwu.mulib.search.trees;

import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.constraints.PartnerClassObjectConstraint;
import de.wwu.mulib.solving.Solution;

import java.util.BitSet;

/**
 * Adds to {@link ThrowablePathSolution} by also storing a cover
 */
public class ThrowablePathSolutionWithCover extends ThrowablePathSolution implements IPathSolutionWithBitSetCover {

    private final BitSet cover;

    ThrowablePathSolutionWithCover(
            Choice.ChoiceOption choiceOption,
            Solution s,
            Constraint[] constraints,
            PartnerClassObjectConstraint[] partnerClassObjectConstraints,
            BitSet cover) {
        super(choiceOption, s, constraints, partnerClassObjectConstraints);
        this.cover = cover;
    }

    /**
     * @return The cover of the path solution in the form of a bit set
     */
    public BitSet getCover() {
        return cover;
    }
}
