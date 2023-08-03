package de.wwu.mulib.tcg;

import de.wwu.mulib.tcg.testclassgenerator.Junit5_8TestClassGenerator;
import de.wwu.mulib.tcg.testclassgenerator.TestClassGenerator;
import de.wwu.mulib.tcg.testmethodgenerator.Junit5_8TestMethodGenerator;
import de.wwu.mulib.tcg.testmethodgenerator.TestMethodGenerator;
import de.wwu.mulib.tcg.testsetreducer.TestSetReducer;
import de.wwu.mulib.tcg.testsetsorter.TestSetSorter;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TestCasesStringGenerator {
    protected final TestClassGenerator testClassGenerator;
    protected final TestMethodGenerator testMethodGenerator;
    protected TestSetReducer testSetReducer;
    protected TestSetSorter testSetSorter;
    protected final TestCases testCases;
    protected final PrintStream printer;

    public TestCasesStringGenerator(
            TestCases testCases,
            TcgConfig tcgConfig) {
        this(
                new Junit5_8TestClassGenerator(tcgConfig),
                new Junit5_8TestMethodGenerator(
                        testCases.getTestedMethod(),
                        tcgConfig
                ),
                tcgConfig.TEST_SET_REDUCER,
                tcgConfig.TEST_SET_SORTER,
                testCases,
                tcgConfig.PRINT_STREAM
        );
    }

    protected TestCasesStringGenerator(
            TestClassGenerator testClassGenerator,
            TestMethodGenerator testMethodGenerator,
            TestSetReducer testSetReducer,
            TestSetSorter testSetSorter,
            TestCases testCases,
            PrintStream printer) {
        this.testClassGenerator = testClassGenerator;
        this.testMethodGenerator = testMethodGenerator;
        this.testSetReducer = testSetReducer;
        this.testSetSorter = testSetSorter;
        this.testCases = testCases;
        this.printer = printer;
    }

    public String generateTestClassStringRepresentation() {
        Collection<TestCase> tests = reduceTestCases(testCases.getTestCases());
        List<TestCase> sortedTests = sortTestCases(tests);
        List<StringBuilder> stringsForTests = generateStringRepresentation(sortedTests);

        String result = testClassGenerator.generateTestClassString(
                testCases.getPackageNameOfClassOfTestedMethod(),
                testCases.getNameOfTestedClass(),
                testMethodGenerator.getEncounteredTypes(),
                testCases.getNumberTestCases(),
                tests.size(),
                stringsForTests
        );
        if (printer != null) {
            printer.println(result);
        }
        return result;
    }

    protected List<StringBuilder> generateStringRepresentation(List<TestCase> testCases) {
        ArrayList<StringBuilder> stringsForTests = new ArrayList<>();
        for (TestCase tc : testCases) {
            stringsForTests.add(testMethodGenerator.generateTestCaseRepresentation(tc));
        }
        return stringsForTests;
    }

    protected Collection<TestCase> reduceTestCases(List<TestCase> testCases) {
        return testSetReducer.apply(testCases);
    }

    protected List<TestCase> sortTestCases(Collection<TestCase> testCases) {
        return testSetSorter.apply(testCases);
    }

    public void setTestSetSorter(TestSetSorter testSetSorter) {
        this.testSetSorter = testSetSorter;
    }

    public void setTestSetReducer(TestSetReducer reducer){
        this.testSetReducer = reducer;
    }
}
