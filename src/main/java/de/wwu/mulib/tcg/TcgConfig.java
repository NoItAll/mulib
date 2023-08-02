package de.wwu.mulib.tcg;

import de.wwu.mulib.tcg.testsetreducer.NullTestSetReducer;
import de.wwu.mulib.tcg.testsetreducer.TestSetReducer;
import de.wwu.mulib.tcg.testsetsorter.NullTestSetSorter;
import de.wwu.mulib.tcg.testsetsorter.TestSetSorter;

import java.io.PrintStream;

public class TcgConfig {
    public final TestSetReducer TEST_SET_REDUCER;
    public final TestSetSorter TEST_SET_SORTER;
    public final String INDENT;
    public final double MAX_FP_DELTA;
    public final PrintStream PRINT_STREAM;
    public final boolean ASSUME_SETTERS;
    public final boolean GENERATE_POST_STATE_CHECKS_FOR_OBJECTS_IF_SPECIFIED;
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
        private boolean generatePostStateChecksForObjectsIfSpecified;
        private Class<?>[] specialCases;

        TcgConfigBuilder() {
            testSetReducer = new NullTestSetReducer();
            testSetSorter = new NullTestSetSorter();
            indent = "    ";
            maxFpDelta = 1e-8;
            printStream = null;
            assumeSetters = true;
            this.generatePostStateChecksForObjectsIfSpecified = true;
            this.specialCases = new Class[0];
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

        public TcgConfigBuilder setGeneratePostStateChecksForObjectsIfSpecified(boolean generatePostStateChecksForObjectsIfSpecified) {
            this.generatePostStateChecksForObjectsIfSpecified = generatePostStateChecksForObjectsIfSpecified;
            return this;
        }

        public TcgConfigBuilder setSpecialCases(Class<?>[] specialCases) {
            this.specialCases = specialCases;
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
                    generatePostStateChecksForObjectsIfSpecified,
                    specialCases
            );
        }
    }

    private TcgConfig(
            TestSetReducer TEST_SET_REDUCER, TestSetSorter TEST_SET_SORTER, String INDENT, double MAX_FP_DELTA,
            PrintStream PRINT_STREAM, boolean ASSUME_SETTERS, boolean GENERATE_POST_STATE_CHECKS_FOR_OBJECTS_IF_SPECIFIED,
            Class<?>[] SPECIAL_CASES) {
        this.TEST_SET_REDUCER = TEST_SET_REDUCER;
        this.TEST_SET_SORTER = TEST_SET_SORTER;
        this.INDENT = INDENT;
        this.MAX_FP_DELTA = MAX_FP_DELTA;
        this.PRINT_STREAM = PRINT_STREAM;
        this.ASSUME_SETTERS = ASSUME_SETTERS;
        this.GENERATE_POST_STATE_CHECKS_FOR_OBJECTS_IF_SPECIFIED = GENERATE_POST_STATE_CHECKS_FOR_OBJECTS_IF_SPECIFIED;
        this.SPECIAL_CASES = SPECIAL_CASES;
    }
}
