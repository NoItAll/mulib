package de.wwu.mulib.transform_and_execute.examples_executor;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.TestUtility;
import de.wwu.mulib.search.trees.ExceptionPathSolution;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.transform_and_execute.examples.mit_examples.FloatInstructions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FloatInstructionsExec {
    @Test
    public void testFloatInstructionsExec() {
        TestUtility.getAllSolutions(
                this::_testFloatInstructionsExec,
                "testFloatInstructionsExec"
        );
    }

    private List<PathSolution> _testFloatInstructionsExec(MulibConfig.MulibConfigBuilder mb) {
        List<PathSolution> result = TestUtility.executeMulib(
                "exec",
                FloatInstructions.class,
                1,
                mb,
                true
        );
        assertEquals(2, result.size());
        assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
        return result;
    }
}
