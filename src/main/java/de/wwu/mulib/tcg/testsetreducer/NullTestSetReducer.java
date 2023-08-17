package de.wwu.mulib.tcg.testsetreducer;

import de.wwu.mulib.tcg.TestCase;

import java.util.Collection;

/**
 * Does not reduce any test cases
 */
public class NullTestSetReducer extends AbstractTestSetReducer {

    @Override
    public Collection<TestCase> apply(Collection<TestCase> testCases) {
        return testCases;
    }
}
