package de.wwu.mulib.tcg;

import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.tcg.testsetreducer.NullTestSetReducer;
import de.wwu.mulib.tcg.testsetreducer.TestSetReducer;
import de.wwu.mulib.tcg.testsetsorter.NullTestSetSorter;
import de.wwu.mulib.tcg.testsetsorter.TestSetSorter;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Optional;

public class TcgConfig {
    public final TestSetSorter PRE_REDUCE_TEST_SET_SORTER;
    public final TestSetReducer TEST_SET_REDUCER;
    public final TestSetSorter POST_REDUCE_TEST_SET_SORTER;
    public final String INDENT;
    public final double MAX_FP_DELTA;
    public final PrintStream PRINT_STREAM;
    public final boolean ASSUME_PUBLIC_ZERO_ARGS_CONSTRUCTOR;
    public final boolean ASSUME_SETTERS;
    public final boolean ASSUME_GETTERS;
    public final boolean ASSUME_EQUALS_METHODS;
    public final boolean GENERATE_POST_STATE_CHECKS_FOR_OBJECTS_IF_SPECIFIED;
    public final Optional<String> TEST_CLASS_POSTFIX;
    public final Class<?>[] SPECIAL_CASES;

    public static TcgConfigBuilder builder() {
        return new TcgConfigBuilder();
    }

    public static class TcgConfigBuilder {
        private TestSetSorter preReduceTestSetSorter;
        private TestSetReducer testSetReducer;
        private TestSetSorter postReduceTestSetSorter;
        private String indent;
        private double maxFpDelta;
        private PrintStream printStream;
        private boolean assumePublicZeroArgsConstructor;
        private boolean assumeSetters;
        private boolean assumeGetters;
        private boolean assumeEqualsMethods;
        private boolean generatePostStateChecksForObjectsIfSpecified;
        private Optional<String> testClassPostfix;
        private Class<?>[] specialCases;

        TcgConfigBuilder() {
            preReduceTestSetSorter = new NullTestSetSorter();
            testSetReducer = new NullTestSetReducer();
            postReduceTestSetSorter = new NullTestSetSorter();
            indent = "    ";
            maxFpDelta = 1e-8;
            printStream = null;
            assumeSetters = true;
            assumeGetters = true;
            assumePublicZeroArgsConstructor = true;
            assumeEqualsMethods = true;
            generatePostStateChecksForObjectsIfSpecified = true;
            specialCases = new Class[0];
            testClassPostfix = Optional.empty();
        }

        public TcgConfigBuilder setTestSetReducer(TestSetReducer testSetReducer) {
            this.testSetReducer = testSetReducer;
            return this;
        }

        public TcgConfigBuilder setPostReduceTestSetSorter(TestSetSorter postReduceTestSetSorter) {
            this.postReduceTestSetSorter = postReduceTestSetSorter;
            return this;
        }

        public TcgConfigBuilder setIndent(String indent) {
            this.indent = indent;
            return this;
        }

        public TcgConfigBuilder setMaxFpDelta(double maxFpDelta) {
            this.maxFpDelta = maxFpDelta;
            return this;
        }

        public TcgConfigBuilder setPrintStream(PrintStream printStream) {
            this.printStream = printStream;
            return this;
        }

        public TcgConfigBuilder setAssumeSetters(boolean assumeSetters) {
            this.assumeSetters = assumeSetters;
            return this;
        }

        public TcgConfigBuilder setAssumeGetters(boolean assumeGetters) {
            this.assumeGetters = assumeGetters;
            return this;
        }

        public TcgConfigBuilder setAssumeEqualsMethods(boolean assumeEqualsMethods) {
            this.assumeEqualsMethods = assumeEqualsMethods;
            return this;
        }

        public TcgConfigBuilder setGeneratePostStateChecksForObjectsIfSpecified(boolean generatePostStateChecksForObjectsIfSpecified) {
            this.generatePostStateChecksForObjectsIfSpecified = generatePostStateChecksForObjectsIfSpecified;
            return this;
        }

        public TcgConfigBuilder setSpecialCases(Class<?>[] specialCases) {
            this.specialCases = specialCases;
            return this;
        }

        public TcgConfigBuilder setAssumePublicZeroArgsConstructor(boolean assumePublicZeroArgsConstructor) {
            this.assumePublicZeroArgsConstructor = assumePublicZeroArgsConstructor;
            return this;
        }

        public TcgConfigBuilder setTestClassPostfix(String testClassPostfix) {
            if (testClassPostfix == null) {
                throw new MulibRuntimeException("Must not add null");
            }
            this.testClassPostfix = Optional.of(testClassPostfix);
            return this;
        }

        public TcgConfigBuilder setPreReduceTestSetSorter(TestSetSorter preReduceTestSetSorter) {
            this.preReduceTestSetSorter = preReduceTestSetSorter;
            return this;
        }

        public TcgConfig build() {
            return new TcgConfig(
                    preReduceTestSetSorter,
                    testSetReducer,
                    postReduceTestSetSorter,
                    indent,
                    maxFpDelta,
                    printStream,
                    assumePublicZeroArgsConstructor,
                    assumeSetters,
                    assumeGetters,
                    assumeEqualsMethods,
                    generatePostStateChecksForObjectsIfSpecified,
                    specialCases,
                    testClassPostfix
            );
        }
    }

    private TcgConfig(
            TestSetSorter PRE_REDUCE_TEST_SET_SORTER,
            TestSetReducer TEST_SET_REDUCER, TestSetSorter POST_REDUCE_TEST_SET_SORTER, String INDENT, double MAX_FP_DELTA,
            PrintStream PRINT_STREAM, boolean ASSUME_PUBLIC_ZERO_ARGS_CONSTRUCTOR,
            boolean ASSUME_SETTERS, boolean ASSUME_GETTERS, boolean ASSUME_EQUALS_METHODS,
            boolean GENERATE_POST_STATE_CHECKS_FOR_OBJECTS_IF_SPECIFIED, Class<?>[] SPECIAL_CASES, Optional<String> TEST_CLASS_POSTFIX) {
        this.PRE_REDUCE_TEST_SET_SORTER = PRE_REDUCE_TEST_SET_SORTER;
        this.TEST_SET_REDUCER = TEST_SET_REDUCER;
        this.POST_REDUCE_TEST_SET_SORTER = POST_REDUCE_TEST_SET_SORTER;
        this.INDENT = INDENT;
        this.MAX_FP_DELTA = MAX_FP_DELTA;
        this.PRINT_STREAM = PRINT_STREAM;
        this.ASSUME_PUBLIC_ZERO_ARGS_CONSTRUCTOR = ASSUME_PUBLIC_ZERO_ARGS_CONSTRUCTOR;
        this.ASSUME_SETTERS = ASSUME_SETTERS;
        this.ASSUME_GETTERS = ASSUME_GETTERS;
        this.ASSUME_EQUALS_METHODS = ASSUME_EQUALS_METHODS;
        this.GENERATE_POST_STATE_CHECKS_FOR_OBJECTS_IF_SPECIFIED = GENERATE_POST_STATE_CHECKS_FOR_OBJECTS_IF_SPECIFIED;
        this.SPECIAL_CASES = SPECIAL_CASES;
        this.TEST_CLASS_POSTFIX = TEST_CLASS_POSTFIX;
    }

    @Override
    public String toString() {
        return String.format("TcgConfig{preReduceSorter=%s, reducer=%s, postReduceSorter=%s, indent=\"%s\", maxFpDelta=%s, assumePublicZeroArgsConstructor=%s, assumeSetters=%s, assumeGetters=%s, assumeEqualsMethods=%s, generatePostExecutionStateChecksForObjectsIfSpecified=%s, specialCases=%s}",
                PRE_REDUCE_TEST_SET_SORTER, TEST_SET_REDUCER, POST_REDUCE_TEST_SET_SORTER, INDENT, MAX_FP_DELTA, ASSUME_PUBLIC_ZERO_ARGS_CONSTRUCTOR, ASSUME_SETTERS, ASSUME_GETTERS, ASSUME_EQUALS_METHODS, GENERATE_POST_STATE_CHECKS_FOR_OBJECTS_IF_SPECIFIED, Arrays.toString(SPECIAL_CASES));
    }

    public String[] configStrings() {
        return toString()
                .replace("TcgConfig{", "")
                .replace("}", "")
                .split(", ");
    }
}
