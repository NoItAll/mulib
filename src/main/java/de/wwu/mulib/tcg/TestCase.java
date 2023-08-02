package de.wwu.mulib.tcg;

import de.wwu.mulib.search.trees.ExceptionPathSolution;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.search.trees.Solution;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;

public class TestCase {
    private static final AtomicLong testCaseNumbers = new AtomicLong(0L);
    private final long testCaseNumber;
    protected final boolean exceptional;
    protected final Solution solution;
    protected final BitSet cover;
    protected final Object[] inputsInArgOrder;
    protected final Object[] inputsInArgOrderPostExecution;
    public TestCase(boolean exceptional, Solution solution, BitSet cover) {
        this.testCaseNumber = testCaseNumbers.getAndIncrement();
        this.exceptional = exceptional;
        this.solution = solution;
        this.cover = cover;
        Map<Integer, Object> inputsInArgOrder = new HashMap<>();
        Map<Integer, Object> inputsInArgOrderPostExecution = new HashMap<>();
        for (Map.Entry<String, Object> entry : solution.labels.getIdToLabel().entrySet()) {
            Matcher m = TcgUtility.INPUT_ARGUMENT_NAME_PATTERN.matcher(entry.getKey());
            if (m.matches()) {
                String number = m.group(1);
                int i = Integer.parseInt(number);
                inputsInArgOrder.put(i, entry.getValue());
            } else {
                m = TcgUtility.INPUT_OBJECT_ARGUMENT_POST_STATE_PATTERN.matcher(entry.getKey());
                if (m.matches()) {
                    String number = m.group(1);
                    int i = Integer.parseInt(number);
                    inputsInArgOrderPostExecution.put(i, entry.getValue());
                }
            }
        }
        this.inputsInArgOrder = new Object[inputsInArgOrder.size()];
        this.inputsInArgOrderPostExecution = new Object[inputsInArgOrderPostExecution.size()];
        for (int i = 0; i < this.inputsInArgOrder.length; i++) {
            assert inputsInArgOrder.containsKey(i);
            Object val = inputsInArgOrder.get(i);
            this.inputsInArgOrder[i] = val;
        }
        int j = 0;
        for (int i = 0; i < this.inputsInArgOrderPostExecution.length; i++) {
            if (!inputsInArgOrderPostExecution.containsKey(i)) {
                continue;
            }
            Object val = inputsInArgOrder.get(i);
            this.inputsInArgOrderPostExecution[j] = val;
            j++;
        }
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
        return inputsInArgOrder;
    }

    public Object[] getPostExecutionStateInputs() {
        return inputsInArgOrderPostExecution;
    }

    public static TestCase fromPathSolution(PathSolution ps, BitSet cover) {
        return new TestCase(ps instanceof ExceptionPathSolution, ps.getSolution(), cover);
    }

    public static TestCase fromSolution(boolean isExceptional, Solution s, BitSet cover) {
        return new TestCase(isExceptional, s, cover);
    }
}
