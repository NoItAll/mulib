package de.wwu.mulib.model;

import de.wwu.mulib.exceptions.MulibIllegalStateException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.substitutions.primitives.Sdouble;
import de.wwu.mulib.substitutions.primitives.Slong;
import de.wwu.mulib.transformations.MulibTransformer;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Collection of default model methods that can be used in place of the default implementations.
 * Method names here follow the scheme full_Class_Name__methodName(parameters).
 */
public final class ModelMethods {

    public static Slong java_lang_Double__doubleToRawLongBits(Sdouble d) {
        throw new NotYetImplementedException();
    }

    public static Sdouble java_lang_Double__longBitsToDouble(Slong l) {
        throw new NotYetImplementedException();
    }

    /**
     * Reads the default model methods and can be used to get pairs of methods
     * @param mulibTransformer The used mulib transformer
     * @return The pairs of substituted to substituting methods
     */
    public static Map<Method, Method> readDefaultModelMethods(MulibTransformer mulibTransformer) {
        Method[] ms = ModelMethods.class.getDeclaredMethods();
        Map<Method, Method> result = new HashMap<>();

        try {
            for (Method m : ms) {
                String conventionMethodName = m.getName();
                if (conventionMethodName.equals("readDefaultModelMethods") || m.isSynthetic()) {
                    continue;
                }
                String[] classAndMethodName = conventionMethodName.split("__");
                String className = classAndMethodName[0];
                String methodName = classAndMethodName[1];
                String classNameReplacedUnderscores = className.replace("_", ".");
                Class<?> c = Class.forName(classNameReplacedUnderscores);
                Class<?>[] transformedParameterTypes =
                        Arrays.stream(m.getParameterTypes())
                                .map(mulibTransformer::transformMulibTypeBackIfNeeded)
                                .toArray(Class[]::new);
                Method substitutedMethod = c.getDeclaredMethod(methodName, transformedParameterTypes);
                result.put(substitutedMethod, m);
            }
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new MulibIllegalStateException("Method should be available", e);
        }

        return result;
    }

}
