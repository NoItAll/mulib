package de.wwu.mulib.tcg.testmethodgenerator;

import de.wwu.mulib.tcg.TestCase;

import java.util.Set;

/**
 * Generates StringBuilder representations of single tests
 */
public interface TestMethodGenerator {

    /**
     * @return The types that have been encountered while generating representations. This can be used to generate
     * suitable imports via the {@link de.wwu.mulib.tcg.testclassgenerator.JunitJupiterTestClassGenerator}.
     */
    Set<Class<?>> getEncounteredTypes();

    /**
     * @param testCase The test case
     * @return A test representation for the test case
     */
    StringBuilder generateTestCaseRepresentation(TestCase testCase);

}
