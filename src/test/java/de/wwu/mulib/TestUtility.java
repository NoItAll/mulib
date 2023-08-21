package de.wwu.mulib;

import de.wwu.mulib.search.executors.SearchStrategy;
import de.wwu.mulib.search.trees.ChoiceOptionDeques;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.solving.Solution;
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
                        .setSOLVER_GLOBAL_TYPE(Solvers.Z3_INCREMENTAL)
                        .setSEARCH_MAIN_STRATEGY(DSAS)
                        .setSEARCH_ADDITIONAL_PARALLEL_STRATEGIES(DSAS, DSAS, DSAS)
//                        .setINCR_ACTUAL_CP_BUDGET(6)
                        .setSOLVER_HIGH_LEVEL_SYMBOLIC_OBJECT_APPROACH(true)
                        .setSEARCH_CHOICE_OPTION_DEQUE_TYPE(ChoiceOptionDeques.DIRECT_ACCESS)
                        .setBUDGET_FIXED_ACTUAL_CP(TEST_FIXED_ACTUAL_CP_BUDGET)
                        .setTRANSF_WRITE_TO_FILE(true)
                        .setTRANSF_LOAD_WITH_SYSTEM_CLASSLOADER(true)
                        .setTRANSF_OVERWRITE_FILE_FOR_SYSTEM_CLASSLOADER(true)
                        .setTRANSF_GENERATED_CLASSES_PATH(TEST_BUILD_PATH)
        );
    }

    private static List<MulibConfig.MulibConfigBuilder> quickParallelExecutorsCheck() {
        return List.of(
                MulibConfig.builder()
                        .setSOLVER_GLOBAL_TYPE(Solvers.Z3_INCREMENTAL)
                        .setSEARCH_MAIN_STRATEGY(DFS)
                        .setSEARCH_ADDITIONAL_PARALLEL_STRATEGIES(IDDFS, BFS, DSAS)
                        .setBUDGET_INCR_ACTUAL_CP(6)
                        .setSEARCH_CHOICE_OPTION_DEQUE_TYPE(ChoiceOptionDeques.DIRECT_ACCESS)
                        .setTREE_ENLIST_LEAVES(true)
                        .assumeMulibDefaultValueRanges()
                        .setBUDGET_FIXED_ACTUAL_CP(TEST_FIXED_ACTUAL_CP_BUDGET),
                MulibConfig.builder()
                        .setSOLVER_GLOBAL_TYPE(Solvers.Z3_GLOBAL_LEARNING)
                        .setSEARCH_MAIN_STRATEGY(BFS)
                        .setSEARCH_ADDITIONAL_PARALLEL_STRATEGIES(IDDFS, IDDSAS, BFS)
                        .setBUDGET_INCR_ACTUAL_CP(2)
                        .setSOLVER_HIGH_LEVEL_SYMBOLIC_OBJECT_APPROACH(true)
                        .setSEARCH_CHOICE_OPTION_DEQUE_TYPE(ChoiceOptionDeques.DIRECT_ACCESS)
                        .setBUDGET_FIXED_ACTUAL_CP(TEST_FIXED_ACTUAL_CP_BUDGET),
                MulibConfig.builder()
                        .setSOLVER_GLOBAL_TYPE(Solvers.JSMT_SMTINTERPOL)
                        .setSEARCH_MAIN_STRATEGY(IDDSAS)
                        .setSEARCH_ADDITIONAL_PARALLEL_STRATEGIES(DSAS, IDDSAS, DSAS)
                        .setBUDGET_INCR_ACTUAL_CP(3)
                        .setTRANSF_GENERATED_CLASSES_PATH(TEST_BUILD_PATH)
                        .setTRANSF_LOAD_WITH_SYSTEM_CLASSLOADER(true)
                        .setSEARCH_CONCOLIC(true)
                        .setSOLVER_HIGH_LEVEL_SYMBOLIC_OBJECT_APPROACH(true)
                        .setBUDGET_FIXED_ACTUAL_CP(TEST_FIXED_ACTUAL_CP_BUDGET)
        );
    }

    private static List<MulibConfig.MulibConfigBuilder> longParallelCheck() {
        return List.of(
                MulibConfig.builder()
                        .setSEARCH_MAIN_STRATEGY(DFS)
                        .setSEARCH_ADDITIONAL_PARALLEL_STRATEGIES(DFS, DFS)
                        .setSEARCH_CHOICE_OPTION_DEQUE_TYPE(ChoiceOptionDeques.DIRECT_ACCESS)
                        .setVALS_TREAT_BOOLEANS_AS_INTS(true)
                        .setSOLVER_HIGH_LEVEL_SYMBOLIC_OBJECT_APPROACH(true)
                        .setBUDGET_FIXED_ACTUAL_CP(TEST_FIXED_ACTUAL_CP_BUDGET),
                MulibConfig.builder()
                        .setSEARCH_MAIN_STRATEGY(BFS)
                        .setSOLVER_GLOBAL_TYPE(Solvers.Z3_GLOBAL_LEARNING)
                        .assumeMulibDefaultValueRanges()
                        .setSEARCH_ADDITIONAL_PARALLEL_STRATEGIES(BFS, BFS)
                        .setBUDGET_FIXED_ACTUAL_CP(TEST_FIXED_ACTUAL_CP_BUDGET),
                MulibConfig.builder()
                        .setSEARCH_MAIN_STRATEGY(IDDFS)
                        .setSEARCH_ADDITIONAL_PARALLEL_STRATEGIES(IDDSAS, IDDSAS)
                        .setBUDGET_INCR_ACTUAL_CP(9)
                        .setTRANSF_GENERATED_CLASSES_PATH(TEST_BUILD_PATH)
                        .setTRANSF_LOAD_WITH_SYSTEM_CLASSLOADER(true)
                        .setSOLVER_HIGH_LEVEL_SYMBOLIC_OBJECT_APPROACH(true)
                        .assumeMulibDefaultValueRanges()
                        .setSEARCH_CHOICE_OPTION_DEQUE_TYPE(ChoiceOptionDeques.DIRECT_ACCESS)
                        .setBUDGET_FIXED_ACTUAL_CP(TEST_FIXED_ACTUAL_CP_BUDGET),
                MulibConfig.builder()
                        .setSEARCH_MAIN_STRATEGY(IDDFS)
                        .setSEARCH_ADDITIONAL_PARALLEL_STRATEGIES(IDDFS, IDDFS)
                        .setBUDGET_INCR_ACTUAL_CP(5)
                        .setTRANSF_GENERATED_CLASSES_PATH(TEST_BUILD_PATH)
                        .setTRANSF_LOAD_WITH_SYSTEM_CLASSLOADER(true)
                        .setSOLVER_HIGH_LEVEL_SYMBOLIC_OBJECT_APPROACH(false)
                        .setSEARCH_CONCOLIC(true)
                        .setSEARCH_CHOICE_OPTION_DEQUE_TYPE(ChoiceOptionDeques.DIRECT_ACCESS)
                        .assumeMulibDefaultValueRanges()
                        .setBUDGET_FIXED_ACTUAL_CP(TEST_FIXED_ACTUAL_CP_BUDGET),
                MulibConfig.builder()
                        .setSOLVER_GLOBAL_TYPE(Solvers.Z3_INCREMENTAL)
                        .setSEARCH_ADDITIONAL_PARALLEL_STRATEGIES(DFS, IDDSAS, IDDSAS)
                        .setSEARCH_MAIN_STRATEGY(DSAS)
                        .setBUDGET_INCR_ACTUAL_CP(13)
                        .setVALS_TREAT_BOOLEANS_AS_INTS(true)
                        .setSEARCH_CHOICE_OPTION_DEQUE_TYPE(ChoiceOptionDeques.DIRECT_ACCESS)
                        .setBUDGET_FIXED_ACTUAL_CP(TEST_FIXED_ACTUAL_CP_BUDGET),
                MulibConfig.builder()
                        .setSOLVER_GLOBAL_TYPE(Solvers.JSMT_SMTINTERPOL)
                        .setSEARCH_ADDITIONAL_PARALLEL_STRATEGIES(BFS, BFS)
                        .setSEARCH_MAIN_STRATEGY(DSAS)
                        .setVALS_TREAT_BOOLEANS_AS_INTS(true)
                        .setSEARCH_CONCOLIC(true)
                        .setBUDGET_FIXED_ACTUAL_CP(TEST_FIXED_ACTUAL_CP_BUDGET),
                MulibConfig.builder()
                        .setSEARCH_ADDITIONAL_PARALLEL_STRATEGIES(DFS, IDDSAS, DSAS, IDDFS)
                        .assumeMulibDefaultValueRanges()
                        .setSEARCH_CHOICE_OPTION_DEQUE_TYPE(ChoiceOptionDeques.DIRECT_ACCESS)
                        .setBUDGET_INCR_ACTUAL_CP(8)
                        .setBUDGET_FIXED_ACTUAL_CP(TEST_FIXED_ACTUAL_CP_BUDGET)
                        .setSEARCH_MAIN_STRATEGY(BFS)
        );
    }

    private static List<MulibConfig.MulibConfigBuilder> longSingleThreadedExecutorsCheck() {
        return List.of(
                MulibConfig.builder()
                        .setSEARCH_MAIN_STRATEGY(DFS)
                        .setBUDGET_FIXED_ACTUAL_CP(TEST_FIXED_ACTUAL_CP_BUDGET),
                MulibConfig.builder()
                        .setSEARCH_MAIN_STRATEGY(BFS)
                        .setSEARCH_CHOICE_OPTION_DEQUE_TYPE(ChoiceOptionDeques.DIRECT_ACCESS)
                        .setSOLVER_GLOBAL_TYPE(Solvers.Z3_GLOBAL_LEARNING)
                        .setTRANSF_GENERATED_CLASSES_PATH(TEST_BUILD_PATH)
                        .setTRANSF_LOAD_WITH_SYSTEM_CLASSLOADER(true)
                        .setTRANSF_OVERWRITE_FILE_FOR_SYSTEM_CLASSLOADER(true)
                        .setBUDGET_FIXED_ACTUAL_CP(TEST_FIXED_ACTUAL_CP_BUDGET),
                MulibConfig.builder()
                        .setSEARCH_MAIN_STRATEGY(IDDFS)
                        .setBUDGET_INCR_ACTUAL_CP(3)
                        .setSOLVER_HIGH_LEVEL_SYMBOLIC_OBJECT_APPROACH(true)
                        .setBUDGET_FIXED_ACTUAL_CP(TEST_FIXED_ACTUAL_CP_BUDGET),
                MulibConfig.builder()
                        .setBUDGET_INCR_ACTUAL_CP(2)
                        .setBUDGET_FIXED_ACTUAL_CP(TEST_FIXED_ACTUAL_CP_BUDGET)
                        .setSEARCH_MAIN_STRATEGY(IDDFS)
                        .setTRANSF_GENERATED_CLASSES_PATH(TEST_BUILD_PATH)
                        .setTRANSF_LOAD_WITH_SYSTEM_CLASSLOADER(true),
                MulibConfig.builder()
                        .setBUDGET_INCR_ACTUAL_CP(2)
                        .setBUDGET_FIXED_ACTUAL_CP(TEST_FIXED_ACTUAL_CP_BUDGET)
                        .setSEARCH_MAIN_STRATEGY(IDDFS)
                        .setSOLVER_GLOBAL_TYPE(Solvers.Z3_GLOBAL_LEARNING)
                        .setTRANSF_GENERATED_CLASSES_PATH(TEST_BUILD_PATH)
                        .setTRANSF_OVERWRITE_FILE_FOR_SYSTEM_CLASSLOADER(true)
                        .setTRANSF_LOAD_WITH_SYSTEM_CLASSLOADER(true)
                        .setSEARCH_CONCOLIC(true),
                MulibConfig.builder()
                        .setBUDGET_INCR_ACTUAL_CP(16)
                        .setSEARCH_MAIN_STRATEGY(SearchStrategy.IDDFS)
                        .setBUDGET_FIXED_ACTUAL_CP(TEST_FIXED_ACTUAL_CP_BUDGET)
                        .setSOLVER_GLOBAL_TYPE(Solvers.JSMT_SMTINTERPOL)
                        .setTREE_ENLIST_LEAVES(true)
                        .setSOLVER_HIGH_LEVEL_SYMBOLIC_OBJECT_APPROACH(true),
                MulibConfig.builder()
                        .setSEARCH_MAIN_STRATEGY(DSAS)
                        .setSEARCH_CONCOLIC(true)
                        .setBUDGET_FIXED_ACTUAL_CP(TEST_FIXED_ACTUAL_CP_BUDGET)
                        .assumeMulibDefaultValueRanges(),
                MulibConfig.builder()
                        .setSEARCH_MAIN_STRATEGY(IDDSAS)
                        .setBUDGET_INCR_ACTUAL_CP(3)
                        .setSOLVER_HIGH_LEVEL_SYMBOLIC_OBJECT_APPROACH(true)
                        .setBUDGET_FIXED_ACTUAL_CP(TEST_FIXED_ACTUAL_CP_BUDGET),
                MulibConfig.builder()
                        .setBUDGET_INCR_ACTUAL_CP(2)
                        .setBUDGET_FIXED_ACTUAL_CP(TEST_FIXED_ACTUAL_CP_BUDGET)
                        .setSEARCH_MAIN_STRATEGY(IDDSAS)
                        .setTRANSF_GENERATED_CLASSES_PATH(TEST_BUILD_PATH)
                        .setTRANSF_LOAD_WITH_SYSTEM_CLASSLOADER(true),
                MulibConfig.builder()
                        .setBUDGET_INCR_ACTUAL_CP(2)
                        .setBUDGET_FIXED_ACTUAL_CP(TEST_FIXED_ACTUAL_CP_BUDGET)
                        .setSEARCH_MAIN_STRATEGY(IDDSAS)
                        .setSOLVER_GLOBAL_TYPE(Solvers.Z3_GLOBAL_LEARNING)
                        .setTRANSF_GENERATED_CLASSES_PATH(TEST_BUILD_PATH)
                        .setTRANSF_OVERWRITE_FILE_FOR_SYSTEM_CLASSLOADER(true)
                        .setTRANSF_LOAD_WITH_SYSTEM_CLASSLOADER(true)
                        .setSEARCH_CONCOLIC(true),
                MulibConfig.builder()
                        .setBUDGET_INCR_ACTUAL_CP(16)
                        .setSEARCH_MAIN_STRATEGY(SearchStrategy.IDDSAS)
                        .setBUDGET_FIXED_ACTUAL_CP(TEST_FIXED_ACTUAL_CP_BUDGET)
                        .setSOLVER_GLOBAL_TYPE(Solvers.Z3_INCREMENTAL)
                        .setTREE_ENLIST_LEAVES(true)
                        .setSOLVER_HIGH_LEVEL_SYMBOLIC_OBJECT_APPROACH(true)
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
        List<PathSolution> result = mc.getPathSolutions(args);
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