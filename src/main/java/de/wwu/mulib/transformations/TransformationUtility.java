package de.wwu.mulib.transformations;

import de.wwu.mulib.exceptions.MulibRuntimeException;

import java.lang.reflect.Field;

public class TransformationUtility {
    public static String determineNestHostFieldName(String classPath) {
        Class<?> c;
        try {
            c = Class.forName(classPath.replace("/", "."));
        } catch (ClassNotFoundException e) {
            throw new MulibRuntimeException("Could not find inner non-static class.", e);
        }
        String nestHostFieldName = null;
        for (Field f : c.getDeclaredFields()) {
            if (f.getName().startsWith("this$") && f.isSynthetic()) {
                if (nestHostFieldName != null) {
                    throw new MulibRuntimeException("We do not yet support automatically finding the correct " +
                            "nest host field when multiple synthetic fields are named starting with 'this$'.");
                }
                nestHostFieldName = f.getName();
            }
        }

        return nestHostFieldName;
    }

    public static Class<?> getClassForPath(String path, ClassLoader classLoader) {
        return getClassForName(classPathToType(path), classLoader);
    }

    public static Class<?> getClassForName(String name, ClassLoader classLoader) {
        try {
            return classLoader.loadClass(name);
        } catch (ClassNotFoundException e) {
            throw new MulibRuntimeException("Cannot locate class for String " + name, e);
        }
    }

    public static String classPathToType(String s) {
        return s.replace("/", ".");
    }

}
