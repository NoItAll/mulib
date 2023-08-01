package de.wwu.mulib.tcg;

// Taken from https://github.com/wwu-pi/muli-classpath/blob/0fad44b5cd5898f1064c558820096c1d7ae85d8e/src/main/java/de/wwu/muli/tcg/utility/Utility.java
public final class TcgUtility {

    public static final String REFLECTION_SETTER_METHOD_NAME = "setWithReflection";
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
