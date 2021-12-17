package de.wwu.mulib.transformer.examples_executor;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.transformer.CustomException1;
import de.wwu.mulib.transformations.MulibTransformer;
import de.wwu.mulib.transformer.examples.CustomException0;
import de.wwu.mulib.transformer.examples.CustomRuntimeException;
import de.wwu.mulib.transformer.examples.TryCatchThrow;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TryCatchThrowExec {

    @Test
    public void testTryCatchThrow() {
        MulibConfig config =
                MulibConfig.builder()
                        .setTRANSF_WRITE_TO_FILE(true)
                        .setTRANSF_VALIDATE_TRANSFORMATION(true)
                        .setTRANSF_REGARD_SPECIAL_CASE(
                                List.of(
                                        TryCatchThrow.class,
                                        CustomRuntimeException.class,
                                        CustomException0.class,
                                        CustomException1.class
                                )
                        ).build();
        MulibTransformer transformer = new MulibTransformer(config);
        transformer.transformAndLoadClasses(TryCatchThrow.class);
        Class<?> transformedClass = transformer.getTransformedClass(TryCatchThrow.class);
        try {
            String className = transformedClass.getSimpleName();
            assertTrue(className.startsWith("__mulib__"));
            // There should always be an empty constructor
            Constructor<?> cons = transformedClass.getConstructor();
            Object o = cons.newInstance();
            assertNotNull(o);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            fail("Exception should not have been thrown");
        }
    }
}
