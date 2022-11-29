package de.wwu.mulib;

import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.search.choice_points.ChoicePointFactory;
import de.wwu.mulib.search.executors.*;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.search.trees.SearchTree;
import de.wwu.mulib.search.trees.Solution;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.primitives.*;
import de.wwu.mulib.transformations.MulibTransformer;
import de.wwu.mulib.transformations.MulibValueTransformer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;

public final class MulibContext {
    private final MulibConfig config;
    private static final Object[] emptyArgs = new Object[0];
    private final Class<?>[] untransformedArgTypes;
    private final Class<?>[] transformedArgTypes;
    private final String methodName;
    private final MulibTransformer mulibTransformer;
    private final Class<?> possiblyTransformedMethodClass;

    protected MulibContext(
            String methodName,
            Class<?> owningMethodClass,
            MulibConfig config,
            Class<?>[] untransformedArgTypes,
            Object... prototypicalArgs) {
        long start = System.nanoTime();
        this.methodName = methodName;
        this.config = config;
        if (untransformedArgTypes == null) {
            untransformedArgTypes = findMethodFittingToArgs(prototypicalArgs, methodName, owningMethodClass);
        }
        this.untransformedArgTypes = untransformedArgTypes;
        if (config.TRANSF_TRANSFORMATION_REQUIRED) {
            mulibTransformer = MulibTransformer.get(config);
            mulibTransformer.transformAndLoadClasses(owningMethodClass);
            possiblyTransformedMethodClass = mulibTransformer.getTransformedClass(owningMethodClass);
            transformedArgTypes = transformArgumentTypes(mulibTransformer, untransformedArgTypes);
        } else {
            mulibTransformer = null;
            possiblyTransformedMethodClass = owningMethodClass;
            transformedArgTypes = untransformedArgTypes;
        }
        long end = System.nanoTime();
        Mulib.log.finer("Took " + ((end - start) / 1e6) + "ms for " + config + " to set up MulibContext");
    }

    private void _throwExceptionOnArgumentMismatch(Object[] providedArgs) {
        if (providedArgs == null || providedArgs.length != transformedArgTypes.length) {
            throw new MulibRuntimeException("The calls to MulibContext must contain the arguments you which to use!");
        }
    }

    private MulibExecutorManager generateNewMulibExecutorManagerForPreInitializedContext(Object[] args) {
        long start = System.nanoTime();
        ChoicePointFactory choicePointFactory = ChoicePointFactory.getInstance(config);
        ValueFactory valueFactory = ValueFactory.getInstance(config);
        CalculationFactory calculationFactory = CalculationFactory.getInstance(config, valueFactory);

        Object[] searchRegionArgs;
        MulibValueTransformer mulibValueTransformer;
        if (config.TRANSF_TRANSFORMATION_REQUIRED) {
            mulibValueTransformer = new MulibValueTransformer(config, mulibTransformer);
            searchRegionArgs = transformArguments(mulibValueTransformer, args);
        } else {
            assert mulibTransformer == null;
            mulibValueTransformer = new MulibValueTransformer(config, null);
            searchRegionArgs = args;
        }

        // Find the next sarray-id if any of the arguments are sarrays
        mulibValueTransformer.setPartnerClassObjectNr(args);

        Function<SymbolicExecution, Object[]> argsSupplier;
        if (searchRegionArgs.length == 0) {
            argsSupplier = (se) -> { return emptyArgs; };
        } else {
            argsSupplier = (se) -> {
                Map<Object, Object> replacedMap = new IdentityHashMap<>();
                Object[] arguments = new Object[searchRegionArgs.length];
                for (int i = 0; i < searchRegionArgs.length; i++) {
                    Object arg = searchRegionArgs[i];
                    Object newArg;
                    if ((newArg = replacedMap.get(arg)) != null) {
                        arguments[i] = newArg;
                        continue;
                    }
                    if (arg instanceof Sprimitive) {
                        if (config.CONCOLIC
                                && arg instanceof SymNumericExpressionSprimitive) {
                            // Creation of wrapper SymSprimitive with concolic container required
                            newArg = se.getMulibValueCopier().copySprimitive((Sprimitive) arg);
                        } else {
                            // Keep value
                            newArg = arg;
                        }
                    } else {
                        // Is null, Sarray, or PartnerClass
                        assert arg == null || arg instanceof PartnerClass;
                        newArg = se.getMulibValueCopier().copyNonSprimitive(arg);
                    }
                    replacedMap.put(arg, newArg);
                    arguments[i] = newArg;
                }
                return arguments;
            };
        }

        MethodHandle methodHandle;
        try {
            Method method = possiblyTransformedMethodClass.getDeclaredMethod(methodName, transformedArgTypes);
            methodHandle = MethodHandles.lookup().unreflect(method);
        } catch (NoSuchMethodException | IllegalAccessException | VerifyError e) {
            e.printStackTrace();
            throw new MulibRuntimeException(e);
        }
        SearchTree searchTree = new SearchTree(config, methodHandle, argsSupplier);
        MulibExecutorManager result = config.ADDITIONAL_PARALLEL_SEARCH_STRATEGIES.isEmpty() ?
                new SingleExecutorManager(
                        config,
                        searchTree,
                        choicePointFactory,
                        valueFactory,
                        calculationFactory,
                        mulibValueTransformer
                )
                :
                new MultiExecutorsManager(
                        config,
                        searchTree,
                        choicePointFactory,
                        valueFactory,
                        calculationFactory,
                        mulibValueTransformer
                );
        long end = System.nanoTime();
        Mulib.log.finer("Took " + ((end - start) / 1e6) + "ms for " + config + " to set up MulibExecutorManager");
        return result;
    }

    private <T> T _checkExecuteAndLog(Object[] args, Function<Object[], T> argsToResult) {
        _throwExceptionOnArgumentMismatch(args);
        long start = System.nanoTime();
        T result = argsToResult.apply(args);
        long end = System.nanoTime();
        Mulib.log.fine("Took " + ((end - start) / 1e6) + "ms for " + config + " to retrieve the solution(s)");
        return result;
    }

    public List<PathSolution> getAllPathSolutions(Object... args) {
        return _checkExecuteAndLog(args, (arguments) -> generateNewMulibExecutorManagerForPreInitializedContext(arguments).getAllPathSolutions());
    }

    public Optional<PathSolution> getPathSolution(Object... args) {
        return _checkExecuteAndLog(args, (arguments) -> generateNewMulibExecutorManagerForPreInitializedContext(arguments).getPathSolution());
    }

    public List<Solution> getAllSolutions(Object... args) {
        return _checkExecuteAndLog(args, (arguments) -> generateNewMulibExecutorManagerForPreInitializedContext(arguments).getUpToNSolutions(Integer.MAX_VALUE));
    }

    public List<Solution> getUpToNSolutions(int N, Object... args) {
        return _checkExecuteAndLog(args, (arguments) -> generateNewMulibExecutorManagerForPreInitializedContext(arguments).getUpToNSolutions(N));
    }

    public Optional<Solution> getSolution(Object... args) {
        List<Solution> result = _checkExecuteAndLog(args, (arguments) -> generateNewMulibExecutorManagerForPreInitializedContext(arguments).getUpToNSolutions(1));
        if (result.size() > 0) {
            return Optional.of(result.get(0));
        } else {
            return Optional.empty();
        }
    }

    private static Object[] transformArguments(
            MulibValueTransformer mulibValueTransformer,
            Object[] args) {
        Object[] result = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            result[i] = mulibValueTransformer.transform(args[i]);
        }
        return result;
    }

    private static Class<?>[] transformArgumentTypes(
            MulibTransformer mulibTransformer,
            Class<?>[] argTypes) {
        if (argTypes == null) {
            return null;
        }
        Class<?>[] result = new Class[argTypes.length];
        for (int i = 0; i < argTypes.length; i++) {
            result[i] = mulibTransformer.transformType(argTypes[i]);
        }
        return result;
    }

    // Returns an array of parameter types fitting for the given list of arguments, if such a fit exists.
    private static Class<?>[] findMethodFittingToArgs(Object[] untransformedArgTypes, String methodName, Class<?> owningMethodClass) {
        // Get types of arguments
        Class<?>[] directTypesOfArgs =
                Arrays.stream(untransformedArgTypes).map(arg -> arg == null ? null : arg.getClass()).toArray(Class<?>[]::new);
        // Get methods matching the name and length of arguments
        Method[] candidates = Arrays.stream(owningMethodClass.getDeclaredMethods())
                .filter(m -> m.getName().equals(methodName) && m.getParameterCount() == directTypesOfArgs.length)
                .toArray(Method[]::new);

        for (Method m : candidates) {
            Class<?>[] paramTypes = m.getParameterTypes();
            boolean valid = true;
            for (int i = 0; i < directTypesOfArgs.length; i++) {
                if (directTypesOfArgs[i] == null) {
                    if (!paramTypes[i].isPrimitive()) {
                        continue;
                    } else {
                        valid = false;
                        break;
                    }
                }
                if (!paramTypes[i].isAssignableFrom(directTypesOfArgs[i])) {
                    if (paramTypes[i].isPrimitive() && isWrapperOfType(paramTypes[i], directTypesOfArgs[i])) {
                        continue;
                    }
                    valid = false;
                    break;
                }
            }

            if (valid) {
                return m.getParameterTypes();
            }
        }
        throw new MulibRuntimeException("Method that fits the argument types: " + Arrays.toString(directTypesOfArgs) +
                " while having the name " + methodName + " cannot be found in class " + owningMethodClass.getName());
    }

    // Checks if 'checkIfWrapper' is a wrapper type of 'type'
    private static boolean isWrapperOfType(Class<?> type, Class<?> checkIfWrapper) {
        Class<?> mulibWrapper;
        Class<?> javaWrapper;
        if (type == int.class) {
            mulibWrapper = Sint.class;
            javaWrapper = Integer.class;
        } else if (type == long.class) {
            mulibWrapper = Slong.class;
            javaWrapper = Long.class;
        } else if (type == double.class) {
            mulibWrapper = Sdouble.class;
            javaWrapper = Double.class;
        } else if (type == float.class) {
            mulibWrapper = Sfloat.class;
            javaWrapper = Float.class;
        } else if (type == short.class) {
            mulibWrapper = Sshort.class;
            javaWrapper = Short.class;
        } else if (type == byte.class) {
            mulibWrapper = Sbyte.class;
            javaWrapper = Byte.class;
        } else if (type == boolean.class) {
            mulibWrapper = Sbool.class;
            javaWrapper = Boolean.class;
        } else if (type == String.class) {
            mulibWrapper = String.class;
            javaWrapper = String.class;
        } else {
            throw new NotYetImplementedException();
        }
        return mulibWrapper.isAssignableFrom(checkIfWrapper) || javaWrapper == checkIfWrapper;
    }

}
