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

/**
 * Class generating a Junit-Jupiter String representation of test cases.
 * Has 6 stages:
 * 1. Sorts lists of {@link TestCase}s according to some {@link TestSetSorter}. This can have some impact on step 2 
 * since some reducers might depend on the concrete order of test cases.
 * 2. Reduces the set of test cases according to some {@link TestSetReducer}.
 * 3. Sort the reduced set of test cases via a  {@link TestSetSorter}.
 * 4. Generate the in-order String representation using a {@link TestMethodGenerator}.
 * 5. Generate the String representing the overall test class with the ordered String representations of the methods 
 * using a {@link TestClassGenerator}.
 * 6. Optionally print the test cases and returning the String representation.
 */
public class TestCasesStringGenerator {
    /**
     * The test class generator
     */
    protected final TestClassGenerator testClassGenerator;
    /**
     * The test method generator
     */
    protected final TestMethodGenerator testMethodGenerator;
    /**
     * The sorter before reducing the set of test cases
     */
    protected final TestSetSorter preReduceTestSetSorter;
    /**
     * The reducer
     */
    protected final TestSetReducer testSetReducer;
    /**
     * The sorter after reducing the set of test cases
     */
    protected final TestSetSorter postReduceTestSetSorter;
    /**
     * The test cases
     */
    protected final TestCases testCases;
    /**
     * Can be null, an optional printer
     */
    protected final PrintStream printer;

    /**
     * @param testCases The test cases for a method under test
     * @param tcgConfig The configuration
     */
    public TestCasesStringGenerator(
            TestCases testCases,
            TcgConfig tcgConfig) {
        this(
                new JunitJupiterTestClassGenerator(tcgConfig),
                new JunitJupiterTestMethodGenerator(
                        testCases.getMethodUnderTest(),
                        tcgConfig
                ),
                tcgConfig.PRE_REDUCE_TEST_SET_SORTER,
                tcgConfig.TEST_SET_REDUCER,
                tcgConfig.POST_REDUCE_TEST_SET_SORTER,
                testCases,
                tcgConfig.PRINT_STREAM
        );
    }

    /**
     * Can be overridden to generate other String representations than Junit Jupiter representations
     * @param testClassGenerator The test class generator
     * @param testMethodGenerator The method generator
     * @param preReduceTestSetSorter The pre-reduce sorter
     * @param testSetReducer The reducer
     * @param postReduceTestSetSorter The post-reduce sorter
     * @param testCases The test cases for a method under test
     * @param printer Can be null, a printer
     */
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

    /**
     * @return A String representation
     */
    public String generateTestClassStringRepresentation() {
        List<TestCase> preSortedTestCases = preSortTestCases(testCases.getTestCases());
        Collection<TestCase> tests = reduceTestCases(preSortedTestCases);
        List<TestCase> sortedTests = sortReducedTestCases(tests);
        List<StringBuilder> stringsForTests = generateStringRepresentation(sortedTests);

        String result = testClassGenerator.generateTestClassString(
                testCases.getPackageNameOfClassOfMethodUnderTest(),
                testCases.getNameOfClassUnderTest(),
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

    /**
     * @param testCases The test cases
     * @return A String representation of the methods
     */
    protected List<StringBuilder> generateStringRepresentation(List<TestCase> testCases) {
        ArrayList<StringBuilder> stringsForTests = new ArrayList<>();
        for (TestCase tc : testCases) {
            stringsForTests.add(testMethodGenerator.generateTestCaseRepresentation(tc));
        }
        return stringsForTests;
    }

    /**
     * @param testCases The test cases
     * @return A reduced collection of test cases
     */
    protected Collection<TestCase> reduceTestCases(List<TestCase> testCases) {
        return testSetReducer.apply(testCases);
    }

    /**
     * @param testCases The test cases
     * @return A sorted list of the input test cases
     */
    protected List<TestCase> preSortTestCases(Collection<TestCase> testCases) {
        return preReduceTestSetSorter.apply(testCases);
    }

    /**
     * @param testCases The test cases
     * @return A sorted list of the test cases after applying {@link TestCasesStringGenerator#reduceTestCases(List)}
     */
    protected List<TestCase> sortReducedTestCases(Collection<TestCase> testCases) {
        return postReduceTestSetSorter.apply(testCases);
    }
}
