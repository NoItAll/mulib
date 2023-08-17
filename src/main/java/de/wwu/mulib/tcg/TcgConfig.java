package de.wwu.mulib.tcg;

import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.tcg.testsetreducer.NullTestSetReducer;
import de.wwu.mulib.tcg.testsetreducer.TestSetReducer;
import de.wwu.mulib.tcg.testsetsorter.NullTestSetSorter;
import de.wwu.mulib.tcg.testsetsorter.TestSetSorter;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Optional;

/**
 * Comprises configuration options for test case generation.
 * Is created using the builder pattern
 * @see TcgConfigBuilder
 */
public class TcgConfig {
    /**
     * The sorter used for sorting before reducing the test cases
     */
    public final TestSetSorter PRE_REDUCE_TEST_SET_SORTER;
    /**
     * The reducer
     */
    public final TestSetReducer TEST_SET_REDUCER;
    /**
     * The sorter for sorting the test cases after reducing
     */
    public final TestSetSorter POST_REDUCE_TEST_SET_SORTER;
    /**
     * The amount of indentation used for the generated test cases
     */
    public final String INDENT;
    /**
     * The maximum delta that is allowed to consider two floating point numbers equal
     */
    public final double MAX_FP_DELTA;
    /**
     * The print stream used for printing the result. Can be null
     */
    public final PrintStream PRINT_STREAM;
    /**
     * true, if zero-args constructors are assume to be available, else false and reflection is used
     */
    public final boolean ASSUME_PUBLIC_ZERO_ARGS_CONSTRUCTORS;
    /**
     * true, if setters are assumed to be available, else false and reflection is used
     */
    public final boolean ASSUME_SETTERS;
    /**
     * true, if getters are assumed to be available, else false and reflection is used
     */
    public final boolean ASSUME_GETTERS;
    /**
     * true, if equals-methods are assumed to be available, else false and reflection is used
     */
    public final boolean ASSUME_EQUALS_METHODS;
    /**
     * true, if the state of input objects after executing the method under test should be checked
     */
    public final boolean GENERATE_POST_STATE_CHECKS_FOR_OBJECTS_IF_SPECIFIED;
    /**
     * A postfix that can optionally be added to the generated test class (if any)
     */
    public final Optional<String> TEST_CLASS_POSTFIX;
    /**
     * Special cases for which a custom initialization behavior is used by providing a subclass of
     * {@link de.wwu.mulib.tcg.testmethodgenerator.TestMethodGenerator}.
     */
    public final Class<?>[] SPECIAL_CASES;

    /**
     * @return A builder
     */
    public static TcgConfigBuilder builder() {
        return new TcgConfigBuilder();
    }

    /**
     * A builder for {@link TcgConfig}
     */
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

        /**
         * @see TcgConfig#TEST_SET_REDUCER
         */
        public TcgConfigBuilder setTestSetReducer(TestSetReducer testSetReducer) {
            this.testSetReducer = testSetReducer;
            return this;
        }

        /**
         * @see TcgConfig#POST_REDUCE_TEST_SET_SORTER
         */
        public TcgConfigBuilder setPostReduceTestSetSorter(TestSetSorter postReduceTestSetSorter) {
            this.postReduceTestSetSorter = postReduceTestSetSorter;
            return this;
        }

        /**
         * @see TcgConfig#INDENT
         */
        public TcgConfigBuilder setIndent(String indent) {
            this.indent = indent;
            return this;
        }

        /**
         * @see TcgConfig#MAX_FP_DELTA
         */
        public TcgConfigBuilder setMaxFpDelta(double maxFpDelta) {
            this.maxFpDelta = maxFpDelta;
            return this;
        }

        /**
         * @see TcgConfig#PRINT_STREAM
         */
        public TcgConfigBuilder setPrintStream(PrintStream printStream) {
            this.printStream = printStream;
            return this;
        }

        /**
         * @see TcgConfig#ASSUME_SETTERS
         */
        public TcgConfigBuilder setAssumeSetters(boolean assumeSetters) {
            this.assumeSetters = assumeSetters;
            return this;
        }

        /**
         * @see TcgConfig#ASSUME_GETTERS
         */
        public TcgConfigBuilder setAssumeGetters(boolean assumeGetters) {
            this.assumeGetters = assumeGetters;
            return this;
        }

        /**
         * @see TcgConfig#ASSUME_EQUALS_METHODS
         */
        public TcgConfigBuilder setAssumeEqualsMethods(boolean assumeEqualsMethods) {
            this.assumeEqualsMethods = assumeEqualsMethods;
            return this;
        }

        /**
         * @see TcgConfig#GENERATE_POST_STATE_CHECKS_FOR_OBJECTS_IF_SPECIFIED
         */
        public TcgConfigBuilder setGeneratePostStateChecksForObjectsIfSpecified(boolean generatePostStateChecksForObjectsIfSpecified) {
            this.generatePostStateChecksForObjectsIfSpecified = generatePostStateChecksForObjectsIfSpecified;
            return this;
        }

        /**
         * @see TcgConfig#SPECIAL_CASES
         */
        public TcgConfigBuilder setSpecialCases(Class<?>[] specialCases) {
            this.specialCases = specialCases;
            return this;
        }

        /**
         * @see TcgConfig#ASSUME_PUBLIC_ZERO_ARGS_CONSTRUCTORS
         */
        public TcgConfigBuilder setAssumePublicZeroArgsConstructor(boolean assumePublicZeroArgsConstructor) {
            this.assumePublicZeroArgsConstructor = assumePublicZeroArgsConstructor;
            return this;
        }

        /**
         * @see TcgConfig#TEST_CLASS_POSTFIX
         */
        public TcgConfigBuilder setTestClassPostfix(String testClassPostfix) {
            if (testClassPostfix == null) {
                throw new MulibRuntimeException("Must not add null");
            }
            this.testClassPostfix = Optional.of(testClassPostfix);
            return this;
        }

        /**
         * @see TcgConfig#PRE_REDUCE_TEST_SET_SORTER
         */
        public TcgConfigBuilder setPreReduceTestSetSorter(TestSetSorter preReduceTestSetSorter) {
            this.preReduceTestSetSorter = preReduceTestSetSorter;
            return this;
        }

        /**
         * @return The configuration
         */
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
        this.ASSUME_PUBLIC_ZERO_ARGS_CONSTRUCTORS = ASSUME_PUBLIC_ZERO_ARGS_CONSTRUCTOR;
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
                PRE_REDUCE_TEST_SET_SORTER, TEST_SET_REDUCER, POST_REDUCE_TEST_SET_SORTER, INDENT, MAX_FP_DELTA, ASSUME_PUBLIC_ZERO_ARGS_CONSTRUCTORS, ASSUME_SETTERS, ASSUME_GETTERS, ASSUME_EQUALS_METHODS, GENERATE_POST_STATE_CHECKS_FOR_OBJECTS_IF_SPECIFIED, Arrays.toString(SPECIAL_CASES));
    }

    /**
     * @return An informative String array where each element is a configuration option
     */
    public String[] configStrings() {
        return toString()
                .replace("TcgConfig{", "")
                .replace("}", "")
                .split(", ");
    }
}
