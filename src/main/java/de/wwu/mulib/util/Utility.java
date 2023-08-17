package de.wwu.mulib.util;

import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.substitutions.PartnerClassObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
