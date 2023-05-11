package de.wwu.mulib.tcg.testsetreducer;

import de.wwu.mulib.tcg.TestCase;

import java.util.Collection;

@FunctionalInterface
public interface TestSetReducer {

    Collection<TestCase> apply(Collection<TestCase> testCases);

}
