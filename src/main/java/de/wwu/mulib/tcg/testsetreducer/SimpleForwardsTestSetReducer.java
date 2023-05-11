package de.wwu.mulib.tcg.testsetreducer;

import de.wwu.mulib.tcg.TestCase;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;

public final class SimpleForwardsTestSetReducer implements TestSetReducer {

    @Override
    public List<TestCase> apply(Collection<TestCase> testCases) {
        List<TestCase> result = new ArrayList<>();
        // Currently no cover at all:
        BitSet currentCover = new BitSet();
        // If the coverage of the current cover with the cover of the current test case is the same as before do not add it:
        for (TestCase tc : testCases) {
            BitSet newCover = (BitSet) currentCover.clone();
            newCover.or(tc.getCover());
            if (currentCover.cardinality() < newCover.cardinality()) {
                result.add(tc);
                currentCover = newCover;
            }
        }
        return result;
    }
}
