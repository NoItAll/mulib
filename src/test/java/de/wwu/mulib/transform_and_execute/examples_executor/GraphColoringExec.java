package de.wwu.mulib.transform_and_execute.examples_executor;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.TestUtility;
import de.wwu.mulib.search.trees.ThrowablePathSolution;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.transform_and_execute.examples.GraphColoring;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GraphColoringExec {

    @Test
    public void testGraphColoring() {
        TestUtility.getAllSolutions((mb) -> _testGraphColoring(mb), "exec");
    }

    private List<PathSolution> _testGraphColoring(MulibConfig.MulibConfigBuilder mb) {
        List<PathSolution> result = TestUtility.executeMulib(
                "exec",
                GraphColoring.class,
                mb,
                true
        );
        assertEquals(1, result.size());
        assertTrue(result.stream().noneMatch(ps -> ps instanceof ThrowablePathSolution));
        return result;
    }

}
