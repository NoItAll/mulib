package de.wwu.mulib.tcg;

import java.util.BitSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;

public class TestCase {
    private static final AtomicLong testCaseNumbers = new AtomicLong(0L);
    private final long testCaseNumber;
    protected final boolean exceptional;
    protected final BitSet cover;
    protected final Object[] inputsInArgOrder;
    protected final Object[] inputsInArgOrderPostExecution;
    protected final Object returnValue;
    protected final IdentityHashMap<Object, Object> inputsPostExecutionToInputs;
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

    public long getTestCaseNumber() {
        return testCaseNumber;
    }


    public BitSet getCover() {
        return cover;
    }

    public boolean isExceptional() {
        return exceptional;
    }

    public Object getReturnValue() {
        return returnValue;
    }

    public Object[] getInputs() {
        return inputsInArgOrder;
    }

    public Object[] getInputsAfterExecution() {
        return inputsInArgOrderPostExecution;
    }

    public Object getInputPreExecutionForInputAfterExecution(Object inputPostExecution) {
        return inputsPostExecutionToInputs.get(inputPostExecution);
    }
}
