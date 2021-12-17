package de.wwu.mulib.transformer.examples_executor;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.transformer.MulibTransformer;
import de.wwu.mulib.transformer.examples.class_hierarchies.*;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ClassHierarchiesExec {

    @Test
    public void testClassHierarchy0() {
        MulibConfig config =
                MulibConfig.builder()
                        .setTRANSF_WRITE_TO_FILE(true)
                        .setTRANSF_VALIDATE_TRANSFORMATION(true)
                        .setTRANSF_REGARD_SPECIAL_CASE(List.of(
                                C0.class, C1.class, C2.class, C3.class, C4.class, C5.class,
                                I0.class, I1.class, I2.class, I3.class, I4.class, I5.class
                        ))
                        .build();
        MulibTransformer transformer = new MulibTransformer(config);
        transformer.transformAndLoadClasses(C1.class);
        Class<?> transformedClass = transformer.getTransformedClass(C0.class);
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

    @Test
    public void testClassHierarchy1() {
        MulibConfig config =
                MulibConfig.builder()
                        .setTRANSF_WRITE_TO_FILE(true)
                        .setTRANSF_VALIDATE_TRANSFORMATION(true)
                        .setTRANSF_REGARD_SPECIAL_CASE(List.of(
                                C0.class, C1.class, C2.class, C3.class, C4.class, C5.class,
                                I0.class, I1.class, I2.class, I3.class, I4.class, I5.class
                        ))
                        .build();
        MulibTransformer transformer = new MulibTransformer(config);
        transformer.transformAndLoadClasses(C2.class);
        Class<?> transformedClass = transformer.getTransformedClass(C2.class);
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

    @Test
    public void testClassHierarchy2() {
        MulibConfig config =
                MulibConfig.builder()
                        .setTRANSF_WRITE_TO_FILE(true)
                        .setTRANSF_VALIDATE_TRANSFORMATION(true)
                        .setTRANSF_REGARD_SPECIAL_CASE(List.of(
                                C0.class, C1.class, C2.class, C3.class, C4.class, C5.class,
                                I0.class, I1.class, I2.class, I3.class, I4.class, I5.class
                        ))
                        .build();
        MulibTransformer transformer = new MulibTransformer(config);
        transformer.transformAndLoadClasses(C3.class);
        Class<?> transformedClass = transformer.getTransformedClass(C3.class);
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
