package de.wwu.mulib.tcg;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

public class TestCases {
    private List<TestCase> testCases;
    private final Method testedMethod;

    public TestCases(List<TestCase> testCases, Method testedMethod) {
        this.testCases = testCases;
        this.testedMethod = testedMethod;
    }

    public Class<?> getClassOfTestedMethod() {
        return testedMethod.getDeclaringClass();
    }

    public Method getTestedMethod() {
        return testedMethod;
    }

    public String getNameOfTestedClass() {
        return getClassOfTestedMethod().getSimpleName();
    }

    public String getNameOfTestedMethod() {
        return getTestedMethod().getName();
    }

    public String getPackageNameOfClassOfTestedMethod() {
        return getClassOfTestedMethod().getPackageName();
    }

    public Iterator<TestCase> iterator() {
        return testCases.iterator();
    }

    public void setTestCases(List<TestCase> testCases) {
       this.testCases = testCases;
    }

    public List<TestCase> getTestCases() {
        return testCases;
    }
}
