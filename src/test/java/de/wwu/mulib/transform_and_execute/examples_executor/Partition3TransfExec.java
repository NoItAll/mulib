package de.wwu.mulib.transform_and_execute.examples_executor;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.TestUtility;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.transform_and_execute.examples.Partition3Transf;
import de.wwu.mulib.transformer.MulibTransformer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class Partition3TransfExec {

    @Test
    public void testPartition3TransfExec() {
        MulibConfig config =
                MulibConfig.builder()
                        .setTRANSF_WRITE_TO_FILE(true)
                        .setTRANSF_VALIDATE_TRANSFORMATION(true)
                        .setTRANSF_REGARD_SPECIAL_CASE(List.of(Partition3Transf.class))
                        .build();
        MulibTransformer transformer = new MulibTransformer(config);
        transformer.transformAndLoadClasses(Partition3Transf.class);
        Class<?> transformedClass = transformer.getTransformedClass(Partition3Transf.class);
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
        Function<MulibConfig, List<PathSolution>> toTestFunction =
                (mulibConfig) -> _testPartition3Transf(transformedClass, mulibConfig);

        TestUtility.getAllSolutions(
            toTestFunction,
            "exec"
        );
    }

    private List<PathSolution> _testPartition3Transf(Class<?> toExecuteOn, MulibConfig config) {
        List<PathSolution> result = TestUtility.executeMulib(
                "exec",
                toExecuteOn,
                1,
                config
        );
        assertFalse(
                result.parallelStream().anyMatch(ps -> {
                    for (PathSolution psInner : result) {
                        if (psInner == ps) continue;
                        if (ps.getInitialSolution().labels.getIdentifiersToValues().equals(
                                psInner.getInitialSolution().labels.getIdentifiersToValues())) {
                            return true;
                        }
                    }
                    return false;
                })
        );
        assertEquals(12, result.size());
        return result;
    }
}
