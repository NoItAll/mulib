package de.wwu.mulib.transformer.examples_executor;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.transformer.MulibTransformer;
import de.wwu.mulib.transformer.examples.inner_classes.SCWI0;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class InnerClassesExec {
    @Test
    public void testInnerClasses0() {
        MulibConfig config =
                MulibConfig.builder()
                        .setTRANSF_WRITE_TO_FILE(true)
                        .setTRANSF_VALIDATE_TRANSFORMATION(true)
                        .setTRANSF_REGARD_SPECIAL_CASE(List.of(SCWI0.class, SCWI0.SCWI_I0.class, SCWI0.SCWI_I1.class))
                        .build();
        MulibTransformer transformer = new MulibTransformer(config);
        transformer.transformAndLoadClasses(SCWI0.class);
        for (Class<?> c : List.of(SCWI0.class, SCWI0.SCWI_I0.class)) {
            Class<?> transformedClass = transformer.getTransformedClass(c);
            try {
                // There should always be a constructor with a SymbolicExecution parameter
                Constructor<?> cons = transformedClass.getDeclaredConstructor(SymbolicExecution.class);
                Object o = cons.newInstance(new Object[]{null});
                assertNotNull(o);
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
                fail("Exception should not have been thrown");
            }
        }
    }
}
