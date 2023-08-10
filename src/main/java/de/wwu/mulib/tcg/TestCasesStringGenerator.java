package de.wwu.mulib.tcg;

import de.wwu.mulib.tcg.testclassgenerator.JunitJupiterTestClassGenerator;
import de.wwu.mulib.tcg.testclassgenerator.TestClassGenerator;
import de.wwu.mulib.tcg.testmethodgenerator.JunitJupiterTestMethodGenerator;
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
    protected final TestSetSorter preReduceTestSetSorter;
    protected final TestSetReducer testSetReducer;
    protected final TestSetSorter postReduceTestSetSorter;
    protected final TestCases testCases;
    protected final PrintStream printer;

    public TestCasesStringGenerator(
            TestCases testCases,
            TcgConfig tcgConfig) {
        this(
                new JunitJupiterTestClassGenerator(tcgConfig),
                new JunitJupiterTestMethodGenerator(
                        testCases.getTestedMethod(),
                        tcgConfig
                ),
                tcgConfig.PRE_REDUCE_TEST_SET_SORTER,
                tcgConfig.TEST_SET_REDUCER,
                tcgConfig.POST_REDUCE_TEST_SET_SORTER,
                testCases,
                tcgConfig.PRINT_STREAM
        );
    }

    protected TestCasesStringGenerator(
            TestClassGenerator testClassGenerator,
            TestMethodGenerator testMethodGenerator,
            TestSetSorter preReduceTestSetSorter,
            TestSetReducer testSetReducer,
            TestSetSorter postReduceTestSetSorter,
            TestCases testCases,
            PrintStream printer) {
        this.testClassGenerator = testClassGenerator;
        this.testMethodGenerator = testMethodGenerator;
        this.preReduceTestSetSorter = preReduceTestSetSorter;
        this.testSetReducer = testSetReducer;
        this.postReduceTestSetSorter = postReduceTestSetSorter;
        this.testCases = testCases;
        this.printer = printer;
    }

    public String generateTestClassStringRepresentation() {
        List<TestCase> preSortedTestCases = preSortTestCases(testCases.getTestCases());
        Collection<TestCase> tests = reduceTestCases(preSortedTestCases);
        List<TestCase> sortedTests = sortReducedTestCases(tests);
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

    protected List<TestCase> preSortTestCases(Collection<TestCase> testCases) {
        return preReduceTestSetSorter.apply(testCases);
    }

    protected List<TestCase> sortReducedTestCases(Collection<TestCase> testCases) {
        return postReduceTestSetSorter.apply(testCases);
    }
}
