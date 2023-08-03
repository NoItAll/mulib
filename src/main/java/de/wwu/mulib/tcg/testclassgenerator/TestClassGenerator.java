package de.wwu.mulib.tcg.testclassgenerator;

import java.util.List;
import java.util.Set;

public interface TestClassGenerator {

    String generateTestClassString(
            String packageName,
            String testedClassName,
            Set<Class<?>> encounteredTypes,
            int initialNumberOfTestCases,
            int reducedNumberOfTestCases,
            List<StringBuilder> testMethodStringBuilders);

}
