package de.wwu.mulib;

import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.transformations.MulibValueTransformer;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public final class Mulib {

    public static final Logger log = Logger.getLogger("mulib");

    private Mulib() {}

    public static List<PathSolution> executeMulib(
            String methodName,
            Class<?> mainClass,
            MulibConfig.MulibConfigBuilder mb,
            Object... args) {
        return executeMulib(methodName, mainClass, mb, null, args);
    }

    public static List<PathSolution> executeMulib(
            String methodName,
            Class<?> methodOwnerClass,
            MulibConfig.MulibConfigBuilder mb,
            Class<?>[] argTypes,
            Object... args) {
        return generateMulibContext(methodName, methodOwnerClass, argTypes, args, mb.build()).getAllPathSolutions(args);
    }

    public static Optional<PathSolution> executeMulibForOne(String methodName, Class<?> methodOwnerClass, MulibConfig.MulibConfigBuilder mb, Class<?>[] argTypes, Object... args) {
        return generateMulibContext(methodName, methodOwnerClass, argTypes, args, mb.build()).getPathSolution(args);
    }

    public static Optional<PathSolution> executeMulibForOne(String methodName, Class<?> methodOwnerClass, MulibConfig.MulibConfigBuilder mb, Object... args) {
        Class<?>[] argTypes = Arrays.stream(args).map(Object::getClass).toArray(Class<?>[]::new);
        return executeMulibForOne(methodName, methodOwnerClass, mb, argTypes, args);
    }

    public static MulibContext getMulibContext(Class<?> methodOwnerClass, String methodName, MulibConfig.MulibConfigBuilder mb, Class<?>[] argTypes) {
        return generateMulibContext(methodName, methodOwnerClass, argTypes, null, mb.build());
    }

    public static MulibContext getMulibContext(String methodName, Class<?> methodOwnerClass, MulibConfig.MulibConfigBuilder mb, Object[] prototypicalArgs) {
        return generateMulibContext(methodName, methodOwnerClass, null, prototypicalArgs, mb.build());
    }

    public static MulibContext getMulibContextWithoutTransformation(String methodName, Class<?> methodOwnerClass, MulibConfig.MulibConfigBuilder mb, Class<?>[] argTypes, Object... prototypicalArgs) {
        mb.setTRANSF_TRANSFORMATION_REQUIRED(false);
        return generateMulibContext(methodName, methodOwnerClass, argTypes, prototypicalArgs, mb.build());
    }

    private static MulibContext generateMulibContext(
            String methodName,
            Class<?> methodOwnerClass,
            Class<?>[] argTypes,
            Object[] args,
            MulibConfig config) {
        return new MulibContext(methodName, methodOwnerClass, config, argTypes, args);
    }

    public static Fail fail() {
        return new Fail();
    }

    /* INITIALIZATIONS OF FREE VARIABLES. THESE METHODS ARE INDICATORS TO BE REPLACED BY A CODE TRANSFORMATION */
    public static int freeInt() {
        throw _shouldHaveBeenReplaced();
    }

    public static long freeLong() {
        throw _shouldHaveBeenReplaced();
    }

    public static double freeDouble() {
        throw _shouldHaveBeenReplaced();
    }

    public static float freeFloat() {
        throw _shouldHaveBeenReplaced();
    }

    public static short freeShort() {
        throw _shouldHaveBeenReplaced();
    }

    public static byte freeByte() {
        throw _shouldHaveBeenReplaced();
    }

    public static boolean freeBoolean() {
        throw _shouldHaveBeenReplaced();
    }

    public static char freeChar() {
        throw _shouldHaveBeenReplaced();
    }

    public static void assume(boolean b) {
        throw _shouldHaveBeenReplaced();
    }

    public static <T> T freeObject(Class<T> objClass) {
        throw _shouldHaveBeenReplaced();
    }

    public static int[] freeIntArray() {
        throw _shouldHaveBeenReplaced();
    }

    public static long[] freeLongArray() {
        throw _shouldHaveBeenReplaced();
    }

    public static double[] freeDoubleArray() {
        throw _shouldHaveBeenReplaced();
    }

    public static float[] freeFloatArray() {
        throw _shouldHaveBeenReplaced();
    }

    public static short[] freeShortArray() {
        throw _shouldHaveBeenReplaced();
    }

    public static byte[] freeByteArray() {
        throw _shouldHaveBeenReplaced();
    }

    public static boolean[] freeBooleanArray() {
        throw _shouldHaveBeenReplaced();
    }

    public static int namedFreeInt(String identifier) {
        throw _shouldHaveBeenReplaced();
    }

    public static long namedFreeLong(String identifier) {
        throw _shouldHaveBeenReplaced();
    }

    public static double namedFreeDouble(String identifier) {
        throw _shouldHaveBeenReplaced();
    }

    public static float namedFreeFloat(String identifier) {
        throw _shouldHaveBeenReplaced();
    }

    public static short namedFreeShort(String identifier) {
        throw _shouldHaveBeenReplaced();
    }

    public static byte namedFreeByte(String identifier) {
        throw _shouldHaveBeenReplaced();
    }

    public static boolean namedFreeBoolean(String identifier) {
        throw _shouldHaveBeenReplaced();
    }

    public static char namedFreeChar(String identifier) {
        throw _shouldHaveBeenReplaced();
    }

    public static <T> T namedFreeObject(String identifier, Class<T> objClass) {
        throw _shouldHaveBeenReplaced();
    }

    public static int[] namedFreeIntArray(String identifier) {
        throw _shouldHaveBeenReplaced();
    }

    public static long[] namedFreeLongArray(String identifier) {
        throw _shouldHaveBeenReplaced();
    }

    public static double[] namedFreeDoubleArray(String identifier) {
        throw _shouldHaveBeenReplaced();
    }

    public static float[] namedFreeFloatArray(String identifier) {
        throw _shouldHaveBeenReplaced();
    }

    public static short[] namedFreeShortArray(String identifier) {
        throw _shouldHaveBeenReplaced();
    }

    public static byte[] namedFreeByteArray(String identifier) {
        throw _shouldHaveBeenReplaced();
    }

    public static boolean[] namedFreeBooleanArray(String identifier) {
        throw _shouldHaveBeenReplaced();
    }

    private static MulibRuntimeException _shouldHaveBeenReplaced() {
        return new MulibRuntimeException("This method should always be replaced by a code transformation.");
    }

    public static Object transformToMulibPartnerClass(Object toTransform, MulibValueTransformer mulibValueTransformer) {
        if (toTransform == null) {
            return null;
        } else if (mulibValueTransformer == null) {
            throw new MulibRuntimeException("MulibValueTransformer must not be null.");
        }

        try {
            Object result = toTransform
                    .getClass()
                    .getConstructor(toTransform.getClass(), mulibValueTransformer.getClass())
                    .newInstance(toTransform, mulibValueTransformer);
            return result;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
            throw new MulibRuntimeException(e);
        }
    }
}
