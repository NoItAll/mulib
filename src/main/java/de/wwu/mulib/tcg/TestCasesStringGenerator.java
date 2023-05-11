package de.wwu.mulib.tcg;

import com.google.common.collect.Lists;
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
    public static final String REFLECTION_SETTER_METHOD_NAME = "setWithReflection";
    protected TestClassGenerator testClassGenerator;
    protected TestMethodGenerator testMethodGenerator;
    protected TestSetReducer testSetReducer;
    protected TestSetSorter testSetSorter;
    protected final TestCases testCases;
    protected final PrintStream printer;

    private TestCasesStringGenerator(
            TestCases testCases,
            PrintStream printer) {
        this.testCases = testCases;
        testMethodGenerator = new Junit5_8TestMethodGenerator(testCases, true, "1e-8");
        testClassGenerator = new Junit5_8TestClassGenerator();
        this.printer = printer;
    }

    public static TestCasesStringGenerator get(TestCases testCases) {
        return get(testCases, System.out);
    }

    public static TestCasesStringGenerator get(TestCases testCases, PrintStream printer) {
        return new TestCasesStringGenerator(testCases, printer);
    }

    public String generateTestClassStringRepresentation() {
        Collection<TestCase> tests = reduceTestCases(testCases.getTestCases());
        List<TestCase> sortedTests = sortTestCases(tests);
        List<StringBuilder> stringsForTests = generateStringRepresentation(sortedTests);

        String result = testClassGenerator.generateTestClassString(
                testCases.getPackageNameOfClassOfTestedMethod(),
                testCases.getNameOfTestedClass(),
                testMethodGenerator.getEncounteredTypes(),
                stringsForTests
        );
        printer.println(result);
        return result;
    }

    protected List<StringBuilder> generateStringRepresentation(Collection<TestCase> testCases) {
        ArrayList<StringBuilder> stringsForTests = new ArrayList<>();
        while (testMethodGenerator.hasNextTestCase()) {
            stringsForTests.add(testMethodGenerator.generateNextTestCaseRepresentation());
        }
        return stringsForTests;
    }

    protected Collection<TestCase> reduceTestCases(Collection<TestCase> testCases) {
        if (testSetReducer == null) {
            testSetReducer = ts -> ts;
        }
        return testSetReducer.apply(testCases);
    }

    protected List<TestCase> sortTestCases(Collection<TestCase> testCases) {
        if (testSetSorter == null) {
            testSetSorter = Lists::newArrayList;
        }
        return testSetSorter.apply(testCases);
    }

    public void setTestSetReducer(TestSetReducer reducer){
        this.testSetReducer = reducer;
    }
}
