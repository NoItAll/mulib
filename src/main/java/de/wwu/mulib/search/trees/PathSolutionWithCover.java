package de.wwu.mulib.search.trees;

import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.constraints.PartnerClassObjectConstraint;

import java.util.BitSet;

public class PathSolutionWithCover extends PathSolution implements IPathSolutionWithBitSetCover {

    private final BitSet cover;

    public PathSolutionWithCover(
            Choice.ChoiceOption parent,
            Solution s,
            Constraint[] pathConstraints,
            PartnerClassObjectConstraint[] partnerClassObjectConstraints,
            BitSet cover) {
        super(parent, s, pathConstraints, partnerClassObjectConstraints);
        this.cover = cover;
    }

    public BitSet getCover() {
        return cover;
    }
}
