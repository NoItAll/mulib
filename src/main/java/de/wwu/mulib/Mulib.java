package de.wwu.mulib;

import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.solving.Solution;
import de.wwu.mulib.tcg.TcgConfig;
import de.wwu.mulib.throwables.MulibRuntimeError;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The access point of users of Mulib.
 * Contains all frontend methods needed to create a search region and call it from another class.
 * For calling the same search region with the same configuration repeatedly, e.g. with different arguments,
 * consider extracting and using {@link MulibContext} instead.
 * Behind the scenes, {@link MulibContext} is used anyway.
 */
public final class Mulib {

    public static final Logger log = Logger.getLogger("mulib");

    static {
        assert log.getHandlers().length == 0;
        log.setUseParentHandlers(false);
        Handler h = new ConsoleHandler();
        log.addHandler(h);
    }

    /**
     * Set the log level to be used by the logger in Mulib.log.
     * @param newLevel The new log level
     */
    public synchronized static void setLogLevel(Level newLevel) {
        log.setLevel(newLevel);
        assert log.getHandlers().length == 1;
        log.getHandlers()[0].setLevel(newLevel);
    }

    private Mulib() {}

    /**
     * Executes mulib with a configuration build from the given config builder. All PathSolutions that can be reached
     * (respecting the budgets) are extracted
     * @param methodName The name of the method that is the search region. The method should be static and accessible
     * @param methodOwnerClass The class containing the search region named according to methodName
     * @param mb The config builder from which an instance of MulibConfig is built
     * @param args The arguments passed to the search region. The types are deduced from the arguments. These types
     *             are needed to find the appropriate method with
     * @return The list of path solutions
     */
    public static List<PathSolution> getPathSolutions(
            String methodName,
            Class<?> methodOwnerClass,
            MulibConfig.MulibConfigBuilder mb,
            Object... args) {
        return getPathSolutions(methodName, methodOwnerClass, mb, null, args);
    }

    /**
     * Executes mulib with a configuration build from the given config builder. All PathSolutions that can be reached
     * (respecting the budgets) are extracted
     * @param methodName The name of the method that is the search region. The method should be static and accessible
     * @param methodOwnerClass The class containing the search region named according to methodName
     * @param mb The config builder from which an instance of MulibConfig is built
     * @param args The arguments passed to the search region
     * @param argTypes The explicitly passed argument types to find the appropriate method with
     * @return The list of path solutions
     */
    public static List<PathSolution> getPathSolutions(
            String methodName,
            Class<?> methodOwnerClass,
            MulibConfig.MulibConfigBuilder mb,
            Class<?>[] argTypes,
            Object... args) {
        return generateMulibContext(methodName, methodOwnerClass, argTypes, args, mb.build()).getPathSolutions(args);
    }

    /**
     * Generates a String representing a class for testing a method identified by methodName and methodOwnerClass.
     * @param methodName The method of the driver method for calling the method under test
     * @param methodOwnerClass The class declaring the driver
     * @param mb The config builder from which an instance of MulibConfig is built
     * @param argTypes The explicitly passed argument types to find the appropriate method with
     * @param args The arguments passed to the search region
     * @param methodUnderTest The method under test which will be called with the arguments defined in the driver
     * @param tcgConfigBuilder The configuration for generating test cases
     * @return A String representing a test class with tests for the method under test
     */
    public static String generateTestCases(
            String methodName,
            Class<?> methodOwnerClass,
            MulibConfig.MulibConfigBuilder mb,
            Class<?>[] argTypes,
            Object[] args,
            Method methodUnderTest,
            TcgConfig.TcgConfigBuilder tcgConfigBuilder) {
        return generateMulibContext(methodName, methodOwnerClass, argTypes, args, mb.build()).generateTestCases(methodUnderTest, tcgConfigBuilder, args);
    }

    /**
     * A simplification of {@link Mulib#generateTestCases(String, Class, MulibConfig.MulibConfigBuilder, Class[], Object[], Method, TcgConfig.TcgConfigBuilder)}
     * where we assume a driver method without any parameters
     * @param methodName The method of the driver method for calling the method under test
     * @param methodOwnerClass The class declaring the driver
     * @param mb The config builder from which an instance of MulibConfig is built
     * @param methodUnderTest The method under test which will be called with the arguments defined in the driver
     * @param tcgConfigBuilder The configuration for generating test cases
     * @return A String representing a test class with tests for the method under test
     */
    public static String generateTestCases(
            String methodName,
            Class<?> methodOwnerClass,
            MulibConfig.MulibConfigBuilder mb,
            Method methodUnderTest,
            TcgConfig.TcgConfigBuilder tcgConfigBuilder) {
        return generateTestCases(methodName, methodOwnerClass, mb, new Class[0], new Object[0], methodUnderTest, tcgConfigBuilder);
    }

    /**
     * Extracts a single path solution from the search region. See {@link Mulib#getPathSolutions(String, Class, MulibConfig.MulibConfigBuilder, Class[], Object...)}
     * for more details
     * @param methodName The method of the driver method for calling the method under test
     * @param methodOwnerClass The class declaring the driver
     * @param mb The config builder from which an instance of MulibConfig is built
     * @param argTypes The explicitly passed argument types to find the appropriate method with
     * @param args The arguments passed to the search region
     * @return A path solution, if any can be extracted
     */
    public static Optional<PathSolution> getSinglePathSolution(String methodName, Class<?> methodOwnerClass, MulibConfig.MulibConfigBuilder mb, Class<?>[] argTypes, Object... args) {
        return generateMulibContext(methodName, methodOwnerClass, argTypes, args, mb.build()).getPathSolution(args);
    }

    /**
     * Extracts a single path solution from the search region. See {@link Mulib#getPathSolutions(String, Class, MulibConfig.MulibConfigBuilder, Object...)}
     * for more details
     * @param methodName The method of the driver method for calling the method under test
     * @param methodOwnerClass The class declaring the driver
     * @param mb The config builder from which an instance of MulibConfig is built
     * @param args The arguments passed to the search region
     * @return A path solution, if any can be extracted
     */
    public static Optional<PathSolution> getSinglePathSolution(String methodName, Class<?> methodOwnerClass, MulibConfig.MulibConfigBuilder mb, Object... args) {
        return getSinglePathSolution(methodName, methodOwnerClass, mb, null, args);
    }

    /**
     * Extracts, if possible given the budget and search problem, N solutions. Consider using {@link MulibContext#getUpToNSolutions(int, Object...)}
     * for a reusable access.
     * Assumes that there are not arguments required for the search region.
     * @param methodName The name of the method that is the search region. The method should be static and accessible
     * @param methodOwnerClass The class containing the search region named according to methodName
     * @param mb The config builder from which an instance of MulibConfig is built
     * @param N The number of requested solutions
     * @return The solutions
     */
    public static List<Solution> getUpToNSolutions(Class<?> methodOwnerClass, String methodName, MulibConfig.MulibConfigBuilder mb, int N) {
        return getUpToNSolutions(methodOwnerClass, methodName, mb, N, new Class[0], new Object[0]);
    }

    /**
     * Extracts, if possible given the budget and search problem, N solutions. Consider using {@link MulibContext#getUpToNSolutions(int, Object...)}
     * for a reusable access.
     * @param methodName The name of the method that is the search region. The method should be static and accessible
     * @param methodOwnerClass The class containing the search region named according to methodName
     * @param mb The config builder from which an instance of MulibConfig is built
     * @param N The number of requested solutions
     * @param argTypes The types of the arguments
     * @param args The arguments
     * @return The solutions
     */
    public static List<Solution> getUpToNSolutions(Class<?> methodOwnerClass, String methodName, MulibConfig.MulibConfigBuilder mb, int N, Class<?>[] argTypes, Object[] args) {
        return generateMulibContext(methodName, methodOwnerClass, argTypes, args, mb.build()).getUpToNSolutions(N, args);
    }

    /**
     * Returns the MulibContext. In contrast to methods directly returning path solutions or solutions, the context can
     * be used to repeatedly extract (path) solutions without repeating the program transformation step. 
     * @param methodOwnerClass The class containing the search region named according to methodName
     * @param methodName The name of the method that is the search region. The method should be static and accessible
     * @param mb The config builder from which an instance of MulibConfig is built
     * @param argTypes The explicitly passed argument types to find the appropriate method with
     * @return The MulibContext for potentially repeated extraction of (path) solutions
     */
    public static MulibContext getMulibContext(Class<?> methodOwnerClass, String methodName, MulibConfig.MulibConfigBuilder mb, Class<?>... argTypes) {
        return generateMulibContext(methodName, methodOwnerClass, argTypes, null, mb.build());
    }

    /**
     * Returns the MulibContext. In contrast to methods directly returning path solutions or solutions, the context can
     * be used to repeatedly extract (path) solutions without repeating the program transformation step.
     * @param methodOwnerClass The class containing the search region named according to methodName
     * @param methodName The name of the method that is the search region. The method should be static and accessible
     * @param mb The config builder from which an instance of MulibConfig is built
     * @param prototypicalArgs The argument types to find the appropriate method representing the search region are deduced
     *                         from these prototypical arguments
     * @return The MulibContext for potentially repeated extraction of (path) solutions
     */
    public static MulibContext getMulibContext(String methodName, Class<?> methodOwnerClass, MulibConfig.MulibConfigBuilder mb, Object... prototypicalArgs) {
        return generateMulibContext(methodName, methodOwnerClass, null, prototypicalArgs, mb.build());
    }

    private static MulibContext generateMulibContext(
            String methodName,
            Class<?> methodOwnerClass,
            Class<?>[] argTypes,
            Object[] args,
            MulibConfig config) {
        return new MulibContext(methodName, methodOwnerClass, config, argTypes, args);
    }

    /**
     * Method for generating a Fail. A fail can be thrown if we want do declare that a branch of the search tree should
     * not be further executed.
     * @return The fail
     */
    public static Fail fail() {
        return Fail.getInstance();
    }

    /* INITIALIZATIONS OF FREE VARIABLES. THESE METHODS ARE INDICATORS TO BE REPLACED BY A CODE TRANSFORMATION */

    /**
     * Indicator method for generating a free int. This method is replaced by the program transformation. Outside
     * of the search region this method solely throws an error.
     * @return Never returns anything. Solely the substitution in the search region will generate a symbolic value
     */
    public static int freeInt() {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for generating a free long. This method is replaced by the program transformation. Outside
     * of the search region this method solely throws an error.
     * @return Never returns anything. Solely the substitution in the search region will generate a symbolic value
     */
    public static long freeLong() {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for generating a free double. This method is replaced by the program transformation. Outside
     * of the search region this method solely throws an error.
     * @return Never returns anything. Solely the substitution in the search region will generate a symbolic value
     */
    public static double freeDouble() {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for generating a free float. This method is replaced by the program transformation. Outside
     * of the search region this method solely throws an error.
     * @return Never returns anything. Solely the substitution in the search region will generate a symbolic value
     */
    public static float freeFloat() {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for generating a free short. This method is replaced by the program transformation. Outside
     * of the search region this method solely throws an error.
     * @return Never returns anything. Solely the substitution in the search region will generate a symbolic value
     */
    public static short freeShort() {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for generating a free byte. This method is replaced by the program transformation. Outside
     * of the search region this method solely throws an error.
     * @return Never returns anything. Solely the substitution in the search region will generate a symbolic value
     */
    public static byte freeByte() {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for generating a free boolean. This method is replaced by the program transformation. Outside
     * of the search region this method solely throws an error.
     * @return Never returns anything. Solely the substitution in the search region will generate a symbolic value
     */
    public static boolean freeBoolean() {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for generating a free char. This method is replaced by the program transformation. Outside
     * of the search region this method solely throws an error.
     * @return Never returns anything. Solely the substitution in the search region will generate a symbolic value
     */
    public static char freeChar() {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for generating a free int. This method is replaced by the program transformation. Outside
     * of the search region this method solely throws an error.
     * @param lb The lower bound. Overrides any global configuration on the lower bound
     * @param ub The upper bound. Overrides any global configuration on the upper bound
     * @return Never returns anything. Solely the substitution in the search region will generate a symbolic value
     */
    public static int freeInt(int lb, int ub) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for generating a free long. This method is replaced by the program transformation. Outside
     * of the search region this method solely throws an error.
     * @param lb The lower bound. Overrides any global configuration on the lower bound
     * @param ub The upper bound. Overrides any global configuration on the upper bound
     * @return Never returns anything. Solely the substitution in the search region will generate a symbolic value
     */
    public static long freeLong(long lb, long ub) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for generating a free double. This method is replaced by the program transformation. Outside
     * of the search region this method solely throws an error.
     * @param lb The lower bound. Overrides any global configuration on the lower bound
     * @param ub The upper bound. Overrides any global configuration on the upper bound
     * @return Never returns anything. Solely the substitution in the search region will generate a symbolic value
     */
    public static double freeDouble(double lb, double ub) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for generating a free float. This method is replaced by the program transformation. Outside
     * of the search region this method solely throws an error.
     * @param lb The lower bound. Overrides any global configuration on the lower bound
     * @param ub The upper bound. Overrides any global configuration on the upper bound
     * @return Never returns anything. Solely the substitution in the search region will generate a symbolic value
     */
    public static float freeFloat(float lb, float ub) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for generating a free short. This method is replaced by the program transformation. Outside
     * of the search region this method solely throws an error.
     * @param lb The lower bound. Overrides any global configuration on the lower bound
     * @param ub The upper bound. Overrides any global configuration on the upper bound
     * @return Never returns anything. Solely the substitution in the search region will generate a symbolic value
     */
    public static short freeShort(short lb, short ub) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for generating a free byte. This method is replaced by the program transformation. Outside
     * of the search region this method solely throws an error.
     * @param lb The lower bound. Overrides any global configuration on the lower bound
     * @param ub The upper bound. Overrides any global configuration on the upper bound
     * @return Never returns anything. Solely the substitution in the search region will generate a symbolic value
     */
    public static byte freeByte(byte lb, byte ub) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for generating a free char. This method is replaced by the program transformation. Outside
     * of the search region this method solely throws an error.
     * @param lb The lower bound. Overrides any global configuration on the lower bound
     * @param ub The upper bound. Overrides any global configuration on the upper bound
     * @return Never returns anything. Solely the substitution in the search region will generate a symbolic value
     */
    public static char freeChar(char lb, char ub) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for assuming that some boolean is true. This method must be transformed by a program transformation
     * as otherwise an error is thrown without any other effect.
     * In the search region, the substitution of this method <strong>might</strong> throw a {@link Fail} if it can be determined that the
     * assumed boolean should rather be false.
     * @param b The boolean representing some constraint to assume true in the following
     */
    public static void assume(boolean b) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for assuming that some boolean is true. This method must be transformed by a program transformation
     * as otherwise an error is thrown without any other effect.
     * @param b The boolean representing some constraint to assume true in the following.
     * @return Never returns anything. Solely the substitution in the search region will check whether the condition b,
     * if added to the existing constraint stack, is satisfiable or not.
     */
    public static boolean check(boolean b) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for checking and possibly assuming that some boolean is true.
     * This method must be transformed by a program transformation as otherwise an error is thrown without any other effect.
     * @param b The boolean representing some constraint to check and possibly assume true in the following.
     * @return Never returns anything. Solely the substitution in the search region will check whether the condition b,
     * if added to the existing constraint stack, is satisfiable or not. If this is the case, b will be assumed in the following.
     */
    public static boolean checkAssume(boolean b) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for generating a free object of the specified class. If, e.g. int[].class is specified, a free int-array is generated.
     * This method must be transformed by a program transformation as otherwise an error is thrown without any other effect.
     * @param objClass The class, either of a array or another Java object, of which to spawn a free instance of.
     * @return Never returns anything. Solely the substitution in the search region will spawn a new free instance of the
     * specified class.
     * @param <T> The type of the class to spawn
     */
    public static <T> T freeObject(Class<T> objClass) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for non-deterministically picking an element from the provided options.
     * This method must be transformed by a program transformation as otherwise an error is thrown without any other effect.
     * @param aliasingTargets The options form which an element is picked non-deterministically
     * @return Never returns anything. Solely the substitution in the search region will non-deterministically return one
     * of the provided elements.
     * @param <T> The type of element to be picked from
     */
    @SafeVarargs
    public static <T> T pickFrom(T... aliasingTargets) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for non-deterministically picking an int from the provided options.
     * This method must be transformed by a program transformation as otherwise an error is thrown without any other effect.
     * @param aliasingTargets The options form which an element is picked non-deterministically
     * @return Never returns anything. Solely the substitution in the search region will non-deterministically return one
     * of the provided elements.
     */
    public static int pickFrom(int... aliasingTargets) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for non-deterministically picking a long from the provided options.
     * This method must be transformed by a program transformation as otherwise an error is thrown without any other effect.
     * @param aliasingTargets The options form which an element is picked non-deterministically
     * @return Never returns anything. Solely the substitution in the search region will non-deterministically return one
     * of the provided elements.
     */
    public static long pickFrom(long... aliasingTargets) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for non-deterministically picking a double from the provided options.
     * This method must be transformed by a program transformation as otherwise an error is thrown without any other effect.
     * @param aliasingTargets The options form which an element is picked non-deterministically
     * @return Never returns anything. Solely the substitution in the search region will non-deterministically return one
     * of the provided elements.
     */
    public static double pickFrom(double... aliasingTargets) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for non-deterministically picking a float from the provided options.
     * This method must be transformed by a program transformation as otherwise an error is thrown without any other effect.
     * @param aliasingTargets The options form which an element is picked non-deterministically
     * @return Never returns anything. Solely the substitution in the search region will non-deterministically return one
     * of the provided elements.
     */
    public static float pickFrom(float... aliasingTargets) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for non-deterministically picking a short from the provided options.
     * This method must be transformed by a program transformation as otherwise an error is thrown without any other effect.
     * @param aliasingTargets The options form which an element is picked non-deterministically
     * @return Never returns anything. Solely the substitution in the search region will non-deterministically return one
     * of the provided elements.
     */
    public static short pickFrom(short... aliasingTargets) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for non-deterministically picking a byte from the provided options.
     * This method must be transformed by a program transformation as otherwise an error is thrown without any other effect.
     * @param aliasingTargets The options form which an element is picked non-deterministically
     * @return Never returns anything. Solely the substitution in the search region will non-deterministically return one
     * of the provided elements.
     */
    public static byte pickFrom(byte... aliasingTargets) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for non-deterministically picking a boolean from the provided options.
     * This method must be transformed by a program transformation as otherwise an error is thrown without any other effect.
     * @param aliasingTargets The options form which an element is picked non-deterministically
     * @return Never returns anything. Solely the substitution in the search region will non-deterministically return one
     * of the provided elements.
     */
    public static boolean pickFrom(boolean... aliasingTargets) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for non-deterministically picking a char from the provided options.
     * This method must be transformed by a program transformation as otherwise an error is thrown without any other effect.
     * @param aliasingTargets The options form which an element is picked non-deterministically
     * @return Never returns anything. Solely the substitution in the search region will non-deterministically return one
     * of the provided elements.
     */
    public static char pickFrom(char... aliasingTargets) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for generating a free object of the specified class. If, e.g. int[].class is specified, a free int-array is generated.
     * This method must be transformed by a program transformation as otherwise an error is thrown without any other effect.
     * @param length The explicitly provided length of the, otherwise freely initialized array
     * @param clazz The component type of the element which should be generated with the specified length
     * @return Never returns anything. Solely the substitution in the search region will spawn a new free array with the
     * given length and a component type equal to clazz
     * @param <T> The type of the class to spawn an array from
     */
    public static <T> T[] freeObjectArray(int length, Class<T> clazz) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for <strong>remembering</strong> an int. A remembered value will capture a snapshot and is included
     * in the set of labels given the specified name.
     * This method must be transformed by a program transformation as otherwise an error is thrown without any other effect.
     * @param i Value to remember
     * @param name Name by which the snapshot is remembered
     */
    public static void remember(int i, String name) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for <strong>remembering</strong> a long. A remembered value will capture a snapshot and is included
     * in the set of labels given the specified name.
     * This method must be transformed by a program transformation as otherwise an error is thrown without any other effect.
     * @param i Value to remember
     * @param name Name by which the snapshot is remembered
     */
    public static void remember(long i, String name) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for <strong>remembering</strong> a double. A remembered value will capture a snapshot and is included
     * in the set of labels given the specified name.
     * This method must be transformed by a program transformation as otherwise an error is thrown without any other effect.
     * @param i Value to remember
     * @param name Name by which the snapshot is remembered
     */
    public static void remember(double i, String name) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for <strong>remembering</strong> a float. A remembered value will capture a snapshot and is included
     * in the set of labels given the specified name.
     * This method must be transformed by a program transformation as otherwise an error is thrown without any other effect.
     * @param i Value to remember
     * @param name Name by which the snapshot is remembered
     */
    public static void remember(float i, String name) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for <strong>remembering</strong> a short. A remembered value will capture a snapshot and is included
     * in the set of labels given the specified name.
     * This method must be transformed by a program transformation as otherwise an error is thrown without any other effect.
     * @param i Value to remember
     * @param name Name by which the snapshot is remembered
     */
    public static void remember(short i, String name) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for <strong>remembering</strong> a byte. A remembered value will capture a snapshot and is included
     * in the set of labels given the specified name.
     * This method must be transformed by a program transformation as otherwise an error is thrown without any other effect.
     * @param i Value to remember
     * @param name Name by which the snapshot is remembered
     */
    public static void remember(byte i, String name) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for <strong>remembering</strong> a boolean. A remembered value will capture a snapshot and is included
     * in the set of labels given the specified name.
     * This method must be transformed by a program transformation as otherwise an error is thrown without any other effect.
     * @param i Value to remember
     * @param name Name by which the snapshot is remembered
     */
    public static void remember(boolean i, String name) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for <strong>remembering</strong> a char. A remembered value will capture a snapshot and is included
     * in the set of labels given the specified name.
     * This method must be transformed by a program transformation as otherwise an error is thrown without any other effect.
     * @param i Value to remember
     * @param name Name by which the snapshot is remembered
     */
    public static void remember(char i, String name) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for <strong>remembering</strong> an object. A remembered value will capture a snapshot and is included
     * in the set of labels given the specified name. Lazy initialization is taken into account, i.e., if new values for the
     * fields of a non-array object are lazily initialized, those are included in the snapshot. Similarly, values lazily initialized
     * by an array with undetermined length are also included in the snapshot.
     * This method must be transformed by a program transformation as otherwise an error is thrown without any other effect.
     * @param i Value to remember
     * @param name Name by which the snapshot is remembered
     */
    public static void remember(Object i, String name) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for initializing a <strong>remembered</strong> int. A remembered value will capture a snapshot and is included
     * in the set of labels given the specified name.
     * This method must be transformed by a program transformation as otherwise an error is thrown without any other effect.
     * @param name Name by which the snapshot is remembered
     * @return Never returns anything. Solely the substitution in the search region will spawn a new symbolic value
     */
    public static int rememberedFreeInt(String name) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for initializing a <strong>remembered</strong> long. A remembered value will capture a snapshot and is included
     * in the set of labels given the specified name.
     * This method must be transformed by a program transformation as otherwise an error is thrown without any other effect.
     * @param name Name by which the snapshot is remembered
     * @return Never returns anything. Solely the substitution in the search region will spawn a new symbolic value
     */
    public static long rememberedFreeLong(String name) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for initializing a <strong>remembered</strong> double. A remembered value will capture a snapshot and is included
     * in the set of labels given the specified name.
     * This method must be transformed by a program transformation as otherwise an error is thrown without any other effect.
     * @param name Name by which the snapshot is remembered
     * @return Never returns anything. Solely the substitution in the search region will spawn a new symbolic value
     */
    public static double rememberedFreeDouble(String name) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for initializing a <strong>remembered</strong> float. A remembered value will capture a snapshot and is included
     * in the set of labels given the specified name.
     * This method must be transformed by a program transformation as otherwise an error is thrown without any other effect.
     * @param name Name by which the snapshot is remembered
     * @return Never returns anything. Solely the substitution in the search region will spawn a new symbolic value
     */
    public static float rememberedFreeFloat(String name) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for initializing a <strong>remembered</strong> short. A remembered value will capture a snapshot and is included
     * in the set of labels given the specified name.
     * This method must be transformed by a program transformation as otherwise an error is thrown without any other effect.
     * @param name Name by which the snapshot is remembered
     * @return Never returns anything. Solely the substitution in the search region will spawn a new symbolic value
     */
    public static short rememberedFreeShort(String name) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for initializing a <strong>remembered</strong> byte. A remembered value will capture a snapshot and is included
     * in the set of labels given the specified name.
     * This method must be transformed by a program transformation as otherwise an error is thrown without any other effect.
     * @param name Name by which the snapshot is remembered
     * @return Never returns anything. Solely the substitution in the search region will spawn a new symbolic value
     */
    public static byte rememberedFreeByte(String name) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for initializing a <strong>remembered</strong> boolean. A remembered value will capture a snapshot and is included
     * in the set of labels given the specified name.
     * This method must be transformed by a program transformation as otherwise an error is thrown without any other effect.
     * @param name Name by which the snapshot is remembered
     * @return Never returns anything. Solely the substitution in the search region will spawn a new symbolic value
     */
    public static boolean rememberedFreeBoolean(String name) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for initializing a <strong>remembered</strong> char. A remembered value will capture a snapshot and is included
     * in the set of labels given the specified name.
     * This method must be transformed by a program transformation as otherwise an error is thrown without any other effect.
     * @param name Name by which the snapshot is remembered
     * @return Never returns anything. Solely the substitution in the search region will spawn a new symbolic value
     */
    public static char rememberedFreeChar(String name) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for initializing a <strong>remembered</strong> int. A remembered value will capture a snapshot and is included
     * in the set of labels given the specified name.
     * This method must be transformed by a program transformation as otherwise an error is thrown without any other effect.
     * @param name Name by which the snapshot is remembered
     * @param lb The lower bound. Overrides any global configuration on the lower bound
     * @param ub The upper bound. Overrides any global configuration on the upper bound
     * @return Never returns anything. Solely the substitution in the search region will spawn a new symbolic value
     */
    public static int rememberedFreeInt(String name, int lb, int ub) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for initializing a <strong>remembered</strong> long. A remembered value will capture a snapshot and is included
     * in the set of labels given the specified name.
     * This method must be transformed by a program transformation as otherwise an error is thrown without any other effect.
     * @param name Name by which the snapshot is remembered
     * @param lb The lower bound. Overrides any global configuration on the lower bound
     * @param ub The upper bound. Overrides any global configuration on the upper bound
     * @return Never returns anything. Solely the substitution in the search region will spawn a new symbolic value
     */
    public static long rememberedFreeLong(String name, long lb, long ub) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for initializing a <strong>remembered</strong> double. A remembered value will capture a snapshot and is included
     * in the set of labels given the specified name.
     * This method must be transformed by a program transformation as otherwise an error is thrown without any other effect.
     * @param name Name by which the snapshot is remembered
     * @param lb The lower bound. Overrides any global configuration on the lower bound
     * @param ub The upper bound. Overrides any global configuration on the upper bound
     * @return Never returns anything. Solely the substitution in the search region will spawn a new symbolic value
     */
    public static double rememberedFreeDouble(String name, double lb, double ub) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for initializing a <strong>remembered</strong> float. A remembered value will capture a snapshot and is included
     * in the set of labels given the specified name.
     * This method must be transformed by a program transformation as otherwise an error is thrown without any other effect.
     * @param name Name by which the snapshot is remembered
     * @param lb The lower bound. Overrides any global configuration on the lower bound
     * @param ub The upper bound. Overrides any global configuration on the upper bound
     * @return Never returns anything. Solely the substitution in the search region will spawn a new symbolic value
     */
    public static float rememberedFreeFloat(String name, float lb, float ub) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for initializing a <strong>remembered</strong> short. A remembered value will capture a snapshot and is included
     * in the set of labels given the specified name.
     * This method must be transformed by a program transformation as otherwise an error is thrown without any other effect.
     * @param name Name by which the snapshot is remembered
     * @param lb The lower bound. Overrides any global configuration on the lower bound
     * @param ub The upper bound. Overrides any global configuration on the upper bound
     * @return Never returns anything. Solely the substitution in the search region will spawn a new symbolic value
     */
    public static short rememberedFreeShort(String name, short lb, short ub) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for initializing a <strong>remembered</strong> byte. A remembered value will capture a snapshot and is included
     * in the set of labels given the specified name.
     * This method must be transformed by a program transformation as otherwise an error is thrown without any other effect.
     * @param name Name by which the snapshot is remembered
     * @param lb The lower bound. Overrides any global configuration on the lower bound
     * @param ub The upper bound. Overrides any global configuration on the upper bound
     * @return Never returns anything. Solely the substitution in the search region will spawn a new symbolic value
     */
    public static byte rememberedFreeByte(String name, byte lb, byte ub) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for initializing a <strong>remembered</strong> char. A remembered value will capture a snapshot and is included
     * in the set of labels given the specified name.
     * This method must be transformed by a program transformation as otherwise an error is thrown without any other effect.
     * @param name Name by which the snapshot is remembered
     * @param lb The lower bound. Overrides any global configuration on the lower bound
     * @param ub The upper bound. Overrides any global configuration on the upper bound
     * @return Never returns anything. Solely the substitution in the search region will spawn a new symbolic value
     */
    public static char rememberedFreeChar(String name, char lb, char ub) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for initializing a <strong>remembered</strong> object. A remembered value will capture a snapshot and is included
     * in the set of labels given the specified name. Lazy initialization is taken into account, i.e., if new values for the
     * fields of a non-array object are lazily initialized, those are included in the snapshot. Similarly, values lazily initialized
     * by an array with undetermined length are also included in the snapshot.
     * This method must be transformed by a program transformation as otherwise an error is thrown without any other effect.
     * @param name Name by which the snapshot is remembered
     * @param objClass The class used to spawn a new free object
     * @param <T> The type of which to generate a new free instance
     * @return Never returns anything. Solely the substitution in the search region will spawn a new free object
     */
    public static <T> T rememberedFreeObject(String name, Class<T> objClass) {
        throw _shouldHaveBeenReplaced();
    }

    /**
     * Indicator method for determining whether we currently are in a search region.
     * By substituting this call in the search region, we can determine whether we are currently in a search region.
     * By doing so, we can create model classes that will employ a more invested lazy initialization approach.
     * @return false. The substitution of the method will return true, if we are actively in a search region.
     */
    public static boolean isInSearch() {
        return false;
    }

    private static MulibRuntimeError _shouldHaveBeenReplaced() {
        return new MulibRuntimeError("This method should always be replaced by a code transformation.");
    }
}
