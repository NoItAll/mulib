package de.wwu.mulib.tcg.testsetreducer;

import de.wwu.mulib.tcg.TestCase;

import java.util.Collection;

public class CombinedTestSetReducer implements TestSetReducer {

    private final TestSetReducer[] inOrder;
    public CombinedTestSetReducer(TestSetReducer... inOrder) {
        this.inOrder = inOrder;
    }

    @Override
    public Collection<TestCase> apply(Collection<TestCase> testCases) {
        Collection<TestCase> result = testCases;
        for (TestSetReducer tsr : inOrder) {
            result = tsr.apply(result);
        }
        return result;
    }
}
