package de.wwu.mulib.transformations;

import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.substitutions.primitives.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static de.wwu.mulib.transformations.StringConstants.*;
import static org.objectweb.asm.Opcodes.*;

public final class TransformationUtility {
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

    private TransformationUtility() {}

    public static boolean isStatic(int access) {
        return (access & Opcodes.ACC_STATIC) != 0;
    }

    public static int getNumInputs(MethodNode mn) {
        return getNumInputs(mn.desc, (mn.access & Opcodes.ACC_STATIC) != 0 );
    }

    public static int getNumInputs(String methodDesc, boolean isStatic) {
        return org.objectweb.asm.Type.getArgumentTypes(methodDesc).length
                + (isStatic ? 0 : 1); // Static vs not static.
    }

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

    public static MethodInsnNode getValueFromJavaWrapperMethodNode(String desc) {
        switch (desc) {
            case "I":
                return newVirtualCall("intValue", "()I", integerCp);
            case "J":
                return newVirtualCall("longValue", "()J", longCp);
            case "D":
                return newVirtualCall("doubleValue", "()D", doubleCp);
            case "F":
                return newVirtualCall("floatValue", "()F", floatCp);
            case "S":
                return newVirtualCall("shortValue", "()S", shortCp);
            case "B":
                return newVirtualCall("byteValue", "()B", byteCp);
            case "Z":
                return newVirtualCall("booleanValue", "()Z", booleanCp);
            default:
                throw new NotYetImplementedException(desc);
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

    public static String determineClassSubstringFromDesc(String localVarDesc) {
        String descWithoutArrays = localVarDesc.replace("[", "");
        if (descWithoutArrays.length() == 1) {
            return descWithoutArrays;
        }
        // Strip "L" and ";"
        return descWithoutArrays.substring(1, descWithoutArrays.length() - 1);
    }

    /* BYTECODE HELPERS */
    public static VarInsnNode loadThis() {
        return new VarInsnNode(ALOAD, 0);
    }

    public static VarInsnNode loadObjVar(int index) {
        return new VarInsnNode(ALOAD, index);
    }

    public static VarInsnNode storeSe(int seIndex) {
        return new VarInsnNode(ASTORE, seIndex);
    }

    public static AbstractInsnNode getSymbolicExecution() {
        return new MethodInsnNode(INVOKESTATIC, seCp, "get", toMethodDesc("", seDesc));
    }

    public static InsnList newVirtualSeCall(String methodName, String descriptor, int seIndex) {
        InsnList result = new InsnList();
        result.add(loadObjVar(seIndex));
        result.add(newVirtualCall(methodName, descriptor, seCp));
        return result;
    }

    public static MethodInsnNode newVirtualCall(String methodName, String descriptor, String owningClass) {
        return new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                owningClass,
                methodName,
                descriptor
        );
    }

    public static MethodInsnNode newInterfaceCall(String methodName, String descriptor, String owningClass) {
        return new MethodInsnNode(
                Opcodes.INVOKEINTERFACE,
                owningClass,
                methodName,
                descriptor
        );
    }

    public static MethodInsnNode newStaticCall(String methodName, String descriptor, String owningClass) {
        return new MethodInsnNode(
                INVOKESTATIC,
                owningClass,
                methodName,
                descriptor
        );
    }

    public static InsnList newStaticSeCall(String methodName, String descriptorName, int seIndex) {
        InsnList result = new InsnList();
        result.add(new VarInsnNode(ALOAD, seIndex));
        result.add(newStaticCall(methodName, descriptorName, seCp));
        return result;
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

    public static byte getWrappingTypeForXALOAD(InsnNode insn, TaintAnalysis ta) {
        switch (insn.getOpcode()) {
            case IALOAD:
                return WR_INT;
            case LALOAD:
                return WR_LONG;
            case DALOAD:
                return WR_DOUBLE;
            case FALOAD:
                return WR_FLOAT;
            case BALOAD:
                if (ta.instructionsToWrapSinceUsedByBoolInsns.contains(insn)) {
                    return WR_BOOL;
                } else {
                    assert ta.instructionsToWrapSinceUsedByByteInsns.contains(insn);
                    return WR_BYTE;
                }
            case CALOAD:
                return WR_CHAR;
            case SALOAD:
                return WR_SHORT;
            default:
                throw new NotYetImplementedException(String.valueOf(insn.getOpcode()));
        }
    }

    public static InsnList newConstantAndWrapper(AbstractInsnNode insn, byte type, int seIndex) {
        InsnList result = new InsnList();
        // Add instruction that should be wrapped
        result.add(insn);
        // According to the type, determine the method that should be used for concretization
        String[] wrapperMethodNameAndDesc = getNameAndDescriptorForConcMethodOfSPrimitiveSubclass(type);
        if (wrapperMethodNameAndDesc == null) {
            return result; // TODO Current workaround since free strings are not yet implemented.
        }
        result.add(
                newStaticSeCall(
                        wrapperMethodNameAndDesc[0],
                        wrapperMethodNameAndDesc[1],
                        seIndex
                )
        );

        return result;
    }

    public static byte getWrappingTypeForLdcInsn(LdcInsnNode insn) {
        if (insn.cst instanceof Integer) {
            return WR_INT;
        } else if (insn.cst instanceof Long) {
            return WR_LONG;
        } else if (insn.cst instanceof Double) {
            return WR_DOUBLE;
        } else if (insn.cst instanceof Float) {
            return WR_FLOAT;
        } else if (insn.cst instanceof String) {
            return WR_STRING;
        } else {
            if (insn.cst instanceof Type) {
                Type t = (Type) insn.cst;
                return WR_TYPE;
            } else {
                throw new NotYetImplementedException(String.valueOf(insn.cst));
            }
        }
    }

    public static byte getWrappingTypeForMethodInsn(MethodInsnNode insn) {
        switch (splitMethodDesc(insn.desc)[1].charAt(0)) {
            case 'I':
                return WR_INT;
            case 'J':
                return WR_LONG;
            case 'B':
                return WR_BYTE;
            case 'S':
                return WR_SHORT;
            case 'D':
                return WR_DOUBLE;
            case 'F':
                return WR_FLOAT;
            case 'Z':
                return WR_BOOL;
            case 'C':
                return WR_CHAR;
            case 'L':
            case '[':
                return WR_TYPE;
            default:
                throw new NotYetImplementedException(insn.owner + insn.name + insn.desc);
        }
    }

    public static byte getWrappingTypeForArithInsn(AbstractInsnNode insn) {
        switch (insn.getOpcode()) {
            case IADD:
            case ISUB:
            case IMUL:
            case IDIV:
            case IREM:
            case INEG:
                return WR_INT;
            case FADD:
            case FSUB:
            case FMUL:
            case FDIV:
            case FREM:
            case FNEG:
                return WR_FLOAT;
            case DADD:
            case DSUB:
            case DMUL:
            case DDIV:
            case DREM:
            case DNEG:
                return WR_DOUBLE;
            case LADD:
            case LSUB:
            case LMUL:
            case LDIV:
            case LREM:
            case LNEG:
                return WR_LONG;
            default:
                throw new NotYetImplementedException();
        }
    }

    public static byte getWrappingTypeForStoreInsn(AbstractInsnNode insn, TaintAnalysis ta) {
        int op = insn.getOpcode();
        if (op == ISTORE) {
            if (ta.instructionsToWrapSinceUsedByShortInsns.contains(insn)) {
                return WR_SHORT;
            } else if (ta.instructionsToWrapSinceUsedByByteInsns.contains(insn)) {
                return WR_BYTE;
            } else if (ta.instructionsToWrapSinceUsedByBoolInsns.contains(insn)) {
                return WR_BOOL;
            }
            return WR_INT;
        } else if (op == LSTORE) {
            return WR_LONG;
        } else if (op == DSTORE) {
            return WR_DOUBLE;
        } else if (op == FSTORE) {
            return WR_FLOAT;
        } else if (op == ASTORE) {
            return WR_TYPE;
        } else {
            throw new NotYetImplementedException(String.valueOf(op));
        }
    }

    public static byte getWrappingTypeForConstInsn(AbstractInsnNode insn, TaintAnalysis ta) {
        int op = insn.getOpcode();
        if (List.of(ICONST_M1, ICONST_0,ICONST_1,ICONST_2,ICONST_3,ICONST_4,ICONST_5,BIPUSH,SIPUSH).contains(op)) {
            if (ta.instructionsToWrapSinceUsedByShortInsns.contains(insn)) {
                return WR_SHORT;
            } else if (ta.instructionsToWrapSinceUsedByByteInsns.contains(insn)) {
                return WR_BYTE;
            } else if (ta.instructionsToWrapSinceUsedByBoolInsns.contains(insn)) {
                return WR_BOOL;
            }
            return WR_INT;
        } else if (op == LCONST_0 || op == LCONST_1) {
            return WR_LONG;
        } else if (op == DCONST_0 || op == DCONST_1) {
            return WR_DOUBLE;
        } else if (op == FCONST_0 || op == FCONST_1 || op == FCONST_2) {
            return WR_FLOAT;
        } else if (op == ALOAD) {
            return WR_TYPE;
        } else {
            throw new NotYetImplementedException(String.valueOf(op));
        }
    }

    public static byte getWrappingTypeForLoadInsn(AbstractInsnNode insn, TaintAnalysis ta) {
        int op = insn.getOpcode();
        if (op == ILOAD) {
            if (ta.instructionsToWrapSinceUsedByShortInsns.contains(insn)) {
                return WR_SHORT;
            } else if (ta.instructionsToWrapSinceUsedByByteInsns.contains(insn)) {
                return WR_BYTE;
            } else if (ta.instructionsToWrapSinceUsedByBoolInsns.contains(insn)) {
                return WR_BOOL;
            }
            return WR_INT;
        } else if (op == LLOAD) {
            return WR_LONG;
        } else if (op == DLOAD) {
            return WR_DOUBLE;
        } else if (op == FLOAD) {
            return WR_FLOAT;
        } else if (op == ALOAD) {
            return WR_TYPE;
        } else {
            throw new NotYetImplementedException(String.valueOf(op));
        }
    }

    public static byte getWrappingTypeForCastConversionInsn(AbstractInsnNode insn) {
        int op = insn.getOpcode();
        if (List.of(L2I, F2I, D2I).contains(op)) {
            return WR_INT;
        } else if (List.of(I2L, F2L, D2L).contains(op)) {
            return WR_LONG;
        } else if (List.of(I2D, L2D, F2D).contains(op)) {
            return WR_DOUBLE;
        } else if (List.of(I2F, L2F, D2F).contains(op)) {
            return WR_FLOAT;
        } else if (op == I2B) {
            return WR_BYTE;
        } else if (op == I2C) {
            return WR_CHAR;
        } else if (op == I2S) {
            return WR_SHORT;
        } else {
            throw new NotYetImplementedException(String.valueOf(op));
        }
    }

    public static LdcInsnNode newConstantClass(Class<?> clazz) {
        return new LdcInsnNode(org.objectweb.asm.Type.getType(clazz));
    }

    public static TypeInsnNode newCastNode(Class<?> clazz) {
        return new TypeInsnNode(Opcodes.CHECKCAST, org.objectweb.asm.Type.getInternalName(clazz));
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

    public static InsnList newSeCastCall(int op, int seIndex) {
        String castName;
        String castResultDesc;
        String castMethodOwnerCp;
        switch (op) {
            case I2L:
                castName = "i2l";
                castResultDesc = slongDesc;
                castMethodOwnerCp = sintCp;
                break;
            case I2F:
                castName = "i2f";
                castResultDesc = sfloatDesc;
                castMethodOwnerCp = sintCp;
                break;
            case I2D:
                castName = "i2d";
                castResultDesc = sdoubleDesc;
                castMethodOwnerCp = sintCp;
                break;
            case L2I:
                castName = "l2i";
                castResultDesc = sintDesc;
                castMethodOwnerCp = slongCp;
                break;
            case L2D:
                castName = "l2d";
                castResultDesc = sdoubleDesc;
                castMethodOwnerCp = slongCp;
                break;
            case L2F:
                castName = "l2f";
                castResultDesc = sfloatDesc;
                castMethodOwnerCp = slongCp;
                break;
            case D2I:
                castName = "d2i";
                castResultDesc = sintDesc;
                castMethodOwnerCp = sdoubleCp;
                break;
            case D2L:
                castName = "d2l";
                castResultDesc = slongDesc;
                castMethodOwnerCp = sdoubleCp;
                break;
            case D2F:
                castName = "d2f";
                castResultDesc = sfloatDesc;
                castMethodOwnerCp = sdoubleCp;
                break;
            case F2I:
                castName = "f2i";
                castResultDesc = sintDesc;
                castMethodOwnerCp = sfloatCp;
                break;
            case F2D:
                castName = "f2d";
                castResultDesc = sdoubleDesc;
                castMethodOwnerCp = sfloatCp;
                break;
            case F2L:
                castName = "f2l";
                castResultDesc = slongDesc;
                castMethodOwnerCp = sfloatCp;
                break;
            case I2B:
                castName = "i2b";
                castResultDesc = sbyteDesc;
                castMethodOwnerCp = sintCp;
                break;
            case I2S:
                castName = "i2s";
                castResultDesc = sshortDesc;
                castMethodOwnerCp = sintCp;
                break;
            default:
                throw new NotYetImplementedException(String.valueOf(op));
        }
        InsnList result = new InsnList();
        result.add(loadObjVar(seIndex));
        result.add(newVirtualCall(castName, toMethodDesc(seDesc, castResultDesc), castMethodOwnerCp));
        return result;
    }

    public static MethodInsnNode newSeArithOp(
            int op, int dadd, int dsub, int dmul, int ddiv, int drem, int dneg,
            String singlesdoubleDesc, String sdoubleArithDesc, String sdoubleCp) {
        return newVirtualCall(
                op == dadd ? add : op == dsub ? sub : op == dmul ? mul : op == ddiv ? div : op == drem ? rem : neg,
                op == dneg ? singlesdoubleDesc : sdoubleArithDesc,
                sdoubleCp
        );
    }


    /* DEVELOPMENT CHECKS */
    public static void throwExceptionIfNotEmpty(Object o) {
        if (o instanceof List) {
            if (((List<?>) o).size() != 0) {
                throw new NotYetImplementedException();
            }
        } else if (o != null) {
            throw new NotYetImplementedException();
        }
    }

    public static void checkMethodNode(MethodNode mn) {
//        throwExceptionIfNotEmpty(mn.attrs);
//        throwExceptionIfNotEmpty(mn.annotationDefault);
//        throwExceptionIfNotEmpty(mn.invisibleAnnotations);
//        throwExceptionIfNotEmpty(mn.invisibleTypeAnnotations);
//        throwExceptionIfNotEmpty(mn.invisibleLocalVariableAnnotations);
//        throwExceptionIfNotEmpty(mn.visibleAnnotations);
//        throwExceptionIfNotEmpty(mn.visibleTypeAnnotations);
//        throwExceptionIfNotEmpty(mn.visibleLocalVariableAnnotations);
//        throwExceptionIfNotEmpty(mn.invisibleParameterAnnotations);
//        throwExceptionIfNotEmpty(mn.visibleParameterAnnotations);
//        throwExceptionIfNotEmpty(mn.parameters);
    }

    public static String insnToString(AbstractInsnNode ain) {
        int op = ain.getOpcode();
        String opname;
        if (-1 == op && ain.getType() == AbstractInsnNode.LABEL) {
            opname = "LABEL";
        } else if (-1 == op && ain.getType() == AbstractInsnNode.LINE) {
            opname = "LINENUMBER";
        } else if (-1 == op && ain.getType() == AbstractInsnNode.FRAME) {
            opname = "FRAME";
        } else if (NOP == op) {
            opname = "NOP";
        } else if (ACONST_NULL == op) {
            opname = "ACONST_NULL";
        } else if (ICONST_M1 == op) {
            opname = "ICONST_M1";
        } else if (ICONST_0 == op) {
            opname = "ICONST_0";
        } else if (ICONST_1 == op) {
            opname = "ICONST_1";
        } else if (ICONST_2 == op) {
            opname = "ICONST_2";
        } else if (ICONST_3 == op) {
            opname = "ICONST_3";
        } else if (ICONST_4 == op) {
            opname = "ICONST_4";
        } else if (ICONST_5 == op) {
            opname = "ICONST_5";
        } else if (LCONST_0 == op) {
            opname = "LCONST_0";
        } else if (LCONST_1 == op) {
            opname = "LCONST_1";
        } else if (FCONST_0 == op) {
            opname = "FCONST_0";
        } else if (FCONST_1 == op) {
            opname = "FCONST_1";
        } else if (FCONST_2 == op) {
            opname = "FCONST_2";
        } else if (DCONST_0 == op) {
            opname = "DCONST_0";
        } else if (DCONST_1 == op) {
            opname = "DCONST_1";
        } else if (BIPUSH == op) {
            opname = "BIPUSH";
        } else if (SIPUSH == op) {
            opname = "SIPUSH";
        } else if (LDC == op) {
            opname = "LDC";
        } else if (ILOAD == op) {
            opname = "ILOAD";
        } else if (LLOAD == op) {
            opname = "LLOAD";
        } else if (FLOAD == op) {
            opname = "FLOAD";
        } else if (DLOAD == op) {
            opname = "DLOAD";
        } else if (ALOAD == op) {
            opname = "ALOAD";
        } else if (IALOAD == op) {
            opname = "IALOAD";
        } else if (LALOAD == op) {
            opname = "LALOAD";
        } else if (FALOAD == op) {
            opname = "FALOAD";
        } else if (DALOAD == op) {
            opname = "DALOAD";
        } else if (AALOAD == op) {
            opname = "AALOAD";
        } else if (BALOAD == op) {
            opname = "BALOAD";
        } else if (CALOAD == op) {
            opname = "CALOAD";
        } else if (SALOAD == op) {
            opname = "SALOAD";
        } else if (ISTORE == op) {
            opname = "ISTORE";
        } else if (LSTORE == op) {
            opname = "LSTORE";
        } else if (FSTORE == op) {
            opname = "FSTORE";
        } else if (DSTORE == op) {
            opname = "DSTORE";
        } else if (ASTORE == op) {
            opname = "ASTORE";
        } else if (IASTORE == op) {
            opname = "IASTORE";
        } else if (LASTORE == op) {
            opname = "LASTORE";
        } else if (FASTORE == op) {
            opname = "FASTORE";
        } else if (DASTORE == op) {
            opname = "DASTORE";
        } else if (AASTORE == op) {
            opname = "AASTORE";
        } else if (BASTORE == op) {
            opname = "BASTORE";
        } else if (CASTORE == op) {
            opname = "CASTORE";
        } else if (SASTORE == op) {
            opname = "SASTORE";
        } else if (POP == op) {
            opname = "POP";
        } else if (POP2 == op) {
            opname = "POP2";
        } else if (DUP == op) {
            opname = "DUP";
        } else if (DUP_X1 == op) {
            opname = "DUP_X1";
        } else if (DUP_X2 == op) {
            opname = "DUP_X2";
        } else if (DUP2 == op) {
            opname = "DUP2";
        } else if (DUP2_X1 == op) {
            opname = "DUP2_X1";
        } else if (DUP2_X2 == op) {
            opname = "DUP2_X2";
        } else if (SWAP == op) {
            opname = "SWAP";
        } else if (IADD == op) {
            opname = "IADD";
        } else if (LADD == op) {
            opname = "LADD";
        } else if (FADD == op) {
            opname = "FADD";
        } else if (DADD == op) {
            opname = "DADD";
        } else if (ISUB == op) {
            opname = "ISUB";
        } else if (LSUB == op) {
            opname = "LSUB";
        } else if (FSUB == op) {
            opname = "FSUB";
        } else if (DSUB == op) {
            opname = "DSUB";
        } else if (IMUL == op) {
            opname = "IMUL";
        } else if (LMUL == op) {
            opname = "LMUL";
        } else if (FMUL == op) {
            opname = "FMUL";
        } else if (DMUL == op) {
            opname = "DMUL";
        } else if (IDIV == op) {
            opname = "DIV";
        } else if (LDIV == op) {
            opname = "LDIV";
        } else if (FDIV == op) {
            opname = "FDIV";
        } else if (DDIV == op) {
            opname = "DDIV";
        } else if (IREM == op) {
            opname = "IREM";
        } else if (LREM == op) {
            opname = "LREM";
        } else if (FREM == op) {
            opname = "FREM";
        } else if (DREM == op) {
            opname = "DREM";
        } else if (INEG == op) {
            opname = "INEG";
        } else if (LNEG == op) {
            opname = "LNEG";
        } else if (FNEG == op) {
            opname = "FNEG";
        } else if (DNEG == op) {
            opname = "DNEG";
        } else if (ISHL == op) {
            opname = "ISHL";
        } else if (LSHL == op) {
            opname = "LSHL";
        } else if (ISHR == op) {
            opname = "ISHR";
        } else if (LSHR == op) {
            opname = "LSHR";
        } else if (IUSHR == op) {
            opname = "IUSHR";
        } else if (LUSHR == op) {
            opname = "LUSHR";
        } else if (IAND == op) {
            opname = "IAND";
        } else if (LAND == op) {
            opname = "LAND";
        } else if (IOR == op) {
            opname = "IOR";
        } else if (LOR == op) {
            opname = "LOR";
        } else if (IXOR == op) {
            opname = "IXOR";
        } else if (LXOR == op) {
            opname = "LXOR";
        } else if (IINC == op) {
            opname = "IINC";
        } else if (I2L == op) {
            opname = "I2L";
        } else if (I2F == op) {
            opname = "I2F";
        } else if (I2D == op) {
            opname = "I2D";
        } else if (L2I == op) {
            opname = "L2I";
        } else if (L2F == op) {
            opname = "L2F";
        } else if (L2D == op) {
            opname = "L2D";
        } else if (F2I == op) {
            opname = "F2I";
        } else if (F2L == op) {
            opname = "F2L";
        } else if (F2D == op) {
            opname = "F2D";
        } else if (D2I == op) {
            opname = "D2I";
        } else if (D2L == op) {
            opname = "D2L";
        } else if (D2F == op) {
            opname = "D2F";
        } else if (I2B == op) {
            opname = "I2B";
        } else if (I2C == op) {
            opname = "I2C";
        } else if (I2S == op) {
            opname = "I2S";
        } else if (LCMP == op) {
            opname = "LCMP";
        } else if (FCMPL == op) {
            opname = "FCMPL";
        } else if (FCMPG == op) {
            opname = "FCMPG";
        } else if (DCMPL == op) {
            opname = "DCMPL";
        } else if (DCMPG == op) {
            opname = "DCMPG";
        } else if (IFEQ == op) {
            opname = "IFEG";
        } else if (IFNE == op) {
            opname = "IFNE";
        } else if (IFLT == op) {
            opname = "IFLT";
        } else if (IFGE == op) {
            opname = "IFGE";
        } else if (IFGT == op) {
            opname = "IFGT";
        } else if (IFLE == op) {
            opname = "IFLE";
        } else if (IF_ICMPEQ == op) {
            opname = "IF_ICMPEQ";
        } else if (IF_ICMPNE == op) {
            opname = "IF_ICMPNE";
        } else if (IF_ICMPLT == op) {
            opname = "IF_ICMPLT";
        } else if (IF_ICMPGE == op) {
            opname = "IF_ICMPGE";
        } else if (IF_ICMPGT == op) {
            opname = "IF_ICMPGT";
        } else if (IF_ICMPLE == op) {
            opname = "IF_ICMPLE";
        } else if (IF_ACMPEQ == op) {
            opname = "IF_ACMPEG";
        } else if (IF_ACMPNE == op) {
            opname = "IF_ACMPNE";
        } else if (GOTO == op) {
            opname = "GOTO";
        } else if (JSR == op) {
            opname = "JSR";
        } else if (RET == op) {
            opname = "RET";
        } else if (TABLESWITCH == op) {
            opname = "TABLESWITCH";
        } else if (LOOKUPSWITCH == op) {
            opname = "LOOKUPSWITCH";
        } else if (IRETURN == op) {
            opname = "IRETURN";
        } else if (LRETURN == op) {
            opname = "LRETURN";
        } else if (FRETURN == op) {
            opname = "FRETURN";
        } else if (DRETURN == op) {
            opname = "DRETURN";
        } else if (ARETURN == op) {
            opname = "ARETURN";
        } else if (RETURN == op) {
            opname = "RETURN";
        } else if (GETSTATIC == op) {
            opname = "GETSTATIC";
        } else if (PUTSTATIC == op) {
            opname = "PUTSTATIC";
        } else if (GETFIELD == op) {
            opname = "GETFIELD";
        } else if (PUTFIELD == op) {
            opname = "PUTFIELD";
        } else if (INVOKEVIRTUAL == op) {
            opname = "INVOKEVIRTUAL";
        } else if (INVOKESPECIAL == op) {
            opname = "INVOKESPECIAL";
        } else if (INVOKESTATIC == op) {
            opname = "INVOKESTATIC";
        } else if (INVOKEINTERFACE == op) {
            opname = "INVOKEINTERFACE";
        } else if (INVOKEDYNAMIC == op) {
            opname = "INVOKEDYNAMIC";
        } else if (NEW == op) {
            opname = "NEW";
        } else if (NEWARRAY == op) {
            opname = "NEWARRAY";
        } else if (ANEWARRAY == op) {
            opname = "ANEWARRAY";
        } else if (ARRAYLENGTH == op) {
            opname = "ARRAYLENGTH";
        } else if (ATHROW == op) {
            opname = "ATHROW";
        } else if (CHECKCAST == op) {
            opname = "CHECKCAST";
        } else if (INSTANCEOF == op) {
            opname = "INSTANCEOF";
        } else if (MONITORENTER == op) {
            opname = "MONITORENTER";
        } else if (MONITOREXIT == op) {
            opname = "MONITOREXIT";
        } else if (MULTIANEWARRAY == op) {
            opname = "MULTIANEWARRAY";
        } else if (IFNULL == op) {
            opname = "IFNULL";
        } else if (IFNONNULL == op) {
            opname = "IFNONNULL";
        } else {
            throw new NotYetImplementedException(String.valueOf(op));
        }
        String additions = "";
        if (ain instanceof LabelNode) {
            additions = ((LabelNode) ain).getLabel().toString();
        } else if (ain instanceof MethodInsnNode) {
            additions = ((MethodInsnNode) ain).owner + "." + ((MethodInsnNode) ain).name + ((MethodInsnNode) ain).desc;
        } else if (ain instanceof FieldInsnNode) {
            additions = ((FieldInsnNode) ain).owner + "." + ((FieldInsnNode) ain).name + ((FieldInsnNode) ain).desc;
        } else if (ain instanceof IincInsnNode) {
            additions = ((IincInsnNode) ain).var + " " + ((IincInsnNode) ain).incr;
        } else if (ain instanceof IntInsnNode) {
            additions = String.valueOf(((IntInsnNode) ain).operand);
        } else if (ain instanceof LdcInsnNode) {
            additions = String.valueOf(((LdcInsnNode) ain).cst);
        } else if (ain instanceof TypeInsnNode) {
            additions = ((TypeInsnNode) ain).desc;
        } else if (ain instanceof InvokeDynamicInsnNode) {
            additions = ((InvokeDynamicInsnNode) ain).name + ((InvokeDynamicInsnNode) ain).desc
                    + " BSM: " + ((InvokeDynamicInsnNode) ain).bsm.getOwner() + ((InvokeDynamicInsnNode) ain).bsm.getName()
                    + ((InvokeDynamicInsnNode) ain).bsm.getDesc() + Arrays.toString(((InvokeDynamicInsnNode) ain).bsmArgs);
        } else if (ain instanceof JumpInsnNode) {
            additions = insnToString(((JumpInsnNode) ain).label);
        } else if (ain instanceof VarInsnNode) {
            additions = String.valueOf(((VarInsnNode) ain).var);
        } else if (ain instanceof FrameNode) {
            additions = ((FrameNode) ain).local + " " + ((FrameNode) ain).stack;
        }

        return opname + " " + additions;
    }

    public static String getBytecode(Iterable<AbstractInsnNode> ains) {
        StringBuilder sb = new StringBuilder();

        for (AbstractInsnNode ain : ains) {
            sb.append("\t").append(insnToString(ain)).append("\r\n");
        }
        
        return sb.toString();
    }

    public static String getBytecodeForMethodNode(MethodNode mn) {
        StringBuilder sb = new StringBuilder();
        sb.append(mn.name).append(mn.desc).append("\r\n");
        for (AbstractInsnNode ain : mn.instructions) {
            sb.append("\t").append(insnToString(ain)).append("\r\n");
        }
        sb.append("\r\n\r\n");
        return sb.toString();
    }

    public static String getBytecodeForClassNodeMethods(ClassNode cn) {
        StringBuilder sb = new StringBuilder();
        sb.append(cn.name).append("\r\n\r\n");
        for (MethodNode mn : cn.methods) {
            sb.append(getBytecodeForMethodNode(mn));
        }
        return sb.toString();
    }
}
