package de.wwu.mulib.tcg;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

/**
 * Comprises a list of {@link TestCase}s and a reference to the method under test
 */
public class TestCases {
    private final List<TestCase> testCases;
    private final Method testedMethod;

    /**
     * @param testCases The test cases
     * @param testedMethod The method under test
     */
    public TestCases(List<TestCase> testCases, Method testedMethod) {
        this.testCases = testCases;
        this.testedMethod = testedMethod;
    }

    /**
     * @return The class declaring the method under test
     */
    public Class<?> getClassOfMethodUnderTest() {
        return testedMethod.getDeclaringClass();
    }

    /**
     * @return The method under test
     */
    public Method getMethodUnderTest() {
        return testedMethod;
    }

    /**
     * @return The name of the class under test
     */
    public String getNameOfClassUnderTest() {
        return getClassOfMethodUnderTest().getSimpleName();
    }

    /**
     * @return The name of the method under test
     */
    public String getNameOfMethodUnderTest() {
        return getMethodUnderTest().getName();
    }

    /**
     * @return The package name of the class declaring the method under test
     */
    public String getPackageNameOfClassOfMethodUnderTest() {
        return getClassOfMethodUnderTest().getPackageName();
    }

    /**
     * @return The test cases
     */
    public List<TestCase> getTestCases() {
        return testCases;
    }

    /**
     * @return The number of test cases
     */
    public int getNumberTestCases() {
        return testCases.size();
    }
}
