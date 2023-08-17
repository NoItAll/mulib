package de.wwu.mulib.tcg.testsetreducer;

import de.wwu.mulib.tcg.TestCase;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;

/**
 * Starts with an empty collection of test cases.
 * Then starts adding test cases that contributes the most to increasing the cardinality of the coverage of
 * the current set of test cases.
 * If the cardinality did not increase, the procedure terminates.
 * This is a coverage loss-less reducer.
 */
public class SimpleGreedyTestSetReducer extends AbstractTestSetReducer {

    @Override
    public Collection<TestCase> apply(Collection<TestCase> testCases) {
        List<TestCase> currentlyRegardedTestCases = new ArrayList<>(testCases);
        BitSet currentCover = new BitSet();
        int currentMaxCardinality = -1;
        List<TestCase> result = new ArrayList<>();
        while (currentMaxCardinality < currentCover.cardinality()) {
            TestCase maxCurrent = null;
            currentMaxCardinality = currentCover.cardinality();
            int maxCardinalityInInnerIteration = currentMaxCardinality;
            List<TestCase> pruneAway = new ArrayList<>();
            for (TestCase tc : currentlyRegardedTestCases) {
                BitSet currentEnrichedWithTc = new BitSet();
                currentEnrichedWithTc.or(currentCover);
                currentEnrichedWithTc.or(tc.getCover());
                int enrichedCardinality = currentEnrichedWithTc.cardinality();
                if (enrichedCardinality > maxCardinalityInInnerIteration) {
                    maxCurrent = tc;
                    maxCardinalityInInnerIteration = enrichedCardinality;
                } else if (enrichedCardinality <= currentMaxCardinality) {
                    pruneAway.add(tc);
                }
            }
            if (maxCurrent == null) {
                break;
            }
            currentlyRegardedTestCases.removeAll(pruneAway);
            result.add(maxCurrent);
            currentCover.or(maxCurrent.getCover());
        }
        return result;
    }
}
