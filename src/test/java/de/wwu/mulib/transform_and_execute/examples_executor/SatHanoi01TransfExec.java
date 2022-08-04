package de.wwu.mulib.transform_and_execute.examples_executor;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.TestUtility;
import de.wwu.mulib.search.trees.ExceptionPathSolution;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.search.trees.Solution;
import de.wwu.mulib.transform_and_execute.examples.mit_examples.SatHanoi01Transf;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SatHanoi01TransfExec {

    @Test
    public void testSatHanoi01Transf() {
        TestUtility.getAllSolutions(
                mulibConfig -> mulibConfig.setFIXED_ACTUAL_CP_BUDGET(2500).setFIXED_POSSIBLE_CP_BUDGET(2500),
                this::_testSatHanoi01,
                "exec"
        );
    }

    private List<PathSolution> _testSatHanoi01(MulibConfig.MulibConfigBuilder mb) {
        List<PathSolution> result = TestUtility.executeMulib(
                "exec",
                SatHanoi01Transf.class,
                mb,
                true
        );
        assertEquals(10, result.size());
        assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));

        List<Solution> solutions = TestUtility.getUpToNSolutions(
                100, // Only 10 possible
                "exec",
                SatHanoi01Transf.class,
                mb
        );
        assertEquals(10, solutions.size());
        for (Solution s : solutions) {
            int counter = (Integer) s.returnValue;
            int n = (Integer) s.labels.getIdToLabel().get("n");
            assertEquals(counter, Math.pow(2, n)-1);
        }
        return result;
    }
}
