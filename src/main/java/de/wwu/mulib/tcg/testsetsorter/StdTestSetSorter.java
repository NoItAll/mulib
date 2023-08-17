package de.wwu.mulib.tcg.testsetsorter;

import de.wwu.mulib.tcg.TestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToLongFunction;

/**
 * Sorts the test cases according to some criterion
 */
public class StdTestSetSorter extends AbstractTestSetSorter {
    private final Comparator<TestCase> comparator;

    /**
     * Creates an instance applying {@link Comparator#comparingLong(ToLongFunction)} to {@link TestCase#getTestCaseNumber()}s
     */
    public StdTestSetSorter() {
        this.comparator = Comparator.comparingLong(TestCase::getTestCaseNumber);
    }

    /**
     * Creates an instance with a custom comparator
     * @param testCaseComparator The comparator
     */
    public StdTestSetSorter(Comparator<TestCase> testCaseComparator) {
        this.comparator = testCaseComparator;
    }

    @Override
    public List<TestCase> apply(Collection<TestCase> testCases) {
        List<TestCase> result;
        if (!(testCases instanceof List)) {
            result = new ArrayList<>(testCases);
        } else {
            result = (List<TestCase>) testCases;
        }
        result.sort(comparator);
        return result;
    }
}
