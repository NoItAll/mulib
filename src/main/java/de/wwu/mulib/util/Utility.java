package de.wwu.mulib.util;

import de.wwu.mulib.substitutions.AbstractPartnerClass;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Utility {

    private Utility() {}

    public static List<Field> getInstanceFieldsIncludingInheritedFieldsExcludingPartnerClassFields(Class<?> c) {
        List<Field> fields = new ArrayList<>(List.of(getNonStaticFields(c.getDeclaredFields())));

        Class<?> currentClass = c;
        while (currentClass.getSuperclass() != AbstractPartnerClass.class
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
}
