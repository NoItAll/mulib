package de.wwu.mulib;

import de.wwu.mulib.throwables.MulibRuntimeException;
import de.wwu.mulib.throwables.NotYetImplementedException;
import de.wwu.mulib.search.choice_points.ChoicePointFactory;
import de.wwu.mulib.search.choice_points.CoverageCfg;
import de.wwu.mulib.search.executors.*;
import de.wwu.mulib.search.trees.*;
import de.wwu.mulib.solving.Solution;
import de.wwu.mulib.substitutions.Sarray;
import de.wwu.mulib.substitutions.ValueFactory;
import de.wwu.mulib.substitutions.primitives.*;
import de.wwu.mulib.tcg.*;
import de.wwu.mulib.transformations.MulibTransformer;
import de.wwu.mulib.transformations.MulibValueTransformer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * While retrieving {@link PathSolution}s mostly is a one-off job, i.e., we generate test cases once, retrieving 
 * {@link Solution}s can be expected to be done recurrently. 
 * This class holds a prepared version of the search region, i.e., the classes have already been transformed and the
 * method handle of the search region was already found.
 * Thus, this class can be used to recurrently execute, e.g., {@link MulibContext#getUpToNSolutions(int, Object...)} 
 * with varying arguments. 
 * Behind the scenes, all functionality of {@link Mulib} creates a temporary instance of {@link MulibContext} to provide
 * its functionality, anyway. 
 */
public final class MulibContext {
    private final MulibConfig config;
    private final Class<?>[] transformedArgTypes;
    private final MulibTransformer mulibTransformer;
    private final Class<?> possiblyTransformedMethodClass;
    private final MethodHandle methodHandle;

    MulibContext(
            String methodName,
            Class<?> owningMethodClass,
            MulibConfig config,
            Class<?>[] untransformedArgTypes,
            Object... prototypicalArgs) {
        long start = System.nanoTime();
        this.config = config;
        if (untransformedArgTypes == null) {
            untransformedArgTypes = findMethodFittingToArgs(prototypicalArgs, methodName, owningMethodClass);
        }
        this.mulibTransformer = MulibTransformer.get(config);
        if (config.TRANSF_TRANSFORMATION_REQUIRED) {
            mulibTransformer.transformAndLoadClasses(owningMethodClass);
            possiblyTransformedMethodClass = mulibTransformer.getTransformedClass(owningMethodClass);
            transformedArgTypes = transformArgumentTypes(mulibTransformer, untransformedArgTypes);
        } else {
            possiblyTransformedMethodClass = owningMethodClass;
            transformedArgTypes = untransformedArgTypes;
        }
        try {
            Method method = possiblyTransformedMethodClass.getDeclaredMethod(methodName, transformedArgTypes);
            this.methodHandle = MethodHandles.lookup().unreflect(method);
        } catch (NoSuchMethodException | IllegalAccessException | VerifyError e) {
            throw new MulibRuntimeException(e);
        }

        long end = System.nanoTime();
        Mulib.log.finer("Took " + ((end - start) / 1e6) + "ms for " + config + " to set up MulibContext");
    }

    private void _throwExceptionOnArgumentMismatch(Object[] providedArgs) {
        if (providedArgs == null || providedArgs.length != transformedArgTypes.length) {
            throw new MulibRuntimeException("The calls to MulibContext must contain the arguments you wish to use! Expected length: "
                    + transformedArgTypes.length + ", provided: " + (providedArgs == null ? "null" : providedArgs.length));
        }
    }

    private MulibExecutorManager generateNewMulibExecutorManagerForPreInitializedContext(Object[] args) {
        long start = System.nanoTime();

        Object[] searchRegionArgs;
        MulibValueTransformer mulibValueTransformer;

        Map<Field, Field> transformedToOriginalStaticFields = new HashMap<>();
        if (config.TRANSF_TRANSFORMATION_REQUIRED) {
            // Get the static fields that are used in the search region
            transformedToOriginalStaticFields =
                    mulibTransformer.getAccessibleStaticFieldsOfTransformedClassesToOriginalClasses();
            mulibValueTransformer = new MulibValueTransformer(config, mulibTransformer);
            searchRegionArgs = transformArguments(mulibValueTransformer, args);
        } else {
            mulibValueTransformer = new MulibValueTransformer(config, null);
            searchRegionArgs = args;
        }

        // Find the next sarray-id if any of the arguments are sarrays
        mulibValueTransformer.setPartnerClassObjectNr(args);
        StaticVariables staticVariables = new StaticVariables(mulibValueTransformer, transformedToOriginalStaticFields);
        SearchTree searchTree = new SearchTree(config);

        Map<Class<?>, Class<?>> arrayTypesToSpecializedSarrayClass = mulibTransformer.getArrayTypesToSpecializedSarrayClass();
        assert arrayTypesToSpecializedSarrayClass.values().stream().allMatch(Sarray.PartnerClassSarray.class::isAssignableFrom) : "Specialized arrays should only be created for arrays of arrays and arrays of partner class objects";
        CoverageCfg coverageCfg;
        if (config.TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID) {
            coverageCfg = new CoverageCfg(config, mulibTransformer.getNumberNumberedChoicePoints());
        } else {
            coverageCfg = null;
        }
        ChoicePointFactory choicePointFactory = ChoicePointFactory.getInstance(config, coverageCfg);
        ValueFactory valueFactory = ValueFactory.getInstance(config, arrayTypesToSpecializedSarrayClass);
        CalculationFactory calculationFactory = CalculationFactory.getInstance(config, valueFactory);
        MulibExecutorManager result = config.SEARCH_ADDITIONAL_PARALLEL_STRATEGIES.isEmpty() ?
                new SingleExecutorManager(
                        config,
                        searchTree,
                        choicePointFactory,
                        valueFactory,
                        calculationFactory,
                        mulibValueTransformer,
                        methodHandle,
                        staticVariables,
                        searchRegionArgs,
                        coverageCfg
                )
                :
                new MultiExecutorsManager(
                        config,
                        searchTree,
                        choicePointFactory,
                        valueFactory,
                        calculationFactory,
                        mulibValueTransformer,
                        methodHandle,
                        staticVariables,
                        searchRegionArgs,
                        coverageCfg
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

    /**
     * @param args The arguments to the search region, if any
     * @return The path solutions
     * @see Mulib#getPathSolutions
     */
    public List<PathSolution> getPathSolutions(Object... args) {
        return _checkExecuteAndLog(args, (arguments) -> generateNewMulibExecutorManagerForPreInitializedContext(arguments).getPathSolutions());
    }

    /**
     * @param args The arguments to the search region, if any
     * @return A path solution, if any could be extracted
     * @see Mulib#getPathSolutions
     */
    public Optional<PathSolution> getPathSolution(Object... args) {
        return _checkExecuteAndLog(args, (arguments) -> generateNewMulibExecutorManagerForPreInitializedContext(arguments).getPathSolution());
    }

    /**
     * Extracts all solutions, if possible under the budgets of this mulib context
     * @param args The arguments to the search region, if any could be found given the budgets
     * @return The solutions
     */
    public List<Solution> getSolutions(Object... args) {
        return _checkExecuteAndLog(args, (arguments) -> generateNewMulibExecutorManagerForPreInitializedContext(arguments).getUpToNSolutions(Integer.MAX_VALUE));
    }

    /**
     * Extracts up to N solutions, if possible under the budgets of this mulib context
     * @param args The arguments to the search region, if any
     * @return The solutions
     */
    public List<Solution> getUpToNSolutions(int N, Object... args) {
        return _checkExecuteAndLog(args, (arguments) -> generateNewMulibExecutorManagerForPreInitializedContext(arguments).getUpToNSolutions(N));
    }

    /**
     * Extracts a solution, if possible under the budgets of this mulib context
     * @param args The arguments to the search region, if any
     * @return A solution, if any could be found given the budgets
     */
    public Optional<Solution> getSolution(Object... args) {
        List<Solution> result = _checkExecuteAndLog(args, (arguments) -> generateNewMulibExecutorManagerForPreInitializedContext(arguments).getUpToNSolutions(1));
        if (result.size() > 0) {
            return Optional.of(result.get(0));
        } else {
            return Optional.empty();
        }
    }

    /**
     * @param methodUnderTest The method under test
     * @param tcgConfigBuilder The config builder
     * @param args The arguments to the search region, if any
     * @return A String representation of the tests
     * @see Mulib#generateTestCases  
     */
    public String generateTestCases(Method methodUnderTest, TcgConfig.TcgConfigBuilder tcgConfigBuilder, Object... args) {
        // First get all path solutions
        List<PathSolution> pathSolutions =
                _checkExecuteAndLog(args, (arguments) -> generateNewMulibExecutorManagerForPreInitializedContext(arguments).getPathSolutions());
        List<TestCase> testCaseList = new ArrayList<>();
        TcgConfig tcgConfig = tcgConfigBuilder.build();
        boolean checkedForCorrectLables = false;
        for (PathSolution ps : pathSolutions) {
            // Create a test case for each path solution
            TestCase testCase = new TestCase(
                    ps instanceof ThrowablePathSolution,
                    ps.getSolution().labels.getIdToLabel(),
                    ps.getSolution().returnValue,
                    ps instanceof IPathSolutionWithBitSetCover ? ((IPathSolutionWithBitSetCover) ps).getCover() : new BitSet(),
                    tcgConfig
            );
            if (!checkedForCorrectLables) {
                // We check for the correct naming of labels
                Solution s = ps.getSolution();
                int numberParameters = methodUnderTest.getParameterCount();
                if (!Modifier.isStatic(methodUnderTest.getModifiers())) {
                    // Must have an object; - this is treated as part of the input
                    numberParameters++;
                }
                BitSet seenLabelsForPosition = new BitSet(numberParameters);
                for (Map.Entry<String, Object> label : s.labels.getIdToLabel().entrySet()) {
                    Matcher matcherForInput = TcgUtility.INPUT_ARGUMENT_NAME_PATTERN.matcher(label.getKey());
                    if (matcherForInput.matches()) {
                        Integer inputNumber = Integer.parseInt(matcherForInput.group(1));
                        seenLabelsForPosition.set(inputNumber);
                    }
                }
                List<Integer> missingParameterNumbers = new ArrayList<>();
                for (int i = 0; i < numberParameters; i++) {
                    if (!seenLabelsForPosition.get(i)) {
                        missingParameterNumbers.add(i);
                    }
                }
                if (!missingParameterNumbers.isEmpty()) {
                    throw new MulibRuntimeException("All input parameters of the method under test must be specified (e.g. via Mulib.remember(...)). " +
                            "Be sure that you name inputs following the scheme 'arg[0-9]+'. The parameters with the following numbers are uninitialized: " + missingParameterNumbers +
                            ". Note that for non-static methods, the object calling the method under test must be named as 'arg0'.");
                }
                checkedForCorrectLables = true;
            }
            testCaseList.add(testCase);
        }
        TestCases testCases = new TestCases(testCaseList, methodUnderTest);

        TestCasesStringGenerator tcg = new TestCasesStringGenerator(testCases, tcgConfig);
        String result = tcg.generateTestClassStringRepresentation();
        return result;
    }

    /**
     * Creates a stream for recurrently calculating an undefined number of solutions from the search region.
     * {@link SolutionIterator} backs this stream.
     * @param batchSizeOfCachedSolutions The number of solutions that are added to the iterator backing this stream
     *                                   in a batch-wise fashion. This can be increased to a number > 1 to increase efficiency
     * @param args The arguments to the search region, if any
     * @return A non-parallel stream of solutions
     * @see #getSolutionIterator(int, Object...)
     */
    public Stream<Solution> getSolutionStream(int batchSizeOfCachedSolutions, Object... args) {
        return StreamSupport.stream(new SolutionSpliterator(generateNewMulibExecutorManagerForPreInitializedContext(args), batchSizeOfCachedSolutions), false);
    }

    /**
     * Creates an iterator that has the added option to shut down the used executor manager explicitly.
     * The used mulib executor manager will terminate either if it can be proven that there are no more solutions
     * in the search region, of if {@link MulibContext.SolutionIterator#terminate()} is called explicitly.
     * @param batchSizeOfCachedSolutions The number of solutions that are added to the iterator in a batch-wise fashion.
     *                                   This can be increased to a number > 1 to increase efficiency
     * @param args The arguments to the search region, if any
     * @return An iterator of solutions
     */
    public SolutionIterator getSolutionIterator(int batchSizeOfCachedSolutions, Object... args) {
        return new SolutionIterator(generateNewMulibExecutorManagerForPreInitializedContext(args), batchSizeOfCachedSolutions);
    }

    static class SolutionSpliterator implements Spliterator<Solution> {
        private final SolutionIterator solutionIterator;
        SolutionSpliterator(MulibExecutorManager mulibExecutorManager, int batchSizeOfCachedSolutions) {
            this.solutionIterator = new SolutionIterator(mulibExecutorManager, batchSizeOfCachedSolutions);
        }

        @Override
        public boolean tryAdvance(Consumer<? super Solution> action) {
            if (solutionIterator.hasNext()) {
                Solution s = solutionIterator.next();
                action.accept(s);
                return true;
            }
            solutionIterator.terminate();
            return false;
        }

        @Override
        public Spliterator<Solution> trySplit() {
            return null;
        }

        @Override
        public long estimateSize() {
            return 0;
        }

        @Override
        public int characteristics() {
            return 0;
        }
    }

    /**
     * Implements an iterator using an executor manager to retrieve solutions with.
     * This type is exposed to offer the {@link #terminate()} method for shutting down the executor manager explicitly.
     */
    public static class SolutionIterator implements Iterator<Solution> {
        private final MulibExecutorManager mulibExecutorManager;
        private final int batchSizeOfCachedSolutions;
        private final ArrayDeque<Solution> solutions = new ArrayDeque<>();

        SolutionIterator(MulibExecutorManager mulibExecutorManager, int batchSizeOfCachedSolutions) {
            this.mulibExecutorManager = mulibExecutorManager;
            this.batchSizeOfCachedSolutions = batchSizeOfCachedSolutions;
        }

        @Override
        public boolean hasNext() {
            if (!solutions.isEmpty()) {
                return true;
            }
            // We do not change the remaining system state. Only the state of the MulibExecutorManager is being changed.
            // Therefore, we hold that the @Pure contract still is maintained, even if calculating new solutions
            solutions.addAll(mulibExecutorManager.getUpToNSolutions(batchSizeOfCachedSolutions, false));
            if (solutions.isEmpty()) {
                mulibExecutorManager.terminate();
                return false;
            }
            return true;
        }

        @Override
        public Solution next() {
            if (!hasNext()) {
                throw new NoSuchElementException("No more elements");
            }
            assert !solutions.isEmpty();
            return solutions.pop();
        }

        /**
         * Terminate the executor manager.
         */
        public void terminate() {
            mulibExecutorManager.terminate();
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
        } else if (type == Character.class) {
            mulibWrapper = Schar.class;
            javaWrapper = Character.class;
        } else {
            throw new NotYetImplementedException();
        }
        return mulibWrapper.isAssignableFrom(checkIfWrapper) || javaWrapper == checkIfWrapper;
    }

}
