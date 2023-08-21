package de.wwu.mulib.util;

import de.wwu.mulib.Mulib;
import de.wwu.mulib.throwables.MulibRuntimeException;
import de.wwu.mulib.substitutions.PartnerClassObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Contains Utility methods for Mulib
 */
public final class Utility {

    private Utility() {}

    /**
     * @param c The class to get fields from
     * @return All non-static fields, even private ones, of this class and superclasses this class inherits from
     */
    public static List<Field> getInstanceFieldsIncludingInheritedFieldsExcludingPartnerClassFields(Class<?> c) {
        List<Field> fields = new ArrayList<>(List.of(getNonStaticFields(c.getDeclaredFields())));

        Class<?> currentClass = c;
        while (currentClass.getSuperclass() != PartnerClassObject.class
                && currentClass.getSuperclass() != Object.class) {
            currentClass = currentClass.getSuperclass();
            fields.addAll(List.of(getNonStaticFields(c.getDeclaredFields())));
        }

        return fields;
    }

    /**
     * @param c The class
     * @return The static accessible fields of this class
     */
    public static Collection<Field> getAccessibleStaticFields(Class<?> c) {
        Set<Field> result = new HashSet<>();
        Field[] fs = c.getDeclaredFields();
        for (Field f : fs) {
            if (Modifier.isFinal(f.getModifiers())) {
                continue;
            }
            if (Modifier.isStatic(f.getModifiers())) {
                if (!f.trySetAccessible()) {
                    Mulib.log.warning("Setting static field " + f.getName() + " in "
                            + f.getDeclaringClass().getName() + " failed. It will not be regarded while backtracking!");
                    continue;
                }
                result.add(f);
            }
        }
        return result;
    }

    /**
     * @param fields The fields
     * @return The non static fields in fields
     */
    public static Field[] getNonStaticFields(Field[] fields) {
        return Arrays.stream(fields).filter(f -> !Modifier.isStatic(f.getModifiers())).toArray(Field[]::new);
    }

    /**
     * Sets all fields in fs as accessible
     * @param fs The fields
     */
    public static void setAllAccessible(Iterable<Field> fs) {
        fs.spliterator().forEachRemaining(f -> f.setAccessible(true));
    }

    /**
     * Sets all fields in fs as accessible
     * @param fs The fields
     */
    public static void setAllAccessible(Field[] fs) {
        Arrays.stream(fs).forEach(f -> f.setAccessible(true));
    }

    /**
     * If there is an exception while getting the method, a {@link MulibRuntimeException} is thrown
     * @param clazz The class containing the method
     * @param methodName The method name
     * @param parameterTypes The parameter types
     * @return The method
     */
    public static Method getMethodFromClass(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        try {
            return clazz.getDeclaredMethod(methodName, parameterTypes);
        } catch (Exception e) {
            throw new MulibRuntimeException("Cannot find method for class", e);
        }
    }

    /**
     * If there is an exception while getting the method, a {@link MulibRuntimeException} is thrown
     * @param className The name of the class containing the method
     * @param methodName The method name
     * @param parameterTypes The parameter types
     * @return The method
     */
    public static Method getMethodFromClass(String className, String methodName, Class<?>... parameterTypes) {
        try {
            Class<?> clazz = Class.forName(className);
            return getMethodFromClass(clazz, methodName, parameterTypes);
        } catch (Exception e) {
            throw new MulibRuntimeException("Cannot find method for class", e);
        }
    }
}
