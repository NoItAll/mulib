package de.wwu.mulib;

import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.search.choice_points.SymbolicChoicePointFactory;
import de.wwu.mulib.search.executors.MulibExecutorManager;
import de.wwu.mulib.search.executors.MultiExecutorsManager;
import de.wwu.mulib.search.executors.SingleExecutorManager;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.search.trees.SearchTree;
import de.wwu.mulib.search.values.SymbolicValueFactory;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sdouble;
import de.wwu.mulib.substitutions.primitives.Sfloat;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.transformer.MulibTransformer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

public final class Mulib {

    public static final Logger log = Logger.getLogger("mulib");

    public enum MethodCallType {
        VIRTUAL, STATIC, INTERFACE
    }

    private Mulib() {}

    private static int i = 0;
    private static long transformDuration;
    private static final List<Long> measuredColdTimes = new ArrayList<>();
    private static final List<Long> measuredWarmTimes = new ArrayList<>();
    public static List<PathSolution> executeMulib(
            String methodName,
            Class<?> mainClass,
            MulibConfig.MulibConfigBuilder mb) {
        long startTransformation = System.nanoTime();
        MulibConfig config =
                mb
                        .setTRANSF_REGARD_SPECIAL_CASE(Collections.singletonList(mainClass))
                        .build();
        MulibTransformer transformer = new MulibTransformer(config);
        transformer.transformAndLoadClasses(mainClass);
        Class<?> transformedClass = transformer.getTransformedClass(mainClass);
        try {
            Method method = transformedClass.getDeclaredMethod(methodName);
            long endTransformation = System.nanoTime();

            long duration = (endTransformation - startTransformation);
            System.out.println("Duration of transformation: " + duration + "ns");
            if (i == 0) {
                transformDuration = duration;
            }
            return executeMulibWithoutTransformation(method, config);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            throw new MulibRuntimeException("Method not found: " + methodName + ", in class: " + transformedClass);
        }
    }

    public static List<PathSolution> executeMulibWithoutTransformation(
            String methodName,
            Class<?> containingClass,
            MulibConfig.MulibConfigBuilder mb) {
        try {
            Method method = containingClass.getDeclaredMethod(methodName);
            return executeMulibWithoutTransformation(method, mb.build());
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            throw new MulibRuntimeException("Method not found: " + methodName + ", in class: " + containingClass);
        }
    }

    private static List<PathSolution> executeMulibWithoutTransformation(
            Method method,
            MulibConfig config) {
        try {
            MulibContext mc = Mulib.generateWithoutTransformation(
                    MethodHandles.lookup().unreflect(method),
                    config
            );
            long start = System.nanoTime();
            List<PathSolution> pathSolutions = mc.getAllPathSolutions();
            long end = System.nanoTime();
            long duration = end - start;
            System.out.println("Duration of search: " + duration + "ns");
            if (i < 10) {
                measuredColdTimes.add(duration);
            } else {
                measuredWarmTimes.add(duration);
            }
            System.out.println("Transform cold: " + transformDuration);
            System.out.println("Cold: " + measuredColdTimes);
            System.out.println("Warm: " + measuredWarmTimes);
            i++;
            return pathSolutions;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new MulibRuntimeException("Illegal access");
        }
    }

    public static MulibContext generateWithoutTransformation(
            MethodHandle methodHandle) {
        return generateMulibContext(methodHandle, MulibConfig.get());
    }

    public static MulibContext generateWithoutTransformation(
            MethodHandle methodHandle,
            MulibConfig mulibConfig) {
        return generateMulibContext(methodHandle, mulibConfig);
    }

    public static MulibContext generateWithTransformation(
            Class<?> containingClass,
            String methodName,
            MethodCallType methodCallType,
            Class<?> returnType,
            List<Class<?>> parameterTypes,
            List<Object> arguments) {
        return generateWithTransformation(
                containingClass,
                methodName,
                methodCallType,
                returnType,
                parameterTypes,
                arguments,
                MulibConfig.get()
        );
    }
    public static MulibContext generateWithTransformation(
            Class<?> containingClass,
            String methodName,
            MethodCallType methodCallType,
            Class<?> returnType,
            List<Class<?>> parameterTypes,
            List<Object> arguments,
            MulibConfig config) {
        MulibTransformer mulibTransformer = new MulibTransformer(config);
        mulibTransformer.transformAndLoadClasses(containingClass);
        Class<?> newContainingClass = mulibTransformer.getTransformedClass(containingClass);
        Class<?> newReturnType = mulibTransformer.getPossiblyTransformedClass(returnType);
        List<Class<?>> newParameterTypes = transformParameterTypes(mulibTransformer, parameterTypes);
        List<Object> newArgs = transformArguments(mulibTransformer, parameterTypes, newParameterTypes, arguments);
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodType methodType =  MethodType.methodType(newReturnType, newParameterTypes);
        MethodHandle methodHandle;
        try {
            if (methodCallType == MethodCallType.VIRTUAL) {
                methodHandle = lookup.findVirtual(newContainingClass, methodName, methodType);
            } else if (methodCallType == MethodCallType.STATIC) {
                methodHandle = lookup.findStatic(newContainingClass, methodName, methodType);
            } else {
                throw new MulibRuntimeException("Unknown methodCallType: " + methodCallType);
            }
            return generateWithoutTransformation(methodHandle);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new MulibRuntimeException(e);
        }
    }

    private static List<Class<?>> transformParameterTypes(
            MulibTransformer transformer,
            List<Class<?>> parameterTypes) {
        List<Class<?>> result = new ArrayList<>();
        for (Class<?> pt : parameterTypes) {
            if (pt == int.class) {
                result.add(Sint.class);
            } else if (pt == double.class) {
                result.add(Sdouble.class);
            } else if (pt == long.class) {
                throw new NotYetImplementedException();
            } else if (pt == float.class) {
                result.add(Sfloat.class);
            } else if (pt == boolean.class) {
                result.add(Sbool.class);
            } else if (pt == short.class) {
                throw new NotYetImplementedException();
            } else if (pt == byte.class) {
                throw new NotYetImplementedException();
            } else if (pt == char.class) {
                throw new NotYetImplementedException();
            } else if (pt == String.class) {
                throw new NotYetImplementedException();
            } else {
                result.add(transformer.getPossiblyTransformedClass(pt));
            }
        }
        return result;
    }

    private static List<Object> transformArguments(
            MulibTransformer transformer,
            List<Class<?>> beforeTransformationParameterTypes, // Here for debugging
            List<Class<?>> transformedParameterTypes,
            List<Object> args) {
        Iterator<Class<?>> oldTypes = beforeTransformationParameterTypes.listIterator();
        Iterator<Class<?>> newTypes = transformedParameterTypes.listIterator();
        Iterator<Object> oldArgs = args.listIterator();
        List<Object> result = new ArrayList<>();
        while (oldTypes.hasNext()) {
            Class<?> pt = oldTypes.next();
            Class<?> npt = newTypes.next();
            Object oa = oldArgs.next();
            Object na = transformer.replaceValue(oa);
            if (!npt.isAssignableFrom(na.getClass())) {
                throw new MulibRuntimeException("The new parameter type and transformed argument should match.");
            }
            // TODO copy old values from oa to na
            result.add(na);
        }
        return result;
    }

    private static MulibContext generateMulibContext(
            MethodHandle methodHandle,
            MulibConfig config) {
        return new MulibContext(
                methodHandle,
                config.ADDITIONAL_PARALLEL_SEARCH_STRATEGIES.isEmpty() ?
                        new SingleExecutorManager(
                            new SearchTree(config, methodHandle, false),
                            new SymbolicChoicePointFactory(),
                            new SymbolicValueFactory(),
                            config
                        )
                        :
                        new MultiExecutorsManager(
                                config,
                                new SearchTree(config, methodHandle, false),
                                new SymbolicChoicePointFactory(),
                                new SymbolicValueFactory()
                        )
                ,
                config
        );
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

    public static <T> T[] freeArray(Class<T> arClass) {
        throw _shouldHaveBeenReplaced();
    }

    public static int trackedFreeInt(String identifier) {
        throw _shouldHaveBeenReplaced();
    }

    public static long trackedFreeLong(String identifier) {
        throw _shouldHaveBeenReplaced();
    }

    public static double trackedFreeDouble(String identifier) {
        throw _shouldHaveBeenReplaced();
    }

    public static float trackedFreeFloat(String identifier) {
        throw _shouldHaveBeenReplaced();
    }

    public static short trackedFreeShort(String identifier) {
        throw _shouldHaveBeenReplaced();
    }

    public static byte trackedFreeByte(String identifier) {
        throw _shouldHaveBeenReplaced();
    }

    public static boolean trackedFreeBoolean(String identifier) {
        throw _shouldHaveBeenReplaced();
    }

    public static char trackedFreeChar(String identifier) {
        throw _shouldHaveBeenReplaced();
    }

    public static <T> T trackedFreeObject(Class<T> objClass, String identifier) {
        throw _shouldHaveBeenReplaced();
    }

    public static <T> T[] trackedFreeArray(Class<T> arClass, String identifier) {
        throw _shouldHaveBeenReplaced();
    }

    private static MulibRuntimeException _shouldHaveBeenReplaced() {
        return new MulibRuntimeException("This method should always be replaced by a code transformation.");
    }
}
