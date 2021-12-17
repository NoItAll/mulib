package de.wwu.mulib.transform_and_execute.examples_executor;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.TestUtility;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.search.trees.ExceptionPathSolution;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.transform_and_execute.examples.Partition3Transf;
import de.wwu.mulib.transformations.MulibTransformer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class Partition3TransfExec {

    @Test
    public void testPartition3TransfExec() {
        TestUtility.getAllSolutions(
                this::_testPartition3Transf,
                "exec"
        );
    }

    private List<PathSolution> _testPartition3Transf(MulibConfig.MulibConfigBuilder mb) {
        List<PathSolution> result = TestUtility.executeMulib(
                "exec",
                Partition3Transf.class,
                1,
                mb,
                true
        );
        assertFalse(
                result.parallelStream().anyMatch(ps -> {
                    for (PathSolution psInner : result) {
                        if (psInner == ps) continue;
                        if (ps.getInitialSolution().labels.getIdToLabel().equals(
                                psInner.getInitialSolution().labels.getIdToLabel())) {
                            return true;
                        }
                    }
                    return false;
                })
        );
        assertEquals(12, result.size());
        assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
        return result;
    }
}
