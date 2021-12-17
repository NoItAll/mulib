package de.wwu.mulib.transform_and_execute.examples_executor;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.TestUtility;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.search.trees.Solution;
import de.wwu.mulib.transform_and_execute.examples.BoardTransf;
import de.wwu.mulib.transform_and_execute.examples.NQueensTransf;
import de.wwu.mulib.transform_and_execute.examples.QueenTransf;
import de.wwu.mulib.transformer.MulibTransformer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class NQueensTransfExec {

    @Test
    public void testNQueensTransf() {
        MulibConfig config =
                MulibConfig.builder()
                        .setTRANSF_WRITE_TO_FILE(true)
                        .setTRANSF_VALIDATE_TRANSFORMATION(true)
                        .setTRANSF_REGARD_SPECIAL_CASE(List.of(NQueensTransf.class))
                        .build();
        MulibTransformer transformer = new MulibTransformer(config);
        transformer.transformAndLoadClasses(NQueensTransf.class, BoardTransf.class, QueenTransf.class);
        Class<?> transformedClass = transformer.getTransformedClass(NQueensTransf.class);
        try {
            String className = transformedClass.getSimpleName();
            assertTrue(className.startsWith("__mulib__"));
            // There should always be an empty constructor
            Constructor<?> cons = transformedClass.getDeclaredConstructor(SymbolicExecution.class);
            Object o = cons.newInstance(new Object[] { null });
            assertNotNull(o);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            fail("Exception should not have been thrown");
        }
        Function<MulibConfig, List<PathSolution>> toTestFunction = (mulibConfig) -> _testNQueens(transformedClass, mulibConfig);

        List<List<PathSolution>> solutions = TestUtility.getAllSolutions(toTestFunction, "solve");

        Function<MulibConfig, List<PathSolution>> toTestFunctionAlt = (mulibConfig) -> _testNQueensAlt(transformedClass, mulibConfig);

        List<List<PathSolution>> solutionsAlt = TestUtility.getAllSolutions(toTestFunctionAlt, "solveAlt");
    }

    private List<PathSolution> _testNQueens(Class<?> toExecuteOn, MulibConfig config) {
        List<PathSolution> result = TestUtility.executeMulib(
                "solve",
                toExecuteOn,
                1,
                config
        );
        assertEquals(1, result.size());
        return result;
    }

    private List<PathSolution> _testNQueensAlt(Class<?> toExecuteOn, MulibConfig config) {
        List<PathSolution> result = TestUtility.executeMulib(
                "solveAlt",
                toExecuteOn,
                120,
                config
        );
        assertEquals(1, result.size());
        assertEquals(92, result.get(0).getCurrentlyInitializedSolutions().size());
        assertFalse(
                result.get(0).getCurrentlyInitializedSolutions().parallelStream().anyMatch(s -> {
                    for (Solution sInner : result.get(0).getCurrentlyInitializedSolutions()) {
                        if (s == sInner) continue;
                        if (s.labels.getIdentifiersToValues().equals(sInner.labels.getIdentifiersToValues())) {
                            return true;
                        }
                    }
                    return false;
                })
        );
        return result;
    }
}
