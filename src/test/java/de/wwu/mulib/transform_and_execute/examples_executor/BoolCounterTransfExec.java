package de.wwu.mulib.transform_and_execute.examples_executor;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.TestUtility;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.transform_and_execute.examples.BoolCounterTransf;
import de.wwu.mulib.transformer.MulibTransformer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class BoolCounterTransfExec {
    @Test
    public void testBoolCounterTransf() {
        MulibConfig config =
                MulibConfig.builder()
                        .setTRANSF_WRITE_TO_FILE(true)
                        .setTRANSF_VALIDATE_TRANSFORMATION(true)
                        .setTRANSF_REGARD_SPECIAL_CASE(List.of(BoolCounterTransf.class))
                        .build();
        MulibTransformer transformer = new MulibTransformer(config);
        transformer.transformAndLoadClasses(BoolCounterTransf.class);
        Class<?> transformedClass = transformer.getTransformedClass(BoolCounterTransf.class);
        try {
            String className = transformedClass.getSimpleName();
            assertTrue(className.startsWith("__mulib__"));
            // There should always be a constructor with a SymbolicExecution parameter
            Constructor<?> cons = transformedClass.getDeclaredConstructor(SymbolicExecution.class);
            Object o = cons.newInstance(new Object[] { null });
            assertNotNull(o);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            fail("Exception should not have been thrown");
        }
        Function<MulibConfig, List<PathSolution>> toTestFunction = (mulibConfig) -> _testBooleanCounterSized4(transformedClass, mulibConfig);

        List<List<PathSolution>> solutions = TestUtility.getAllSolutions(toTestFunction, "count4");
    }

    private List<PathSolution> _testBooleanCounterSized4(Class<?> toExecuteOn, MulibConfig config) {
        List<PathSolution> result = TestUtility.executeMulib(
                "count4",
                toExecuteOn,
                config
        );
        assertEquals(16, result.size());
        testIfAllNumbersInRangeAndNoAdditionalSolutions(result);
        return result;
    }

    private void testIfAllNumbersInRangeAndNoAdditionalSolutions(List<PathSolution> result) {
        for (int i = 0; i < 16; i++) {
            final Integer I = i;
            assertTrue(result.stream().anyMatch(s -> I.equals(((Integer) s.getInitialSolution().value))),
                    "Value " + i + " is expected but cannot be found.");
        }
        for (PathSolution ps : result) {
            assertEquals(1, ps.getCurrentlyInitializedSolutions().size());
        }
    }
}
