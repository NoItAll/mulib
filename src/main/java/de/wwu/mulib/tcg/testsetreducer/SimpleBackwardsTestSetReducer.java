package de.wwu.mulib.tcg.testsetreducer;

import de.wwu.mulib.tcg.TestCase;

import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Starts with a collection in which all test cases are present.
 * Then starts removing test cases in-order. If the new overall test set coverage is not reduced,
 * the test case is discarded. Otherwise, the test case is added again.
 * This is a coverage loss-less reducer.
 */
public final class SimpleBackwardsTestSetReducer extends AbstractTestSetReducer {

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
