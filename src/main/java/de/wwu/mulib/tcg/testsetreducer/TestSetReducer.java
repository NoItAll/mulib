package de.wwu.mulib.tcg.testsetreducer;

import de.wwu.mulib.tcg.TestCase;

import java.util.Collection;

/**
 * Reduces a collection of test sets.
 * It is not required that this reduction is loss-less.
 * Typically, it should work on the coverage of the various test cases.
 */
@FunctionalInterface
public interface TestSetReducer {

    /**
     * @param testCases The collection of test cases that should be reduced
     * @return A subset of the test cases
     */
    Collection<TestCase> apply(Collection<TestCase> testCases);

}
