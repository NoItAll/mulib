package de.wwu.mulib.tcg;

import de.wwu.mulib.search.trees.ExceptionPathSolution;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.search.trees.Solution;

import java.util.BitSet;
import java.util.concurrent.atomic.AtomicLong;

public class TestCase {
    private static final AtomicLong testCaseNumbers = new AtomicLong(0L);
    private final long testCaseNumber;
    protected final boolean exceptional;
    protected final Solution solution;
    protected final BitSet cover;

    public TestCase(boolean exceptional, Solution solution, BitSet cover) {
        this.testCaseNumber = testCaseNumbers.getAndIncrement();
        this.exceptional = exceptional;
        this.solution = solution;
        this.cover = cover;
    }

    public long getTestCaseNumber() {
        return testCaseNumber;
    }

    public Solution getSolution() {
        return solution;
    }

    public BitSet getCover() {
        return cover;
    }

    public boolean isExceptional() {
        return exceptional;
    }

    public Object getReturnValue() {
        return solution.returnValue;
    }

    public Object[] getInputs() {
        return solution.labels.getLabels();
    }

    public static TestCase fromPathSolution(PathSolution ps, BitSet cover) {
        return new TestCase(ps instanceof ExceptionPathSolution, ps.getSolution(), cover);
    }

    public static TestCase fromSolution(boolean isExceptional, Solution s, BitSet cover) {
        return new TestCase(isExceptional, s, cover);
    }
}
