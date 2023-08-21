package de.wwu.mulib.transform_and_execute.examples_executor;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.TestUtility;
import de.wwu.mulib.search.trees.ThrowablePathSolution;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.solving.Solution;
import de.wwu.mulib.transform_and_execute.examples.NQueensTransf;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class NQueensTransfExec {

    @Test
    public void testNQueensTransfSolve() {
        TestUtility.getAllSolutions(this::_testNQueens, "solve");
        // Check sequence
        TestUtility.getAllSolutions(this::_testNQueensAlt, "solveAlt");
    }

    private List<PathSolution> _testNQueens(MulibConfig.MulibConfigBuilder mb) {
        List<PathSolution> result = TestUtility.executeMulib(
                "solve",
                NQueensTransf.class,
                mb,
                true
        );
        assertEquals(1, result.size());
        assertTrue(result.stream().noneMatch(ps -> ps instanceof ThrowablePathSolution));
        return result;
    }

    private List<Solution> _testNQueensAlt(MulibConfig.MulibConfigBuilder mb) {
        List<Solution> result = TestUtility.getUpToNSolutions(
                120, // Only 92 possible
                "solveAlt",
                NQueensTransf.class,
                mb,
                new Class[0],
                new Object[0]
        );
        assertEquals(92, result.size());
        assertFalse(
                result.parallelStream().anyMatch(s -> {
                    for (Solution sInner : result) {
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
