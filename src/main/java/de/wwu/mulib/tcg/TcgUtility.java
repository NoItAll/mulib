package de.wwu.mulib.tcg;

import java.util.regex.Pattern;

// Taken from https://github.com/wwu-pi/muli-classpath/blob/0fad44b5cd5898f1064c558820096c1d7ae85d8e/src/main/java/de/wwu/muli/tcg/utility/Utility.java
public final class TcgUtility {

    public static final Pattern INPUT_ARGUMENT_NAME_PATTERN = Pattern.compile("arg([0-9]+)");
    public static final Pattern INPUT_OBJECT_ARGUMENT_POST_STATE_PATTERN = Pattern.compile("arg([0-9]+)AfterExec");
    public static final String REFLECTION_SETTER_METHOD_NAME = "setWithReflection";
    public static final String REFLECTION_GETTER_METHOD_NAME = "getWithReflection";
    public static final String REFLECTION_COMPARE_ARRAYS_INNER = "_assertArraysEqualWithReflection";
    public static final String REFLECTION_COMPARE_OBJECTS_INNER = "_assertObjectsEqualWithReflection";
    public static final String REFLECTION_COMPARE_OBJECTS = "assertEqualWithReflection";

    private TcgUtility() {}

    public static String toFirstLower(String s) {
        return s.substring(0, 1).toLowerCase() + s.substring(1);
    }

    public static String toFirstUpper(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    public static boolean isFloatingPointClass(Class<?> oc) {
        return Double.class.equals(oc) || Float.class.equals(oc) || double.class.equals(oc) || float.class.equals(oc);
    }

    public static boolean isWrappingClass(Class<?> oc) {
        return Integer.class.equals(oc) || Long.class.equals(oc) || Double.class.equals(oc) || Float.class.equals(oc) ||
                Short.class.equals(oc) || Byte.class.equals(oc) || Boolean.class.equals(oc) || Character.class.equals(oc);
    }

    public static boolean isPrimitiveClass(Class<?> oc) {
        return int.class.equals(oc) || long.class.equals(oc) || double.class.equals(oc) || float.class.equals(oc) ||
                short.class.equals(oc) || byte.class.equals(oc) || boolean.class.equals(oc) || char.class.equals(oc);
    }

    public static boolean isStringClass(Class<?> oc) {
        return oc.equals(String.class);
    }
}
