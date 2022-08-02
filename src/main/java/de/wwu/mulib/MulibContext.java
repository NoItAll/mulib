package de.wwu.mulib;

import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.search.choice_points.ChoicePointFactory;
import de.wwu.mulib.search.executors.*;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.search.trees.SearchTree;
import de.wwu.mulib.search.trees.Solution;
import de.wwu.mulib.solving.Solvers;
import de.wwu.mulib.solving.solvers.SolverManager;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.Sarray;
import de.wwu.mulib.substitutions.primitives.*;
import de.wwu.mulib.transformations.MulibTransformer;
import de.wwu.mulib.transformations.MulibValueLabeler;
import de.wwu.mulib.transformations.MulibValueTransformer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;

@SuppressWarnings("FieldCanBeLocal")
public class MulibContext {
    @SuppressWarnings("all")
    private final MulibConfig mulibConfig;
    @SuppressWarnings("all")
    private final MethodHandle methodHandle;
    private final MulibExecutorManager mulibExecutorManager;
    private SolverManager solverManager = null;
    private final Function<SymbolicExecution, Object[]> argsSupplier;
    private final MulibTransformer mulibTransformer;
    private final MulibValueTransformer mulibValueTransformer;
    private final ChoicePointFactory choicePointFactory;
    private final ValueFactory valueFactory;
    private final CalculationFactory calculationFactory;
    private static final Object[] emptyArgs = new Object[0];
    private final boolean transformationRequired;

    protected MulibContext(
            String methodName,
            Class<?> owningMethodClass,
            MulibConfig config,
            boolean transformationRequired,
            Class<?>[] untransformedArgs,
            Object[] args) {
        Class<?> possiblyTransformedMethodClass;
        Object[] searchRegionArgs;
        Class<?>[] searchRegionArgTypes;
        if (untransformedArgs == null) {
            untransformedArgs = findMethodFittingToArgs(args, methodName, owningMethodClass);
        }
        this.choicePointFactory = ChoicePointFactory.getInstance(config);
        this.valueFactory = ValueFactory.getInstance(config);
        this.calculationFactory = CalculationFactory.getInstance(config);
        this.transformationRequired = transformationRequired;
        if (transformationRequired) {
            this.mulibTransformer = MulibTransformer.get(config);
            this.mulibTransformer.transformAndLoadClasses(owningMethodClass);
            possiblyTransformedMethodClass = this.mulibTransformer.getTransformedClass(owningMethodClass);
            this.mulibValueTransformer = new MulibValueTransformer(config, mulibTransformer, true);
            searchRegionArgs = transformArguments(mulibValueTransformer, args);
            searchRegionArgTypes = transformArgumentTypes(mulibValueTransformer, untransformedArgs);
        } else {
            this.mulibTransformer = null;
            this.mulibValueTransformer = new MulibValueTransformer(config, null, false);
            possiblyTransformedMethodClass = owningMethodClass;
            searchRegionArgs = args;
            searchRegionArgTypes = untransformedArgs;
        }
        // Find the next sarray-id if any of the arguments are sarrays
        this.mulibValueTransformer.setNextSarrayId(args);

        this.mulibConfig = config;

        if (searchRegionArgs.length == 0) {
            this.argsSupplier = (se) -> { return emptyArgs; };
        } else {
            this.argsSupplier = (se) -> {
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
                        assert arg == null || arg instanceof PartnerClass || arg instanceof Sarray;
                        newArg = se.getMulibValueCopier().copyNonSprimitive(arg);
                    }
                    replacedMap.put(arg, newArg);
                    arguments[i] = newArg;
                }
                return arguments;
            };
        }

        try {
            Method method = possiblyTransformedMethodClass.getDeclaredMethod(methodName, searchRegionArgTypes);
            this.methodHandle = MethodHandles.lookup().unreflect(method);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new MulibRuntimeException(e);
        } catch (VerifyError t) {
            t.printStackTrace();
            throw new MulibRuntimeException(t);
        }
        SearchTree searchTree = new SearchTree(config, methodHandle, argsSupplier);
        this.mulibExecutorManager = config.ADDITIONAL_PARALLEL_SEARCH_STRATEGIES.isEmpty() ?
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
    }

    public synchronized List<PathSolution> getAllPathSolutions() {
        long startTime = System.nanoTime();
        List<PathSolution> result = mulibExecutorManager.getAllPathSolutions();
        long endTime = System.nanoTime();
        Mulib.log.log(Level.INFO, "Took " + (endTime - startTime) + "ns");
        return result;
    }

    public synchronized Optional<PathSolution> getPathSolution() {
        return mulibExecutorManager.getPathSolution();
    }

    public synchronized List<Solution> getAllSolutions(PathSolution pathSolution) {
        return getUpToNSolutions(pathSolution, Integer.MAX_VALUE);
    }

    public synchronized List<Solution> getUpToNSolutions(PathSolution pathSolution, int N) {
        if (solverManager == null) {
            solverManager = Solvers.getSolverManager(mulibConfig);
        }
        return solverManager.getUpToNSolutions(pathSolution, N, new MulibValueLabeler(mulibConfig, transformationRequired));
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
            MulibValueTransformer mulibValueTransformer,
            Class<?>[] argTypes) {
        Class<?>[] result = new Class[argTypes.length];
        for (int i = 0; i < argTypes.length; i++) {
            result[i] = mulibValueTransformer.transformType(argTypes[i]);
        }
        return result;
    }

    // Returns an array of parameter types fitting for the given list of arguments, if such a fit exists.
    private static Class<?>[] findMethodFittingToArgs(Object[] untransformedArgs, String methodName, Class<?> owningMethodClass) {
        // Get types of arguments
        Class<?>[] directTypesOfArgs =
                Arrays.stream(untransformedArgs).map(arg -> arg == null ? null : arg.getClass()).toArray(Class<?>[]::new);
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
