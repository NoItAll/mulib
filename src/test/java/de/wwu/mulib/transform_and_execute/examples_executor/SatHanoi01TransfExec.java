package de.wwu.mulib.transform_and_execute.examples_executor;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.TestUtility;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.transform_and_execute.examples.SatHanoi01Transf;
import de.wwu.mulib.transformer.MulibTransformer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class SatHanoi01TransfExec {

    @Test
    public void testSatHanoi01Transf() {
        MulibConfig config =
                MulibConfig.builder()
                        .setTRANSF_WRITE_TO_FILE(true)
                        .setTRANSF_VALIDATE_TRANSFORMATION(true)
                        .setTRANSF_REGARD_SPECIAL_CASE(List.of(SatHanoi01Transf.class))
                        .build();
        MulibTransformer transformer = new MulibTransformer(config);
        transformer.transformAndLoadClasses(SatHanoi01Transf.class);
        Class<?> transformedClass = transformer.getTransformedClass(SatHanoi01Transf.class);
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
                (mulibConfig) -> _testSatHanoi01(transformedClass, mulibConfig);

        List<List<PathSolution>> solutions =
                TestUtility.getAllSolutions(
                        mulibConfig -> mulibConfig.setFIXED_ACTUAL_CP_BUDGET(2500).setFIXED_POSSIBLE_CP_BUDGET(2500),
                        toTestFunction,
                        "exec"
                );
    }

    private List<PathSolution> _testSatHanoi01(Class<?> toExecuteOn, MulibConfig config) {
        List<PathSolution> result = TestUtility.executeMulib(
                "exec",
                toExecuteOn,
                1,
                config
        );
        assertEquals(10, result.size());
        for (PathSolution ps : result) {
            assertEquals(1, ps.getCurrentlyInitializedSolutions().size());
            int counter = (Integer) ps.getInitialSolution().value;
            int n = (Integer) ps.getInitialSolution().labels.getIdentifiersToValues().get("n");
            assertEquals(counter, Math.pow(2, n)-1);
        }
        return result;
    }
}
