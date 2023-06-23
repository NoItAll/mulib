package de.wwu.mulib;

import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.search.trees.PathSolution;

import java.util.List;
import java.util.Optional;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Mulib {

    public static final Logger log = Logger.getLogger("mulib");

    static {
        assert log.getHandlers().length == 0;
        log.setUseParentHandlers(false);
        Handler h = new ConsoleHandler();
        log.addHandler(h);
    }

    public synchronized static void setLogLevel(Level newLevel) {
        log.setLevel(newLevel);
        assert log.getHandlers().length == 1;
        log.getHandlers()[0].setLevel(newLevel);
    }

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
        return executeMulibForOne(methodName, methodOwnerClass, mb, null, args);
    }

    public static MulibContext getMulibContext(Class<?> methodOwnerClass, String methodName, MulibConfig.MulibConfigBuilder mb, Class<?>... argTypes) {
        return generateMulibContext(methodName, methodOwnerClass, argTypes, null, mb.build());
    }

    public static MulibContext getMulibContext(String methodName, Class<?> methodOwnerClass, MulibConfig.MulibConfigBuilder mb, Object... prototypicalArgs) {
        return generateMulibContext(methodName, methodOwnerClass, null, prototypicalArgs, mb.build());
    }

    public static MulibContext getMulibContextWithoutTransformation(String methodName, Class<?> methodOwnerClass, MulibConfig.MulibConfigBuilder mb, Class<?>[] argTypes, Object[] prototypicalArgs) {
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

    public static int freeInt(int lb, int ub) {
        throw _shouldHaveBeenReplaced();
    }

    public static long freeLong(long lb, long ub) {
        throw _shouldHaveBeenReplaced();
    }

    public static double freeDouble(double lb, double ub) {
        throw _shouldHaveBeenReplaced();
    }

    public static float freeFloat(float lb, float ub) {
        throw _shouldHaveBeenReplaced();
    }

    public static short freeShort(short lb, short ub) {
        throw _shouldHaveBeenReplaced();
    }

    public static byte freeByte(byte lb, byte ub) {
        throw _shouldHaveBeenReplaced();
    }

    public static char freeChar(char lb, char ub) {
        throw _shouldHaveBeenReplaced();
    }

    public static void assume(boolean b) {
        throw _shouldHaveBeenReplaced();
    }

    public static boolean check(boolean b) {
        throw _shouldHaveBeenReplaced();
    }

    public static boolean checkAssume(boolean b) {
        throw _shouldHaveBeenReplaced();
    }

    public static <T> T freeObject(Class<T> objClass) {
        throw _shouldHaveBeenReplaced();
    }

    @SafeVarargs
    public static <T> T pickFrom(T... aliasingTargets) {
        throw _shouldHaveBeenReplaced();
    }

    public static int pickFrom(int... aliasingTargets) {
        throw _shouldHaveBeenReplaced();
    }

    public static long pickFrom(long... aliasingTargets) {
        throw _shouldHaveBeenReplaced();
    }

    public static double pickFrom(double... aliasingTargets) {
        throw _shouldHaveBeenReplaced();
    }

    public static float pickFrom(float... aliasingTargets) {
        throw _shouldHaveBeenReplaced();
    }

    public static short pickFrom(short... aliasingTargets) {
        throw _shouldHaveBeenReplaced();
    }

    public static byte pickFrom(byte... aliasingTargets) {
        throw _shouldHaveBeenReplaced();
    }

    public static boolean pickFrom(boolean... aliasingTargets) {
        throw _shouldHaveBeenReplaced();
    }

    public static char pickFrom(char... aliasingTargets) {
        throw _shouldHaveBeenReplaced();
    }

    public static <T> T[] freeObjectArray(int length, Class<T> clazz) {
        throw _shouldHaveBeenReplaced();
    }

    public static void remember(int i, String id) {
        throw _shouldHaveBeenReplaced();
    }

    public static void remember(long i, String id) {
        throw _shouldHaveBeenReplaced();
    }

    public static void remember(double i, String id) {
        throw _shouldHaveBeenReplaced();
    }

    public static void remember(float i, String id) {
        throw _shouldHaveBeenReplaced();
    }

    public static void remember(short i, String id) {
        _shouldHaveBeenReplaced();
    }

    public static void remember(byte i, String id) {
        throw _shouldHaveBeenReplaced();
    }

    public static void remember(boolean i, String id) {
        throw _shouldHaveBeenReplaced();
    }

    public static void remember(char i, String id) {
        throw _shouldHaveBeenReplaced();
    }

    public static void remember(Object i, String id) {
        throw _shouldHaveBeenReplaced();
    }

    public static int rememberedFreeInt(String identifier) {
        throw _shouldHaveBeenReplaced();
    }

    public static long rememberedFreeLong(String identifier) {
        throw _shouldHaveBeenReplaced();
    }

    public static double rememberedFreeDouble(String identifier) {
        throw _shouldHaveBeenReplaced();
    }

    public static float rememberedFreeFloat(String identifier) {
        throw _shouldHaveBeenReplaced();
    }

    public static short rememberedFreeShort(String identifier) {
        throw _shouldHaveBeenReplaced();
    }

    public static byte rememberedFreeByte(String identifier) {
        throw _shouldHaveBeenReplaced();
    }

    public static boolean rememberedFreeBoolean(String identifier) {
        throw _shouldHaveBeenReplaced();
    }

    public static char rememberedFreeChar(String identifier) {
        throw _shouldHaveBeenReplaced();
    }

    public static int rememberedFreeInt(String identifier, int lb, int ub) {
        throw _shouldHaveBeenReplaced();
    }

    public static long rememberedFreeLong(String identifier, long lb, long ub) {
        throw _shouldHaveBeenReplaced();
    }

    public static double rememberedFreeDouble(String identifier, double lb, double ub) {
        throw _shouldHaveBeenReplaced();
    }

    public static float rememberedFreeFloat(String identifier, float lb, float ub) {
        throw _shouldHaveBeenReplaced();
    }

    public static short rememberedFreeShort(String identifier, short lb, short ub) {
        throw _shouldHaveBeenReplaced();
    }

    public static byte rememberedFreeByte(String identifier, byte lb, byte ub) {
        throw _shouldHaveBeenReplaced();
    }

    public static char rememberedFreeChar(String identifier, char lb, char ub) {
        throw _shouldHaveBeenReplaced();
    }

    public static <T> T rememberedFreeObject(String identifier, Class<T> objClass) {
        throw _shouldHaveBeenReplaced();
    }

    public static boolean isInSearch() {
        throw _shouldHaveBeenReplaced();
    }

    private static MulibRuntimeException _shouldHaveBeenReplaced() {
        return new MulibRuntimeException("This method should always be replaced by a code transformation.");
    }
}
