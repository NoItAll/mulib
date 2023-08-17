package de.wwu.mulib.tcg;

import java.util.regex.Pattern;

// HW: Parts are taken from https://github.com/wwu-pi/muli-classpath/blob/0fad44b5cd5898f1064c558820096c1d7ae85d8e/src/main/java/de/wwu/muli/tcg/utility/Utility.java

/**
 * Utility methods and constants for test case generation
 */
public final class TcgUtility {

    /**
     * Input arguments fed into the constructor of {@link TestCase} must follow this pattern
     */
    public static final Pattern INPUT_ARGUMENT_NAME_PATTERN = Pattern.compile("arg([0-9]+)");
    /**
     * The objects that document the state of input objects after executing the method under test and that are fed into
     * the constructor of {@link TestCase} must follow this pattern
     */
    public static final Pattern INPUT_OBJECT_ARGUMENT_POST_STATE_PATTERN = Pattern.compile("arg([0-9]+)AfterExec");
    /**
     * Name of the method used if we generate new instances via reflection
     */
    public static final String REFLECTION_NEW_INSTANCE = "newInstanceWithReflection";
    /**
     * Name of the method used if we set fields via reflection
     */
    public static final String REFLECTION_SETTER_METHOD_NAME = "setWithReflection";
    /**
     * Name of the method used if we get fields via reflection
     */
    public static final String REFLECTION_GETTER_METHOD_NAME = "getWithReflection";
    /**
     * Name of the helper method for evaluating whether two arrays are equal using reflection
     */
    public static final String REFLECTION_COMPARE_ARRAYS_INNER = "_assertArraysEqualWithReflection";
    /**
     * Name of the helper method for evaluating whether two objects are equal using reflection
     */
    public static final String REFLECTION_COMPARE_OBJECTS_INNER = "_assertObjectsEqualWithReflection";
    /**
     * Name of the method used for evaluating whether two objects are equal using reflection
     */
    public static final String REFLECTION_COMPARE_OBJECTS = "assertEqualWithReflection";

    private TcgUtility() {}

    /**
     * @param s A String with a length of at least 1
     * @return A String where the first character is now lower case
     */
    public static String toFirstLower(String s) {
        return s.substring(0, 1).toLowerCase() + s.substring(1);
    }

    /**
     * @param s A String with a length of at least 1
     * @return A String where the first character is now upper case
     */
    public static String toFirstUpper(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    /**
     * @param oc Some class
     * @return true if the class is a floating point class
     */
    public static boolean isFloatingPointClass(Class<?> oc) {
        return Double.class.equals(oc) || Float.class.equals(oc) || double.class.equals(oc) || float.class.equals(oc);
    }

    /**
     * @param oc Some class
     * @return true, if the class is a Java wrapper type, else false
     */
    public static boolean isWrappingClass(Class<?> oc) {
        return Integer.class.equals(oc) || Long.class.equals(oc) || Double.class.equals(oc) || Float.class.equals(oc) ||
                Short.class.equals(oc) || Byte.class.equals(oc) || Boolean.class.equals(oc) || Character.class.equals(oc);
    }

    /**
     * @param oc Some class
     * @return true, if the class is a primitive class
     */
    public static boolean isPrimitiveClass(Class<?> oc) {
        return int.class.equals(oc) || long.class.equals(oc) || double.class.equals(oc) || float.class.equals(oc) ||
                short.class.equals(oc) || byte.class.equals(oc) || boolean.class.equals(oc) || char.class.equals(oc);
    }

    /**
     * @param oc Some class
     * @return true, if the class is a String class
     */
    public static boolean isStringClass(Class<?> oc) {
        return oc.equals(String.class);
    }
}
