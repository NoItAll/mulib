package de.wwu.mulib.tcg.testclassgenerator;

import java.util.List;
import java.util.Set;

/**
 * Generates String representations of test classes together with all the methods.
 * Can also generate utility methods as implied by, e.g., {@link de.wwu.mulib.tcg.TcgConfig#ASSUME_EQUALS_METHODS}.
 */
public interface TestClassGenerator {

    /**
     * @param packageName The package name to generate the test class in
     * @param testedClassName The name of the class declaring the method under test
     * @param encounteredTypes The types that shall be imported
     * @param initialNumberOfTestCases The initial number of test cases for statistics
     * @param reducedNumberOfTestCases The number of test cases after reduction for statistics
     * @param testMethodStringBuilders The StringBuilders for the single method representations
     * @return A String representing a test class with its tests
     */
    String generateTestClassString(
            String packageName,
            String testedClassName,
            Set<Class<?>> encounteredTypes,
            int initialNumberOfTestCases,
            int reducedNumberOfTestCases,
            List<StringBuilder> testMethodStringBuilders);

}
