package de.wwu.mulib.tcg.testmethodgenerator;

import de.wwu.mulib.tcg.TestCase;

import java.util.Set;

public interface TestMethodGenerator {

    Set<Class<?>> getEncounteredTypes();

    StringBuilder generateTestCaseRepresentation(TestCase testCase);

}
