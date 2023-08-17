package de.wwu.mulib.tcg;

import java.util.BitSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;

/**
 * Represents a test case with original inputs and, optionally, the state of the inputs after executing the method
 */
public class TestCase {
    private static final AtomicLong testCaseNumbers = new AtomicLong(0L);
    private final long testCaseNumber;
    /**
     * Whether the return value is a thrown {@link Throwable}
     */
    protected final boolean exceptional;
    /**
     * Each bit represents, e.g., a bytecode instruction, a branch, or a def-use chain that is covered by this
     * test case.
     */
    protected final BitSet cover;
    /**
     * The arguments in the order they are added to the method call of the method under test
     */
    protected final Object[] inputsInArgOrder;
    /**
     * The state of the arguments after executing the method under test
     */
    protected final Object[] inputsInArgOrderPostExecution;
    /**
     * The return value of the method under test
     */
    protected final Object returnValue;
    /**
     * Maps the inputs after the execution to the original inputs
     */
    protected final IdentityHashMap<Object, Object> inputsPostExecutionToInputs;

    /**
     * @param exceptional Whether the test case has a thrown {@link Throwable} as a result value
     * @param labelNameToLabel A map of pairs (either argX or argXPostExec, input object). The values are the input
     *                         arguments to the method under test if the key follows the pattern {@link TcgUtility#INPUT_ARGUMENT_NAME_PATTERN}.
     *                         If the key follows the pattern {@link TcgUtility#INPUT_OBJECT_ARGUMENT_POST_STATE_PATTERN}, the value is
     *                         expected to hold argX AFTER executing the method under test. In this case, the value must be an object.
     *                         Other key-names are ignored.
     *                         The position of the input in the call to the method under test is determined by the X.
     * @param returnValue The value returned by the method under test. If 'exceptional' is true, 'returnValue' must be a {@link Throwable}.
     * @param cover Each bit represents, e.g., a bytecode instruction, a branch, or a def-use chain that is covered by
     *              this test case. It is allowed to have empty bit sets here, yet, {@link de.wwu.mulib.tcg.testsetreducer.TestSetReducer}s
     *              should not be used, since then, all test cases are discarded.
     * @param config The configuration
     */
    public TestCase(boolean exceptional, Map<String, Object> labelNameToLabel, Object returnValue, BitSet cover, TcgConfig config) {
        this.testCaseNumber = testCaseNumbers.getAndIncrement();
        this.exceptional = exceptional;
        this.returnValue = returnValue;
        this.cover = cover;
        Map<Integer, Object> inputsInArgOrder = new HashMap<>();
        Map<Integer, Object> inputsInArgOrderAfterExecution = new HashMap<>();
        for (Map.Entry<String, Object> entry : labelNameToLabel.entrySet()) {
            Matcher m = TcgUtility.INPUT_ARGUMENT_NAME_PATTERN.matcher(entry.getKey());
            if (m.matches()) {
                String number = m.group(1);
                int i = Integer.parseInt(number);
                inputsInArgOrder.put(i, entry.getValue());
            } else if (config.GENERATE_POST_STATE_CHECKS_FOR_OBJECTS_IF_SPECIFIED) {
                m = TcgUtility.INPUT_OBJECT_ARGUMENT_POST_STATE_PATTERN.matcher(entry.getKey());
                if (m.matches()) {
                    String number = m.group(1);
                    int i = Integer.parseInt(number);
                    inputsInArgOrderAfterExecution.put(i, entry.getValue());
                }
            }
        }

        this.inputsPostExecutionToInputs = new IdentityHashMap<>();
        for (Map.Entry<Integer, Object> entry : inputsInArgOrderAfterExecution.entrySet()) {
            Object inputPreExecution = inputsInArgOrder.get(entry.getKey());
            assert (inputPreExecution == null) == (entry.getValue() == null) : "There must be a input pre execution if there is an input post execution";
            inputsPostExecutionToInputs.put(entry.getValue(), inputPreExecution);
        }

        this.inputsInArgOrder = new Object[inputsInArgOrder.size()];
        this.inputsInArgOrderPostExecution = new Object[inputsInArgOrderAfterExecution.size()];
        for (int i = 0; i < this.inputsInArgOrder.length; i++) {
            assert inputsInArgOrder.containsKey(i);
            Object val = inputsInArgOrder.get(i);
            this.inputsInArgOrder[i] = val;
        }
        if (config.GENERATE_POST_STATE_CHECKS_FOR_OBJECTS_IF_SPECIFIED) {
            int j = 0;
            for (int i = 0; i < this.inputsInArgOrderPostExecution.length; i++) {
                if (!inputsInArgOrderAfterExecution.containsKey(i)) {
                    continue;
                }
                Object val = inputsInArgOrderAfterExecution.get(i);
                this.inputsInArgOrderPostExecution[j] = val;
                j++;
            }
        }
    }

    /**
     * @return A distinct identifier for this test case
     */
    public long getTestCaseNumber() {
        return testCaseNumber;
    }

    /**
     * @return The coverage information for this test case
     */
    public BitSet getCover() {
        return cover;
    }

    /**
     * @return Whether the method under test was left due to a thrown {@link Throwable}
     */
    public boolean isExceptional() {
        return exceptional;
    }

    /**
     * @return The return value. In case {@link TestCase#isExceptional()} is true, a {@link Throwable} is returned.
     */
    public Object getReturnValue() {
        return returnValue;
    }

    /**
     * @return A representation of the specified inputs to the method under test in the order they are fed into it
     */
    public Object[] getInputs() {
        return inputsInArgOrder;
    }

    /**
     * @return A representation of the specified object inputs for the method under test after executing it
     */
    public Object[] getInputsAfterExecution() {
        return inputsInArgOrderPostExecution;
    }

    /**
     * @param inputPostExecution An element of the array returned by {@link TestCase#getInputsAfterExecution()}
     * @return An object of {@link TestCase#getInputs()}
     */
    public Object getInputPreExecutionForInputAfterExecution(Object inputPostExecution) {
        return inputsPostExecutionToInputs.get(inputPostExecution);
    }
}
