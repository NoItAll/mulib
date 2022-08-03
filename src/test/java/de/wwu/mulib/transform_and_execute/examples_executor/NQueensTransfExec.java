package de.wwu.mulib.transform_and_execute.examples_executor;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.TestUtility;
import de.wwu.mulib.search.trees.ExceptionPathSolution;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.search.trees.Solution;
import de.wwu.mulib.transform_and_execute.examples.NQueensTransf;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class NQueensTransfExec {

    @Test
    public void testNQueensTransfSolve() {
        TestUtility.getAllSolutions(this::_testNQueens, "solve");
        // Check sequenece
        TestUtility.getAllSolutions(this::_testNQueensAlt, "solveAlt");
    }

    private List<PathSolution> _testNQueens(MulibConfig.MulibConfigBuilder mb) {
        List<PathSolution> result = TestUtility.executeMulib(
                "solve",
                NQueensTransf.class,
                1,
                mb,
                true
        );
        assertEquals(1, result.size());
        assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
        return result;
    }

    private List<PathSolution> _testNQueensAlt(MulibConfig.MulibConfigBuilder mb) {
        List<PathSolution> result = TestUtility.executeMulib(
                "solveAlt",
                NQueensTransf.class,
                120,
                mb,
                true
        );
        assertEquals(1, result.size());
        assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
        assertEquals(92, result.get(0).getCurrentlyInitializedSolutions().size());
        assertFalse(
                result.get(0).getCurrentlyInitializedSolutions().parallelStream().anyMatch(s -> {
                    for (Solution sInner : result.get(0).getCurrentlyInitializedSolutions()) {
                        if (s == sInner) continue;
                        if (s.labels.getIdToLabel().equals(sInner.labels.getIdToLabel())) {
                            return true;
                        }
                    }
                    return false;
                })
        );
        return result;
    }
}
