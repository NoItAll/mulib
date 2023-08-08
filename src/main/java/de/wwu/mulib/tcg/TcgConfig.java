package de.wwu.mulib.tcg;

import de.wwu.mulib.tcg.testsetreducer.NullTestSetReducer;
import de.wwu.mulib.tcg.testsetreducer.TestSetReducer;
import de.wwu.mulib.tcg.testsetsorter.NullTestSetSorter;
import de.wwu.mulib.tcg.testsetsorter.TestSetSorter;

import java.io.PrintStream;
import java.util.Arrays;

public class TcgConfig {
    public final TestSetReducer TEST_SET_REDUCER;
    public final TestSetSorter TEST_SET_SORTER;
    public final String INDENT;
    public final double MAX_FP_DELTA;
    public final PrintStream PRINT_STREAM;
    public final boolean ASSUME_SETTERS;
    public final boolean ASSUME_GETTERS;
    public final boolean ASSUME_EQUALS_METHODS;
    public final boolean GENERATE_POST_STATE_CHECKS_FOR_OBJECTS_IF_SPECIFIED;

    public final boolean DACITE_TCG;
    public final Class<?>[] SPECIAL_CASES;

    public static TcgConfigBuilder builder() {
        return new TcgConfigBuilder();
    }

    public static class TcgConfigBuilder {
        private TestSetReducer testSetReducer;
        private TestSetSorter testSetSorter;
        private String indent;
        private double maxFpDelta;
        private PrintStream printStream;
        private boolean assumeSetters;
        private boolean assumeGetters;
        private boolean assumeEqualsMethods;
        private boolean generatePostStateChecksForObjectsIfSpecified;

        private boolean daciteTcg;
        private Class<?>[] specialCases;

        TcgConfigBuilder() {
            testSetReducer = new NullTestSetReducer();
            testSetSorter = new NullTestSetSorter();
            indent = "    ";
            maxFpDelta = 1e-8;
            printStream = null;
            assumeSetters = true;
            assumeGetters = true;
            assumeEqualsMethods = true;
            generatePostStateChecksForObjectsIfSpecified = true;
            specialCases = new Class[0];
            daciteTcg = false;

        }

        public TcgConfigBuilder setTestSetReducer(TestSetReducer testSetReducer) {
            this.testSetReducer = testSetReducer;
            return this;
        }

        public TcgConfigBuilder setTestSetSorter(TestSetSorter testSetSorter) {
            this.testSetSorter = testSetSorter;
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

        public TcgConfigBuilder setDaciteTcg(boolean daciteTcg){
            this.daciteTcg = daciteTcg;
            return this;
        }

        public TcgConfig build() {
            return new TcgConfig(
                    testSetReducer,
                    testSetSorter,
                    indent,
                    maxFpDelta,
                    printStream,
                    assumeSetters,
                    assumeGetters,
                    assumeEqualsMethods,
                    generatePostStateChecksForObjectsIfSpecified,
                    specialCases,
                    daciteTcg
            );
        }
    }

    private TcgConfig(
            TestSetReducer TEST_SET_REDUCER, TestSetSorter TEST_SET_SORTER, String INDENT, double MAX_FP_DELTA,
            PrintStream PRINT_STREAM, boolean ASSUME_SETTERS, boolean ASSUME_GETTERS, boolean ASSUME_EQUALS_METHODS,
            boolean GENERATE_POST_STATE_CHECKS_FOR_OBJECTS_IF_SPECIFIED, Class<?>[] SPECIAL_CASES, boolean DACITE_TCG) {
        this.TEST_SET_REDUCER = TEST_SET_REDUCER;
        this.TEST_SET_SORTER = TEST_SET_SORTER;
        this.INDENT = INDENT;
        this.MAX_FP_DELTA = MAX_FP_DELTA;
        this.PRINT_STREAM = PRINT_STREAM;
        this.ASSUME_SETTERS = ASSUME_SETTERS;
        this.ASSUME_GETTERS = ASSUME_GETTERS;
        this.ASSUME_EQUALS_METHODS = ASSUME_EQUALS_METHODS;
        this.GENERATE_POST_STATE_CHECKS_FOR_OBJECTS_IF_SPECIFIED = GENERATE_POST_STATE_CHECKS_FOR_OBJECTS_IF_SPECIFIED;
        this.SPECIAL_CASES = SPECIAL_CASES;
        this.DACITE_TCG = DACITE_TCG;
    }

    @Override
    public String toString() {
        return String.format("TcgConfig{reducer=%s, sorter=%s, indent=\"%s\", maxFpDelta=%s, assumeSetters=%s, assumeGetters=%s, assumeEqualsMethods=%s, generatePostExecutionStateChecksForObjectsIfSpecified=%s, specialCases=%s}",
                TEST_SET_REDUCER, TEST_SET_SORTER, INDENT, MAX_FP_DELTA, ASSUME_SETTERS, ASSUME_GETTERS, ASSUME_EQUALS_METHODS, GENERATE_POST_STATE_CHECKS_FOR_OBJECTS_IF_SPECIFIED, Arrays.toString(SPECIAL_CASES));
    }
}
