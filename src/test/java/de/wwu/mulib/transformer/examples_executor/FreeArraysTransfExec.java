package de.wwu.mulib.transformer.examples_executor;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.transformations.MulibTransformer;
import de.wwu.mulib.transformer.examples.free_arrays.*;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static de.wwu.mulib.TestUtility.TEST_BUILD_PATH;
import static org.junit.jupiter.api.Assertions.*;

public class FreeArraysTransfExec {

    @Test
    public void testArrayFieldsWithDifferentClassLoader() {
        MulibConfig config =
                MulibConfig.builder()
                        .setTRANSF_WRITE_TO_FILE(true)
                        .setTRANSF_VALIDATE_TRANSFORMATION(true)
                        .build();
        MulibTransformer transformer = MulibTransformer.get(config);
        transformer.transformAndLoadClasses(ArrayFields.class);
        Class<?> transformedClass = transformer.getTransformedClass(ArrayFields.class);
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
    public void testArrayFieldsWithSystemClassLoader() {
        MulibConfig config =
                MulibConfig.builder()
                        .setTRANSF_WRITE_TO_FILE(true)
                        .setTRANSF_VALIDATE_TRANSFORMATION(true)
                        .setTRANSF_LOAD_WITH_SYSTEM_CLASSLOADER(true)
                        .setTRANSF_GENERATED_CLASSES_PATH(TEST_BUILD_PATH)
                        .build();
        MulibTransformer transformer = MulibTransformer.get(config);
        transformer.transformAndLoadClasses(ArrayFields.class);
        Class<?> transformedClass = transformer.getTransformedClass(ArrayFields.class);
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
    public void testArrayFieldsWithInitializationWithDifferentClassLoader() {
        MulibConfig config =
                MulibConfig.builder()
                        .setTRANSF_WRITE_TO_FILE(true)
                        .setTRANSF_VALIDATE_TRANSFORMATION(true)
                        .build();
        MulibTransformer transformer = MulibTransformer.get(config);
        transformer.transformAndLoadClasses(ArrayFieldsWithInitialization.class);
        Class<?> transformedClass = transformer.getTransformedClass(ArrayFieldsWithInitialization.class);
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
    public void testArrayFieldsWithInitializationWithSystemClassLoader() {
        MulibConfig config =
                MulibConfig.builder()
                        .setTRANSF_WRITE_TO_FILE(true)
                        .setTRANSF_VALIDATE_TRANSFORMATION(true)
                        .setTRANSF_LOAD_WITH_SYSTEM_CLASSLOADER(true)
                        .setTRANSF_GENERATED_CLASSES_PATH(TEST_BUILD_PATH)
                        .build();
        MulibTransformer transformer = MulibTransformer.get(config);
        transformer.transformAndLoadClasses(ArrayFieldsWithInitialization.class);
        Class<?> transformedClass = transformer.getTransformedClass(ArrayFieldsWithInitialization.class);
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
    public void testArrayParametersWithDifferentClassLoader() {
        MulibConfig config =
                MulibConfig.builder()
                        .setTRANSF_WRITE_TO_FILE(true)
                        .setTRANSF_VALIDATE_TRANSFORMATION(true)
                        .build();
        MulibTransformer transformer = MulibTransformer.get(config);
        transformer.transformAndLoadClasses(ArrayParameters.class);
        Class<?> transformedClass = transformer.getTransformedClass(ArrayParameters.class);
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
    public void testArrayParametersWithSystemClassLoader() {
        MulibConfig config =
                MulibConfig.builder()
                        .setTRANSF_WRITE_TO_FILE(true)
                        .setTRANSF_VALIDATE_TRANSFORMATION(true)
                        .setTRANSF_LOAD_WITH_SYSTEM_CLASSLOADER(true)
                        .setTRANSF_GENERATED_CLASSES_PATH(TEST_BUILD_PATH)
                        .build();
        MulibTransformer transformer = MulibTransformer.get(config);
        transformer.transformAndLoadClasses(ArrayParameters.class);
        Class<?> transformedClass = transformer.getTransformedClass(ArrayParameters.class);
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
    public void testArrayReturnWithDifferentClassLoader() {
        MulibConfig config =
                MulibConfig.builder()
                        .setTRANSF_WRITE_TO_FILE(true)
                        .setTRANSF_VALIDATE_TRANSFORMATION(true)
                        .build();
        MulibTransformer transformer = MulibTransformer.get(config);
        transformer.transformAndLoadClasses(ArrayReturn.class);
        Class<?> transformedClass = transformer.getTransformedClass(ArrayReturn.class);
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
    public void testArrayReturnWithSystemClassLoader() {
        MulibConfig config =
                MulibConfig.builder()
                        .setTRANSF_WRITE_TO_FILE(true)
                        .setTRANSF_VALIDATE_TRANSFORMATION(true)
                        .setTRANSF_OVERWRITE_FILE_FOR_SYSTEM_CLASSLOADER(true)
                        .setTRANSF_LOAD_WITH_SYSTEM_CLASSLOADER(true)
                        .setTRANSF_GENERATED_CLASSES_PATH(TEST_BUILD_PATH)
                        .build();
        MulibTransformer transformer = MulibTransformer.get(config);
        transformer.transformAndLoadClasses(ArrayReturn.class);
        Class<?> transformedClass = transformer.getTransformedClass(ArrayReturn.class);
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
    public void testArrayIntraMethodTaintWithDifferentClassLoader() {
        MulibConfig config =
                MulibConfig.builder()
                        .setTRANSF_WRITE_TO_FILE(true)
                        .setTRANSF_VALIDATE_TRANSFORMATION(true)
                        .build();
        MulibTransformer transformer = MulibTransformer.get(config);
        transformer.transformAndLoadClasses(ArrayIntraMethodTaint.class);
        Class<?> transformedClass = transformer.getTransformedClass(ArrayIntraMethodTaint.class);
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
    public void testArrayIntraMethodTaintWithSystemClassLoader() {
        MulibConfig config =
                MulibConfig.builder()
                        .setTRANSF_WRITE_TO_FILE(true)
                        .setTRANSF_VALIDATE_TRANSFORMATION(true)
                        .setTRANSF_LOAD_WITH_SYSTEM_CLASSLOADER(true)
                        .setTRANSF_GENERATED_CLASSES_PATH(TEST_BUILD_PATH)
                        .build();
        MulibTransformer transformer = MulibTransformer.get(config);
        transformer.transformAndLoadClasses(ArrayIntraMethodTaint.class);
        Class<?> transformedClass = transformer.getTransformedClass(ArrayIntraMethodTaint.class);
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
    public void testArrayIntraMethodTaintFieldTaintAndReturnTaintWithDifferentClassLoader() {
        MulibConfig config =
                MulibConfig.builder()
                        .setTRANSF_WRITE_TO_FILE(true)
                        .setTRANSF_VALIDATE_TRANSFORMATION(true)
                        .build();
        MulibTransformer transformer = MulibTransformer.get(config);
        transformer.transformAndLoadClasses(ArrayIntraMethodTaintFieldTaintAndReturnTaint.class);
        Class<?> transformedClass = transformer.getTransformedClass(ArrayIntraMethodTaintFieldTaintAndReturnTaint.class);
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
