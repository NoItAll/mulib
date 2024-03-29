package de.wwu.mulib.transform_and_execute.examples_executor;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.TestUtility;
import de.wwu.mulib.search.trees.ThrowablePathSolution;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.solving.Solution;
import de.wwu.mulib.transform_and_execute.examples.BoolCounterTransf;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BoolCounterTransfExec {
    @Test
    public void testBoolCounterTransf() {
        TestUtility.getAllSolutions(this::_testBooleanCounterSized4, "count4");
    }

    private List<PathSolution> _testBooleanCounterSized4(MulibConfig.MulibConfigBuilder mb) {
        List<PathSolution> result = TestUtility.executeMulib(
                "count4",
                BoolCounterTransf.class,
                mb,
                true
        );
        assertEquals(16, result.size());
        assertTrue(result.stream().noneMatch(ps -> ps instanceof ThrowablePathSolution));
        testIfAllNumbersInRangeAndNoAdditionalSolutions(result);
        List<Solution> solutions = TestUtility.getUpToNSolutions(
                100, // Only 16 possible
                "count4",
                BoolCounterTransf.class,
                mb,
                true,
                new Class[0],
                new Object[0]
        );
        assertEquals(16, solutions.size());
        return result;
    }

    private void testIfAllNumbersInRangeAndNoAdditionalSolutions(List<PathSolution> result) {
        for (int i = 0; i < 16; i++) {
            final Integer I = i;
            assertTrue(result.stream().anyMatch(s -> I.equals((s.getSolution().returnValue))),
                    "Value " + i + " is expected but cannot be found.");
        }
    }
}
