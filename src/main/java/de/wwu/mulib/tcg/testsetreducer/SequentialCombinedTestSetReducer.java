package de.wwu.mulib.tcg.testsetreducer;

import de.wwu.mulib.tcg.TestCase;

import java.util.Arrays;
import java.util.Collection;

/**
 * Simple wrapper for pipelining two {@link TestSetReducer}s.
 * These test set reducers than forward their output to the next reducer in the pipeline, if any
 */
public class SequentialCombinedTestSetReducer extends AbstractTestSetReducer {

    private final TestSetReducer[] inOrder;

    /**
     * @param inOrder The reducers that are applied in-order
     */
    public SequentialCombinedTestSetReducer(TestSetReducer... inOrder) {
        this.inOrder = Arrays.copyOf(inOrder, inOrder.length);
    }

    @Override
    public Collection<TestCase> apply(Collection<TestCase> testCases) {
        Collection<TestCase> result = testCases;
        for (TestSetReducer tsr : inOrder) {
            result = tsr.apply(result);
        }
        return result;
    }

    @Override
    public String toString() {
        return String.format("CombinedTestSetReducer%s", Arrays.toString(inOrder));
    }
}
