package de.wwu.mulib.tcg.testsetsorter;

import de.wwu.mulib.tcg.TestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class StdTestSetSorter implements TestSetSorter {
    protected final Comparator<TestCase> comparator;

    public StdTestSetSorter() {
        this.comparator = Comparator.comparingLong(TestCase::getTestCaseNumber);
    }

    public StdTestSetSorter(Comparator<TestCase> testCaseComparator) {
        this.comparator = testCaseComparator;
    }

    @Override
    public List<TestCase> apply(Collection<TestCase> testCases) {
        List<TestCase> result = new ArrayList<>(testCases);
        result.sort(comparator);
        return result;
    }
}
