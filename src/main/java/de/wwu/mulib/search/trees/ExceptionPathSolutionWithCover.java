package de.wwu.mulib.search.trees;

import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.constraints.PartnerClassObjectConstraint;

import java.util.BitSet;

public class ExceptionPathSolutionWithCover extends ExceptionPathSolution implements IPathSolutionWithBitSetCover {

    private final BitSet cover;

    public ExceptionPathSolutionWithCover(
            Choice.ChoiceOption choiceOption,
            Solution s,
            Constraint[] constraints,
            PartnerClassObjectConstraint[] partnerClassObjectConstraints,
            BitSet cover) {
        super(choiceOption, s, constraints, partnerClassObjectConstraints);
        this.cover = cover;
    }

    public BitSet getCover() {
        return cover;
    }
}
