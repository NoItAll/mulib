package de.wwu.mulib.tcg.testsetreducer;

import de.wwu.mulib.tcg.TestCase;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Test set reducer that delegates to other test set reducers, executes them all, and returns the smallest subset.
 */
public class CompetingTestSetReducer extends AbstractTestSetReducer {
    private final TestSetReducer[] testSetReducers;
    private final boolean useParallelStream;
    public CompetingTestSetReducer(TestSetReducer... testSetReducers) {
        this.testSetReducers = testSetReducers;
        this.useParallelStream = false;
    }

    public CompetingTestSetReducer(boolean useParallelStream, TestSetReducer... testSetReducers) {
        this.testSetReducers = testSetReducers;
        this.useParallelStream = useParallelStream;
    }

    @Override
    public Collection<TestCase> apply(Collection<TestCase> testCases) {
        Stream<TestSetReducer> stream = Arrays.stream(testSetReducers);
        if (useParallelStream) {
            stream = stream.parallel();
        }
        Collection<TestCase> result =
                stream
                        .map(tsr -> tsr.apply(testCases))
                        .min(Comparator.comparingInt(Collection::size))
                        .get();
        return result;
    }
}
