package de.wwu.mulib.transform_and_execute.examples_executor;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.TestUtility;
import de.wwu.mulib.search.trees.ExceptionPathSolution;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.transform_and_execute.examples.mit_examples.SatPrimes01Transf;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SatPrimes01TransfExec {

    @Test
    public void testSatPrimes01_0() {
        List<List<PathSolution>> solutions =
                TestUtility.getAllSolutions(
                        this::_testSatPrimes01_0,
                        "testSatPrimes01_0"
                );
    }

    @Test
    public void testSatPrimes01_1() {
        List<List<PathSolution>> solutions =
                TestUtility.getAllSolutions(
                        this::_testSatPrimes01_1,
                        "testSatPrimes01_1"
                );
    }

    private List<PathSolution> _testSatPrimes01_0(MulibConfig.MulibConfigBuilder mb) {
        List<PathSolution> result = TestUtility.executeMulib(
                "exec0",
                SatPrimes01Transf.class,
                1,
                mb,
                true
        );
        assertFalse(result.isEmpty());
        assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
        return result;
    }

    private List<PathSolution> _testSatPrimes01_1(MulibConfig.MulibConfigBuilder mb) {
        mb.setFIXED_POSSIBLE_CP_BUDGET(0);
        mb.setFIXED_ACTUAL_CP_BUDGET(0);
        List<PathSolution> result = TestUtility.executeMulib(
                "exec1",
                SatPrimes01Transf.class,
                1,
                mb,
                true
        );
        assertEquals(14, result.size());
        assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
        return result;
    }
}
