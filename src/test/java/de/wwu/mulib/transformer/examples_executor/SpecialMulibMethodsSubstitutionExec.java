package de.wwu.mulib.transformer.examples_executor;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.transformer.MulibTransformer;
import de.wwu.mulib.transformer.examples.SpecialMulibSubstitutionMethods;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SpecialMulibMethodsSubstitutionExec {

    @Test
    public void testSpecialMulibMethodsSubstitution() {
        MulibConfig config =
                MulibConfig.builder()
                        .setTRANSF_WRITE_TO_FILE(true)
                        .setTRANSF_VALIDATE_TRANSFORMATION(true)
                        .setTRANSF_REGARD_SPECIAL_CASE(List.of(SpecialMulibSubstitutionMethods.class))
                        .build();
        MulibTransformer transformer = new MulibTransformer(config);
        transformer.transformAndLoadClasses(SpecialMulibSubstitutionMethods.class);
        Class<?> transformedClass = transformer.getTransformedClass(SpecialMulibSubstitutionMethods.class);
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
    }
}
