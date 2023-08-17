package de.wwu.mulib.util;

import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.substitutions.PartnerClassObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Utility {

    private Utility() {}

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

    public static Field[] getNonStaticFields(Field[] fields) {
        return Arrays.stream(fields).filter(f -> !Modifier.isStatic(f.getModifiers())).toArray(Field[]::new);
    }

    public static void setAllAccessible(Iterable<Field> fs) {
        fs.spliterator().forEachRemaining(f -> f.setAccessible(true));
    }

    public static void setAllAccessible(Field[] fs) {
        Arrays.stream(fs).forEach(f -> f.setAccessible(true));
    }

    public static Method getMethodFromClass(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        try {
            return clazz.getDeclaredMethod(methodName, parameterTypes);
        } catch (Exception e) {
            throw new MulibRuntimeException("Cannot find method for class", e);
        }
    }

    public static Method getMethodFromClass(String className, String methodName, Class<?>... parameterTypes) {
        try {
            Class<?> clazz = Class.forName(className);
            return getMethodFromClass(clazz, methodName, parameterTypes);
        } catch (Exception e) {
            throw new MulibRuntimeException("Cannot find method for class", e);
        }
    }
}
