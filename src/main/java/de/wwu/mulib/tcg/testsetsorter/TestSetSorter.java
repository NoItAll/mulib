package de.wwu.mulib.tcg.testsetsorter;

import de.wwu.mulib.tcg.TestCase;

import java.util.Collection;
import java.util.List;

@FunctionalInterface
public interface TestSetSorter {
    List<TestCase> apply(Collection<TestCase> testCases);

}
