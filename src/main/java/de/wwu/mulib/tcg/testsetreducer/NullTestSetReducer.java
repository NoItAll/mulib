package de.wwu.mulib.tcg.testsetreducer;

import de.wwu.mulib.tcg.TestCase;

import java.util.Collection;

public class NullTestSetReducer implements TestSetReducer {

    @Override
    public Collection<TestCase> apply(Collection<TestCase> testCases) {
        return testCases;
    }
}
