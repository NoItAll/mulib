package de.wwu.mulib.transform_and_execute.examples_executor;

import de.wwu.mulib.TestUtility;
import de.wwu.mulib.model.classes.java.lang.IntegerReplacement;
import de.wwu.mulib.model.classes.java.lang.NumberReplacement;
import de.wwu.mulib.search.trees.ExceptionPathSolution;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.search.trees.Solution;
import de.wwu.mulib.transform_and_execute.examples.IntegerScenario;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReplacementExec {

    @Test
    public void checkTransformIntegerReplacement() {
        TestUtility.getAllSolutions(
                (mb) -> {
                    mb.setHIGH_LEVEL_FREE_ARRAY_THEORY(true);
                    mb.setTRANSF_REGARD_SPECIAL_CASE(List.of(IntegerScenario.class, NumberReplacement.class, IntegerReplacement.class));
                    List<PathSolution> result = TestUtility.executeMulib(
                            "checkInit",
                            IntegerScenario.class,
                            mb,
                            true
                    );
                    assertEquals(4, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    int numbersLessThan = 0;
                    int numbersMoreThan = 0;
                    for (PathSolution ps : result) {
                        Solution s = ps.getSolution();
                        if (1 > (Integer) s.labels.getLabelForId("i")) {
                            numbersLessThan++;
                            assertEquals(s.labels.getLabelForId("i"), s.returnValue);
                        } else {
                            numbersMoreThan++;
                            assertEquals(155, s.returnValue);
                        }
                    }
                    assertEquals(2, numbersLessThan);
                    assertEquals(2, numbersMoreThan);
                },
                "checkInit");
    }

}
