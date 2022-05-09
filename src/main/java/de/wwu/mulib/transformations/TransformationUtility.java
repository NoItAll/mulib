package de.wwu.mulib.transformations;

import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.substitutions.primitives.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static de.wwu.mulib.transformations.StringConstants.*;

public class TransformationUtility {
    // int = 1, long = 2, double = 3, float = 4, String = 5, byte = 6, short = 7, boolean = 8, char = 9, type = 10, array = 11
    public static final byte WR_INT = 1;
    public static final byte WR_LONG = 2;
    public static final byte WR_DOUBLE = 3;
    public static final byte WR_FLOAT = 4;
    public static final byte WR_STRING = 5;
    public static final byte WR_BYTE = 6;
    public static final byte WR_SHORT = 7;
    public static final byte WR_BOOL = 8;
    public static final byte WR_CHAR = 9;
    public static final byte WR_TYPE = 10;

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

    public static Class<?> getClassForPath(String path) {
        return getClassForName(classPathToType(path));
    }

    public static Class<?> getClassForName(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new MulibRuntimeException("Cannot locate class for String " + name, e);
        }
    }

    public static String classPathToType(String s) {
        return s.replace("/", ".");
    }

    public static String toMethodDesc(String inParameters, String returnType) {
        return "(" + inParameters + ")" + returnType;
    }

    public static String toObjDesc(String typeName) {
        return "L" + typeName + ";";
    }

    public static String cnForClass(Class<?> c) {
        return c.getName();
    }

    public static String cpForClass(Class<?> c) {
        return c.getName().replace(".", "/");
    }

    public static String cdescForClass(Class<?> c) {
        if (c.isPrimitive()) {
            throw new MulibRuntimeException("Should not occur.");
        } else {
            return "L" + cpForClass(c) + ";";
        }
    }

    public static String getOriginalDescForMulibType(String desc) {
        if (sintDesc.equals(desc)) {
            return "I";
        } else if (slongDesc.equals(desc)) {
            return "J";
        } else if (sdoubleDesc.equals(desc)) {
            return "D";
        } else if (sfloatDesc.equals(desc)) {
            return "F";
        } else if (sshortDesc.equals(desc)) {
            return "S";
        } else if (sbyteDesc.equals(desc)) {
            return "B";
        } else if (sboolDesc.equals(desc)) {
            return "Z";
        } else if (stringDesc.equals(desc)) { // TODO Free strings
            return stringDesc;
        } else if (desc.startsWith("[")) { // TODO Free arrays
            throw new NotYetImplementedException();
        } else {
            return desc.replace("/", ".").replace(_TRANSFORMATION_PREFIX, "");
        }
    }

    public static String[] getSingleDescsFromMethodParams(String methodParams) {
        if (methodParams.charAt(0) != '(' || methodParams.charAt(methodParams.length() - 1) != ')') {
            throw new MulibRuntimeException("Should be methodParams");
        }
        methodParams = methodParams.substring(1, methodParams.length() - 1);
        List<String> singleDescs = new ArrayList<>();
        for (int i = 0; i < methodParams.length();) {
            if (isPrimitive(String.valueOf(methodParams.charAt(i)))) {
                singleDescs.add(String.valueOf(methodParams.charAt(i)));
                i++;
            } else if (methodParams.charAt(i) == '[') {
                i++;
                StringBuilder sb = new StringBuilder();
                sb.append('[');
                while (methodParams.charAt(i) == '[') {
                    i++;
                    sb.append('[');
                }
                // Again check for primitive vs object
                if (isPrimitive(String.valueOf(methodParams.charAt(i)))) {
                    sb.append(methodParams.charAt(i));
                } else {
                    int endIndex = methodParams.indexOf(';', i) + 1;
                    sb.append(methodParams, i, endIndex);
                }
                singleDescs.add(sb.toString());
            } else {
                int endIndex = methodParams.indexOf(';', i) + 1;
                String desc = methodParams.substring(i, endIndex);
                singleDescs.add(desc);
                i = endIndex;
            }
        }
        return singleDescs.toArray(new String[0]);
    }

    public static String addPrefix(String addTo) {
        if (addTo == null) {
            return null;
        }
        int actualNameIndex = addTo.lastIndexOf('/') + 1;
        String packageName = addTo.substring(0, actualNameIndex);
        String actualName = addTo.substring(actualNameIndex);
        String[] innerClassSplit = actualName.split("\\$");
        StringBuilder resultBuilder = new StringBuilder(packageName);
        for (int i = 0; i < innerClassSplit.length; i++) {
            String s = innerClassSplit[i];
            resultBuilder.append(_TRANSFORMATION_PREFIX)
                    .append(s);
            if (i < innerClassSplit.length - 1) {
                resultBuilder.append('$');
            }
        }
        return resultBuilder.toString();
    }

    public static String determineClassSubstringFromDesc(String localVarDesc) {
        String descWithoutArrays = localVarDesc.replace("[", "");
        if (descWithoutArrays.length() == 1) {
            return descWithoutArrays;
        }
        // Strip "L" and ";"
        return descWithoutArrays.substring(1, descWithoutArrays.length() - 1);
    }

    public static String[] getNameAndDescriptorForConcMethodOfSPrimitiveSubclass(byte type) {
        return getNameAndDescriptorForConcMethodOfSPrimitiveSubclass(type, true);
    }

    public static String[] getNameAndDescriptorForConcMethodOfSPrimitiveSubclass(byte type, boolean useSymbolicExecution) {
        assert type < WR_TYPE : "Only primitives are allowed";
        String wrapperMethodName;
        String wrapperMethodDesc;
        String optionalSeDesc = useSymbolicExecution ? seDesc : "";
        if (type == WR_INT) {
            wrapperMethodName = concSint;
            wrapperMethodDesc = toMethodDesc("I" + optionalSeDesc, sintDesc);
        } else if (type == WR_LONG) {
            wrapperMethodName = concSlong;
            wrapperMethodDesc = toMethodDesc("J" + optionalSeDesc, slongDesc);
        } else if (type == WR_DOUBLE) {
            wrapperMethodName = concSdouble;
            wrapperMethodDesc = toMethodDesc("D" + optionalSeDesc, sdoubleDesc);
        } else if (type == WR_FLOAT) {
            wrapperMethodName = concSfloat;
            wrapperMethodDesc = toMethodDesc("F" + optionalSeDesc, sfloatDesc);
        } else if (type == WR_STRING) {
            return null; // TODO Free Strings
        } else if (type == WR_BYTE) {
            wrapperMethodName = concSbyte;
            wrapperMethodDesc = toMethodDesc("B" + optionalSeDesc, sbyteDesc);
        } else if (type == WR_SHORT) {
            wrapperMethodName = concSshort;
            wrapperMethodDesc = toMethodDesc("S" + optionalSeDesc, sshortDesc);
        } else if (type == WR_BOOL) {
            wrapperMethodName = concSbool;
            wrapperMethodDesc = toMethodDesc("Z" + optionalSeDesc, sboolDesc);
        } else if (type == WR_CHAR) { // Char
            throw new NotYetImplementedException();
        } else {
            throw new NotYetImplementedException(String.valueOf(type));
        }
        return new String[] {wrapperMethodName, wrapperMethodDesc};
    }

    public static Class<?> getClassOfWrappingType(byte type) {
        if (type == WR_INT) {
            return Sint.class;
        } else if (type == WR_LONG) {
            return Slong.class;
        } else if (type == WR_DOUBLE) {
            return Sdouble.class;
        } else if (type == WR_FLOAT) {
            return Sfloat.class;
        } else if (type == WR_STRING) {
            return String.class; // TODO support for free strings
        } else if (type == WR_BYTE) {
            return Sbyte.class;
        } else if (type == WR_SHORT) {
            return Sshort.class;
        } else if (type == WR_BOOL) {
            return Sbool.class;
        } else if (type == WR_CHAR) {
            throw new NotYetImplementedException();
        } else {
            throw new NotYetImplementedException(String.valueOf(type));
        }
    }

    public static Class<?> getClassOfType(byte type) {
        if (type == WR_INT) {
            return int.class;
        } else if (type == WR_LONG) {
            return long.class;
        } else if (type == WR_DOUBLE) {
            return double.class;
        } else if (type == WR_FLOAT) {
            return float.class;
        } else if (type == WR_STRING) {
            return String.class;
        } else if (type == WR_BYTE) {
            return byte.class;
        } else if (type == WR_SHORT) {
            return short.class;
        } else if (type == WR_BOOL) {
            return boolean.class;
        } else if (type == WR_CHAR) {
            throw new NotYetImplementedException();
        } else {
            throw new NotYetImplementedException(String.valueOf(type));
        }
    }

    public static String[] splitMethodDesc(String methodDesc) {
        int lastIndexOfParameterPart = methodDesc.lastIndexOf(')') + 1;
        String parameterPart = methodDesc.substring(0, lastIndexOfParameterPart);
        String returnPart = methodDesc.substring(lastIndexOfParameterPart);

        return new String[] {parameterPart, returnPart};
    }

    public static boolean isPrimitive(String desc) {
        return primitiveTypes.contains(desc);
    }

    public static Class<?> getPrimitiveAwareJavaWrapperClass(String desc) {
        switch (desc) {
            case "I":
                return Integer.class;
            case "J":
                return Long.class;
            case "D":
                return Double.class;
            case "F":
                return Float.class;
            case "S":
                return Short.class;
            case "B":
                return Byte.class;
            case "Z":
                return Boolean.class;
            default:
                return getClassForName(desc.substring(1, desc.length() - 1));
        }
    }
}
