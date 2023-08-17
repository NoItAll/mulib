package de.wwu.mulib.tcg.testsetsorter;

import de.wwu.mulib.tcg.TestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Doesn't sort anything
 */
public class NullTestSetSorter extends AbstractTestSetSorter {
    @Override
    public List<TestCase> apply(Collection<TestCase> testCases) {
        if (testCases instanceof List) {
            return (List<TestCase>) testCases;
        }
        return new ArrayList<>(testCases);
    }
}
