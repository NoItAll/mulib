package de.wwu.mulib.transform_and_execute.examples_executor;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.TestUtility;
import de.wwu.mulib.search.trees.ExceptionPathSolution;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.transform_and_execute.examples.mit_examples.SatPrimes01Transf;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SatPrimes01TransfExec {


    @Test
    public void testSatPrimes01_1() {
        TestUtility.getAllSolutions(
                        this::_testSatPrimes01_1,
                        "testSatPrimes01_1"
        );
    }

    private List<PathSolution> _testSatPrimes01_1(MulibConfig.MulibConfigBuilder mb) {
        mb.setBUDGET_FIXED_ACTUAL_CP(0); // This means infinite budget
        List<PathSolution> result = TestUtility.executeMulib(
                "exec1",
                SatPrimes01Transf.class,
                mb,
                true
        );
        assertEquals(14, result.size());
        assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
        return result;
    }
}
