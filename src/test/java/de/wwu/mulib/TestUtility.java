package de.wwu.mulib;

import de.wwu.mulib.search.executors.SearchStrategy;
import de.wwu.mulib.search.trees.ChoiceOptionDeques;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.search.trees.Solution;
import de.wwu.mulib.solving.Solvers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static de.wwu.mulib.search.executors.SearchStrategy.*;

public final class TestUtility {

    private static final int longSingleThreadedExecutorsCheckBatchSize = 6;
    private static final int longParallelExecutorsCheckBatchSize = 2;
    private static final int quickParallelExecutorsCheckBatchSize = 3;

    private static final boolean longSingleThreadedExecutorsCheck = false;
    private static final boolean longParallelExecutorsCheck =       false;
    private static final boolean quickParallelExecutorsCheck =      false;
    private static final boolean quickCheck =                       true;
    private static final boolean runWithCfg =                       false;

    public static final long TEST_FIXED_ACTUAL_CP_BUDGET = 500;
    public static final String TEST_BUILD_PATH = "build/classes/java/test/";

    private TestUtility() {}

    private static List<MulibConfig.MulibConfigBuilder> quickCheck() {
        return List.of(
                MulibConfig.builder()
                        .setGLOBAL_SOLVER_TYPE(Solvers.Z3_INCREMENTAL)
                        .setGLOBAL_SEARCH_STRATEGY(DSAS)
                        .setADDITIONAL_PARALLEL_SEARCH_STRATEGIES(DSAS, DSAS, DSAS)
//                        .setINCR_ACTUAL_CP_BUDGET(6)
                        .setHIGH_LEVEL_FREE_ARRAY_THEORY(true)
                        .setCHOICE_OPTION_DEQUE_TYPE(ChoiceOptionDeques.DIRECT_ACCESS)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET)
                        .setTRANSF_WRITE_TO_FILE(true)
                        .setTRANSF_LOAD_WITH_SYSTEM_CLASSLOADER(true)
                        .setTRANSF_OVERWRITE_FILE_FOR_SYSTEM_CLASSLOADER(true)
                        .setTRANSF_GENERATED_CLASSES_PATH(TEST_BUILD_PATH)
        );
    }

    private static List<MulibConfig.MulibConfigBuilder> quickParallelExecutorsCheck() {
        return List.of(
                MulibConfig.builder()
                        .setGLOBAL_SOLVER_TYPE(Solvers.Z3_INCREMENTAL)
                        .setGLOBAL_SEARCH_STRATEGY(DFS)
                        .setADDITIONAL_PARALLEL_SEARCH_STRATEGIES(IDDFS, BFS, DSAS)
                        .setINCR_ACTUAL_CP_BUDGET(6)
                        .setCHOICE_OPTION_DEQUE_TYPE(ChoiceOptionDeques.DIRECT_ACCESS)
                        .setENLIST_LEAVES(true)
                        .assumeMulibDefaultValueRanges()
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET),
                MulibConfig.builder()
                        .setGLOBAL_SOLVER_TYPE(Solvers.Z3_GLOBAL_LEARNING)
                        .setGLOBAL_SEARCH_STRATEGY(BFS)
                        .setADDITIONAL_PARALLEL_SEARCH_STRATEGIES(IDDFS, IDDSAS, BFS)
                        .setINCR_ACTUAL_CP_BUDGET(2)
                        .setHIGH_LEVEL_FREE_ARRAY_THEORY(true)
                        .setCHOICE_OPTION_DEQUE_TYPE(ChoiceOptionDeques.DIRECT_ACCESS)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET),
                MulibConfig.builder()
                        .setGLOBAL_SOLVER_TYPE(Solvers.JSMT_SMTINTERPOL)
                        .setGLOBAL_SEARCH_STRATEGY(IDDSAS)
                        .setADDITIONAL_PARALLEL_SEARCH_STRATEGIES(DSAS, IDDSAS, DSAS)
                        .setINCR_ACTUAL_CP_BUDGET(3)
                        .setTRANSF_GENERATED_CLASSES_PATH(TEST_BUILD_PATH)
                        .setTRANSF_LOAD_WITH_SYSTEM_CLASSLOADER(true)
                        .setCONCOLIC(true)
                        .setHIGH_LEVEL_FREE_ARRAY_THEORY(true)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET)
        );
    }

    private static List<MulibConfig.MulibConfigBuilder> longParallelCheck() {
        return List.of(
                MulibConfig.builder()
                        .setGLOBAL_SEARCH_STRATEGY(DFS)
                        .setADDITIONAL_PARALLEL_SEARCH_STRATEGIES(DFS, DFS)
                        .setCHOICE_OPTION_DEQUE_TYPE(ChoiceOptionDeques.DIRECT_ACCESS)
                        .setTREAT_BOOLEANS_AS_INTS(true)
                        .setHIGH_LEVEL_FREE_ARRAY_THEORY(true)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET),
                MulibConfig.builder()
                        .setGLOBAL_SEARCH_STRATEGY(BFS)
                        .setGLOBAL_SOLVER_TYPE(Solvers.Z3_GLOBAL_LEARNING)
                        .assumeMulibDefaultValueRanges()
                        .setADDITIONAL_PARALLEL_SEARCH_STRATEGIES(BFS, BFS)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET),
                MulibConfig.builder()
                        .setGLOBAL_SEARCH_STRATEGY(IDDFS)
                        .setADDITIONAL_PARALLEL_SEARCH_STRATEGIES(IDDSAS, IDDSAS)
                        .setINCR_ACTUAL_CP_BUDGET(9)
                        .setTRANSF_GENERATED_CLASSES_PATH(TEST_BUILD_PATH)
                        .setTRANSF_LOAD_WITH_SYSTEM_CLASSLOADER(true)
                        .setHIGH_LEVEL_FREE_ARRAY_THEORY(true)
                        .assumeMulibDefaultValueRanges()
                        .setCHOICE_OPTION_DEQUE_TYPE(ChoiceOptionDeques.DIRECT_ACCESS)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET),
                MulibConfig.builder()
                        .setGLOBAL_SEARCH_STRATEGY(IDDFS)
                        .setADDITIONAL_PARALLEL_SEARCH_STRATEGIES(IDDFS, IDDFS)
                        .setINCR_ACTUAL_CP_BUDGET(5)
                        .setTRANSF_GENERATED_CLASSES_PATH(TEST_BUILD_PATH)
                        .setTRANSF_LOAD_WITH_SYSTEM_CLASSLOADER(true)
                        .setHIGH_LEVEL_FREE_ARRAY_THEORY(false)
                        .setCONCOLIC(true)
                        .setCHOICE_OPTION_DEQUE_TYPE(ChoiceOptionDeques.DIRECT_ACCESS)
                        .assumeMulibDefaultValueRanges()
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET),
                MulibConfig.builder()
                        .setGLOBAL_SOLVER_TYPE(Solvers.Z3_INCREMENTAL)
                        .setADDITIONAL_PARALLEL_SEARCH_STRATEGIES(DFS, IDDSAS, IDDSAS)
                        .setGLOBAL_SEARCH_STRATEGY(DSAS)
                        .setINCR_ACTUAL_CP_BUDGET(13)
                        .setTREAT_BOOLEANS_AS_INTS(true)
                        .setCHOICE_OPTION_DEQUE_TYPE(ChoiceOptionDeques.DIRECT_ACCESS)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET),
                MulibConfig.builder()
                        .setGLOBAL_SOLVER_TYPE(Solvers.JSMT_SMTINTERPOL)
                        .setADDITIONAL_PARALLEL_SEARCH_STRATEGIES(BFS, BFS)
                        .setGLOBAL_SEARCH_STRATEGY(DSAS)
                        .setTREAT_BOOLEANS_AS_INTS(true)
                        .setCONCOLIC(true)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET),
                MulibConfig.builder()
                        .setADDITIONAL_PARALLEL_SEARCH_STRATEGIES(DFS, IDDSAS, DSAS, IDDFS)
                        .assumeMulibDefaultValueRanges()
                        .setCHOICE_OPTION_DEQUE_TYPE(ChoiceOptionDeques.DIRECT_ACCESS)
                        .setINCR_ACTUAL_CP_BUDGET(8)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET)
                        .setGLOBAL_SEARCH_STRATEGY(BFS)
        );
    }

    private static List<MulibConfig.MulibConfigBuilder> longSingleThreadedExecutorsCheck() {
        return List.of(
                MulibConfig.builder()
                        .setGLOBAL_SEARCH_STRATEGY(DFS)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET),
                MulibConfig.builder()
                        .setGLOBAL_SEARCH_STRATEGY(BFS)
                        .setCHOICE_OPTION_DEQUE_TYPE(ChoiceOptionDeques.DIRECT_ACCESS)
                        .setGLOBAL_SOLVER_TYPE(Solvers.Z3_GLOBAL_LEARNING)
                        .setTRANSF_GENERATED_CLASSES_PATH(TEST_BUILD_PATH)
                        .setTRANSF_LOAD_WITH_SYSTEM_CLASSLOADER(true)
                        .setTRANSF_OVERWRITE_FILE_FOR_SYSTEM_CLASSLOADER(true)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET),
                MulibConfig.builder()
                        .setGLOBAL_SEARCH_STRATEGY(IDDFS)
                        .setINCR_ACTUAL_CP_BUDGET(3)
                        .setHIGH_LEVEL_FREE_ARRAY_THEORY(true)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET),
                MulibConfig.builder()
                        .setINCR_ACTUAL_CP_BUDGET(2)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET)
                        .setGLOBAL_SEARCH_STRATEGY(IDDFS)
                        .setTRANSF_GENERATED_CLASSES_PATH(TEST_BUILD_PATH)
                        .setTRANSF_LOAD_WITH_SYSTEM_CLASSLOADER(true),
                MulibConfig.builder()
                        .setINCR_ACTUAL_CP_BUDGET(2)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET)
                        .setGLOBAL_SEARCH_STRATEGY(IDDFS)
                        .setGLOBAL_SOLVER_TYPE(Solvers.Z3_GLOBAL_LEARNING)
                        .setTRANSF_GENERATED_CLASSES_PATH(TEST_BUILD_PATH)
                        .setTRANSF_OVERWRITE_FILE_FOR_SYSTEM_CLASSLOADER(true)
                        .setTRANSF_LOAD_WITH_SYSTEM_CLASSLOADER(true)
                        .setCONCOLIC(true),
                MulibConfig.builder()
                        .setINCR_ACTUAL_CP_BUDGET(16)
                        .setGLOBAL_SEARCH_STRATEGY(SearchStrategy.IDDFS)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET)
                        .setGLOBAL_SOLVER_TYPE(Solvers.JSMT_SMTINTERPOL)
                        .setENLIST_LEAVES(true)
                        .setHIGH_LEVEL_FREE_ARRAY_THEORY(true),
                MulibConfig.builder()
                        .setGLOBAL_SEARCH_STRATEGY(DSAS)
                        .setCONCOLIC(true)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET)
                        .assumeMulibDefaultValueRanges(),
                MulibConfig.builder()
                        .setGLOBAL_SEARCH_STRATEGY(IDDSAS)
                        .setINCR_ACTUAL_CP_BUDGET(3)
                        .setHIGH_LEVEL_FREE_ARRAY_THEORY(true)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET),
                MulibConfig.builder()
                        .setINCR_ACTUAL_CP_BUDGET(2)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET)
                        .setGLOBAL_SEARCH_STRATEGY(IDDSAS)
                        .setTRANSF_GENERATED_CLASSES_PATH(TEST_BUILD_PATH)
                        .setTRANSF_LOAD_WITH_SYSTEM_CLASSLOADER(true),
                MulibConfig.builder()
                        .setINCR_ACTUAL_CP_BUDGET(2)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET)
                        .setGLOBAL_SEARCH_STRATEGY(IDDSAS)
                        .setGLOBAL_SOLVER_TYPE(Solvers.Z3_GLOBAL_LEARNING)
                        .setTRANSF_GENERATED_CLASSES_PATH(TEST_BUILD_PATH)
                        .setTRANSF_OVERWRITE_FILE_FOR_SYSTEM_CLASSLOADER(true)
                        .setTRANSF_LOAD_WITH_SYSTEM_CLASSLOADER(true)
                        .setCONCOLIC(true),
                MulibConfig.builder()
                        .setINCR_ACTUAL_CP_BUDGET(16)
                        .setGLOBAL_SEARCH_STRATEGY(SearchStrategy.IDDSAS)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET)
                        .setGLOBAL_SOLVER_TYPE(Solvers.Z3_INCREMENTAL)
                        .setENLIST_LEAVES(true)
                        .setHIGH_LEVEL_FREE_ARRAY_THEORY(true)
        );
    }

    private static List<MulibConfig.MulibConfigBuilder> initBuilders() {
        List<MulibConfig.MulibConfigBuilder> result = new ArrayList<>();
        if (longSingleThreadedExecutorsCheck) {
            result.addAll(longSingleThreadedExecutorsCheck());
        }
        if (longParallelExecutorsCheck) {
            result.addAll(longParallelCheck());
        }
        if (quickParallelExecutorsCheck) {
            result.addAll(quickParallelExecutorsCheck());
        }
        if (quickCheck) {
            result.addAll(quickCheck());
        }
        return List.copyOf(result);
    }

    public static void getAllSolutions(
            Consumer<MulibConfig.MulibConfigBuilder> mbToList,
            String testedMethodName) {
        getAllSolutions(b -> b, mbToList, testedMethodName);
    }

    public static void getAllSolutions(
            Function<MulibConfig.MulibConfigBuilder, MulibConfig.MulibConfigBuilder> adjustment,
            Consumer<MulibConfig.MulibConfigBuilder> mulibConfigToList,
            String testedMethodName) {
        if (quickCheck) {
            executeTestConfigsBatched(quickCheck(), adjustment, mulibConfigToList, testedMethodName, 1);
        }
        if (quickParallelExecutorsCheck) {
            executeTestConfigsBatched(quickParallelExecutorsCheck(), adjustment, mulibConfigToList, testedMethodName, quickParallelExecutorsCheckBatchSize);
        }
        if (longParallelExecutorsCheck) {
            executeTestConfigsBatched(longParallelCheck(), adjustment, mulibConfigToList, testedMethodName, longParallelExecutorsCheckBatchSize);
        }
        if (longSingleThreadedExecutorsCheck) {
            executeTestConfigsBatched(longSingleThreadedExecutorsCheck(), adjustment, mulibConfigToList, testedMethodName, longSingleThreadedExecutorsCheckBatchSize);
        }
    }

    private static void executeTestConfigsBatched(
            List<MulibConfig.MulibConfigBuilder> executeTestsWith,
            Function<MulibConfig.MulibConfigBuilder, MulibConfig.MulibConfigBuilder> adjustment,
            Consumer<MulibConfig.MulibConfigBuilder> mulibConfigToList,
            String testedMethodName,
            int batchSize) {
        int currentFirstElementOfNextList = 0;
        while (currentFirstElementOfNextList < executeTestsWith.size()) {
            int nextEndpoint = currentFirstElementOfNextList + batchSize;
            List<MulibConfig.MulibConfigBuilder> currentBatch =
                    executeTestsWith.subList(
                            currentFirstElementOfNextList,
                            Math.min(nextEndpoint, executeTestsWith.size())
                    );
            currentBatch.parallelStream().forEach(mcb -> {
                MulibConfig.MulibConfigBuilder mb = adjustment.apply(mcb);
                mb.setTRANSF_VALIDATE_TRANSFORMATION(true);
                if (runWithCfg) {
                    boolean useCfgGuidance = !mb.isConcolic();
                    mb.setTRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID(useCfgGuidance, false, true);
                }
                Mulib.log.info("Started '" + testedMethodName + "' with config " + mb.build());
                mulibConfigToList.accept(mb);
                Mulib.log.info("Returns for '" + testedMethodName + "' with config " + mb.build());
            });
            currentFirstElementOfNextList = nextEndpoint;
        }
    }

    public static void getSolution(
            Consumer<MulibConfig.MulibConfigBuilder> mulibConfigToPossibleSolution) {
        getSolution(b -> b, mulibConfigToPossibleSolution);
    }

    public static void getSolution(
            Function<MulibConfig.MulibConfigBuilder, MulibConfig.MulibConfigBuilder> adjustment,
            Consumer<MulibConfig.MulibConfigBuilder> mulibConfigToPossibleSolution) {
        final List<MulibConfig.MulibConfigBuilder> executeTestsWith = initBuilders();
        for (MulibConfig.MulibConfigBuilder mulibConfigBuilder : executeTestsWith) {
            MulibConfig.MulibConfigBuilder config = adjustment.apply(mulibConfigBuilder);
            mulibConfigToPossibleSolution.accept(config);
        }
    }

    public static List<PathSolution> executeMulib(
            String methodName,
            Class<?> containingClass,
            MulibConfig.MulibConfigBuilder mb,
            boolean transformationRequired) {
        return executeMulib(
                methodName,
                containingClass,
                mb,
                transformationRequired,
                new Class[0],
                new Object[0]
        );
    }

    public static List<PathSolution> executeMulib(
            String methodName,
            Class<?> containingClass,
            MulibConfig.MulibConfigBuilder mb,
            boolean transformationRequired,
            Class<?>[] argTypes,
            Object[] args) {
        mb.setTRANSF_TRANSFORMATION_REQUIRED(transformationRequired);
        MulibContext mc = Mulib.getMulibContext(containingClass, methodName, mb, argTypes);
        List<PathSolution> result = mc.getAllPathSolutions(args);
        return result;
    }

    public static Optional<PathSolution> executeMulibForOne(
            String methodName,
            Class<?> containingClass,
            MulibConfig.MulibConfigBuilder mb,
            boolean transformationRequired) {
        return executeMulibForOne(
                methodName,
                containingClass,
                mb,
                transformationRequired,
                new Class[0],
                new Object[0]
        );
    }

    public static Optional<PathSolution> executeMulibForOne(
            String methodName,
            Class<?> containingClass,
            MulibConfig.MulibConfigBuilder mb,
            boolean transformationRequired,
            Class<?>[] argTypes,
            Object[] args) {
        mb.setTRANSF_TRANSFORMATION_REQUIRED(transformationRequired);
        MulibContext mc = Mulib.getMulibContext(containingClass, methodName, mb, argTypes);
        Optional<PathSolution> result = mc.getPathSolution(args);
        return result;
    }

    public static List<Solution> getUpToNSolutions(
            int N,
            String methodName,
            Class<?> containingClass,
            MulibConfig.MulibConfigBuilder mb) {
        return getUpToNSolutions(N, methodName, containingClass, mb, new Class[0], new Object[0]);
    }

    public static List<Solution> getUpToNSolutions(
            int N,
            String methodName,
            Class<?> containingClass,
            MulibConfig.MulibConfigBuilder mb,
            Class<?>[] argTypes,
            Object[] args) {
        return getUpToNSolutions(N, methodName, containingClass, mb, true, argTypes, args);
    }

    public static List<Solution> getUpToNSolutions(
            int N,
            String methodName,
            Class<?> containingClass,
            MulibConfig.MulibConfigBuilder mb,
            boolean transformationRequired,
            Class<?>[] argTypes,
            Object[] args) {
        mb.setTRANSF_TRANSFORMATION_REQUIRED(transformationRequired);
        MulibContext mc = Mulib.getMulibContext(containingClass, methodName, mb, argTypes);
        return mc.getUpToNSolutions(N, args);
    }
}