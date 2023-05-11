package de.wwu.mulib.tcg.testsetreducer;

import de.wwu.mulib.tcg.TestCase;

import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class SimpleBackwardsTestSetReducer implements TestSetReducer {

    @Override
    public Set<TestCase> apply(Collection<TestCase> testCases) {
        Set<TestCase> result = new HashSet<>(testCases);
        BitSet overallCover = calculateOverallCover(testCases);

        for (TestCase tc : testCases) {
            result.remove(tc);
            BitSet newOverallCover = calculateOverallCover(result);
            if (newOverallCover.cardinality() < overallCover.cardinality()) {
                result.add(tc);
            }
        }

        return result;
    }

    private static BitSet calculateOverallCover(Collection<TestCase> testCases) {
        BitSet result = new BitSet();
        // Add the cover for each test case
        for (TestCase tc : testCases) {
            result.or(tc.getCover());
        }

        return result;
    }

}
