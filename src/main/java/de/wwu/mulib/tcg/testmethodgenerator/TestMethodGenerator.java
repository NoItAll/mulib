package de.wwu.mulib.tcg.testmethodgenerator;

import java.util.Set;

public interface TestMethodGenerator {

    Set<Class<?>> getEncounteredTypes();

    StringBuilder generateNextTestCaseRepresentation();

    boolean hasNextTestCase();

}
