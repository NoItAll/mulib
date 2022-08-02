package de.wwu.mulib;

import de.wwu.mulib.search.executors.SearchStrategy;
import de.wwu.mulib.search.trees.ChoiceOptionDeques;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.solving.Solvers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.wwu.mulib.search.executors.SearchStrategy.*;

public final class TestUtility {

    private static final boolean longSingleThreadedExecutorsCheck = false;
    private static final boolean longParallelExecutorsCheck =       false;
    private static final boolean quickParallelExecutorsCheck =      false;
    private static final boolean quickCheck =                       true;

    public static final long TEST_FIXED_POSSIBLE_CP_BUDGET = 1000;
    public static final long TEST_FIXED_ACTUAL_CP_BUDGET = 500;
    public static final String TEST_BUILD_PATH = "build/classes/java/test/";

    private TestUtility() {}

    private static List<MulibConfig.MulibConfigBuilder> quickCheck() {
        return List.of(
                MulibConfig.builder()
                        .setGLOBAL_SOLVER_TYPE(Solvers.Z3)
                        .setGLOBAL_SEARCH_STRATEGY(DSAS)
                        .setADDITIONAL_PARALLEL_SEARCH_STRATEGIES(DSAS, DSAS, DSAS)
                        .setCONCOLIC(false)
                        .setGENERATE_NEW_SYM_AFTER_STORE(true)
                        .setCHOICE_OPTION_DEQUE_TYPE(ChoiceOptionDeques.DIRECT_ACCESS)
                        .setFIXED_POSSIBLE_CP_BUDGET(TEST_FIXED_POSSIBLE_CP_BUDGET)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET)
//                        .setTRANSF_LOAD_WITH_SYSTEM_CLASSLOADER(true)
//                        .setTRANSF_GENERATED_CLASSES_PATH(TEST_BUILD_PATH)
        );
    }

    private static List<MulibConfig.MulibConfigBuilder> quickParallelExecutorsCheck() {
        return List.of(
                MulibConfig.builder()
                        .setGLOBAL_SOLVER_TYPE(Solvers.Z3)
                        .setGLOBAL_SEARCH_STRATEGY(DFS)
                        .setADDITIONAL_PARALLEL_SEARCH_STRATEGIES(IDDFS, BFS, DSAS)
                        .setINCR_ACTUAL_CP_BUDGET(6)
                        .setCHOICE_OPTION_DEQUE_TYPE(ChoiceOptionDeques.DIRECT_ACCESS)
                        .setFIXED_POSSIBLE_CP_BUDGET(TEST_FIXED_POSSIBLE_CP_BUDGET)
                        .setENLIST_LEAVES(true)
                        .assumeMulibDefaultValueRanges()
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET),
                MulibConfig.builder()
                        .setGLOBAL_SOLVER_TYPE(Solvers.JSMT_SMTINTERPOL)
                        .setGLOBAL_SEARCH_STRATEGY(DSAS)
                        .setADDITIONAL_PARALLEL_SEARCH_STRATEGIES(DSAS, DSAS, DSAS)
                        .setTRANSF_GENERATED_CLASSES_PATH(TEST_BUILD_PATH)
                        .setTRANSF_LOAD_WITH_SYSTEM_CLASSLOADER(true)
                        .setFIXED_POSSIBLE_CP_BUDGET(TEST_FIXED_POSSIBLE_CP_BUDGET)
                        .setCONCOLIC(true)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET)
        );
    }

    private static List<MulibConfig.MulibConfigBuilder> longParallelCheck() {
        return List.of(
                MulibConfig.builder()
                        .setGLOBAL_SEARCH_STRATEGY(DFS)
                        .setADDITIONAL_PARALLEL_SEARCH_STRATEGIES(DFS, DFS)
                        .setCHOICE_OPTION_DEQUE_TYPE(ChoiceOptionDeques.DIRECT_ACCESS)
                        .setFIXED_POSSIBLE_CP_BUDGET(TEST_FIXED_POSSIBLE_CP_BUDGET)
                        .setTREAT_BOOLEANS_AS_INTS(true)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET),
                MulibConfig.builder()
                        .setGLOBAL_SEARCH_STRATEGY(BFS)
                        .assumeMulibDefaultValueRanges()
                        .setADDITIONAL_PARALLEL_SEARCH_STRATEGIES(BFS, BFS)
                        .setFIXED_POSSIBLE_CP_BUDGET(TEST_FIXED_POSSIBLE_CP_BUDGET)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET),
                MulibConfig.builder()
                        .setGLOBAL_SEARCH_STRATEGY(IDDFS)
                        .setADDITIONAL_PARALLEL_SEARCH_STRATEGIES(IDDFS, IDDFS)
                        .setINCR_ACTUAL_CP_BUDGET(3)
                        .setTRANSF_GENERATED_CLASSES_PATH(TEST_BUILD_PATH)
                        .setTRANSF_LOAD_WITH_SYSTEM_CLASSLOADER(true)
                        .setFIXED_POSSIBLE_CP_BUDGET(TEST_FIXED_POSSIBLE_CP_BUDGET)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET),
                MulibConfig.builder()
                        .setGLOBAL_SOLVER_TYPE(Solvers.JSMT_SMTINTERPOL)
                        .setADDITIONAL_PARALLEL_SEARCH_STRATEGIES(DFS, IDDFS)
                        .setGLOBAL_SEARCH_STRATEGY(DSAS)
                        .setINCR_ACTUAL_CP_BUDGET(13)
                        .setTREAT_BOOLEANS_AS_INTS(true)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET)
                        .setFIXED_POSSIBLE_CP_BUDGET(TEST_FIXED_POSSIBLE_CP_BUDGET),
                MulibConfig.builder()
                        .setADDITIONAL_PARALLEL_SEARCH_STRATEGIES(DFS, BFS, DSAS, IDDFS)
                        .assumeMulibDefaultValueRanges()
                        .setCHOICE_OPTION_DEQUE_TYPE(ChoiceOptionDeques.DIRECT_ACCESS)
                        .setINCR_ACTUAL_CP_BUDGET(8)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET)
                        .setGLOBAL_SEARCH_STRATEGY(BFS)
                        .setFIXED_POSSIBLE_CP_BUDGET(TEST_FIXED_POSSIBLE_CP_BUDGET)
        );
    }

    private static List<MulibConfig.MulibConfigBuilder> longSingleThreadedExecutorsCheck() {
        return List.of(
                MulibConfig.builder()
                        .setGLOBAL_SEARCH_STRATEGY(DFS)
                        .setFIXED_POSSIBLE_CP_BUDGET(TEST_FIXED_POSSIBLE_CP_BUDGET)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET),
                MulibConfig.builder()
                        .setGLOBAL_SEARCH_STRATEGY(BFS)
                        .setCHOICE_OPTION_DEQUE_TYPE(ChoiceOptionDeques.DIRECT_ACCESS)
                        .setFIXED_POSSIBLE_CP_BUDGET(TEST_FIXED_POSSIBLE_CP_BUDGET)
                        .setTRANSF_GENERATED_CLASSES_PATH(TEST_BUILD_PATH)
                        .setTRANSF_LOAD_WITH_SYSTEM_CLASSLOADER(true)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET),
                MulibConfig.builder()
                        .setGLOBAL_SEARCH_STRATEGY(SearchStrategy.IDDFS)
                        .setINCR_ACTUAL_CP_BUDGET(3)
                        .setFIXED_POSSIBLE_CP_BUDGET(TEST_FIXED_POSSIBLE_CP_BUDGET)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET),
                MulibConfig.builder()
                        .setINCR_ACTUAL_CP_BUDGET(2)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET)
                        .setGLOBAL_SEARCH_STRATEGY(SearchStrategy.IDDFS)
                        .setTRANSF_GENERATED_CLASSES_PATH(TEST_BUILD_PATH)
                        .setTRANSF_LOAD_WITH_SYSTEM_CLASSLOADER(true)
                        .setFIXED_POSSIBLE_CP_BUDGET(TEST_FIXED_POSSIBLE_CP_BUDGET),
                MulibConfig.builder()
                        .setINCR_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET)
                        .setGLOBAL_SEARCH_STRATEGY(SearchStrategy.IDDFS)
                        .setCHOICE_OPTION_DEQUE_TYPE(ChoiceOptionDeques.DIRECT_ACCESS)
                        .setFIXED_POSSIBLE_CP_BUDGET(TEST_FIXED_POSSIBLE_CP_BUDGET),
                MulibConfig.builder()
                        .setINCR_ACTUAL_CP_BUDGET(1)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET)
                        .setGLOBAL_SEARCH_STRATEGY(SearchStrategy.IDDFS)
                        .setTRANSF_GENERATED_CLASSES_PATH(TEST_BUILD_PATH)
                        .setTRANSF_LOAD_WITH_SYSTEM_CLASSLOADER(true)
                        .setFIXED_POSSIBLE_CP_BUDGET(TEST_FIXED_POSSIBLE_CP_BUDGET),
                MulibConfig.builder()
                        .setINCR_ACTUAL_CP_BUDGET(16)
                        .setGLOBAL_SEARCH_STRATEGY(SearchStrategy.IDDFS)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET)
                        .setGLOBAL_SOLVER_TYPE(Solvers.JSMT_SMTINTERPOL)
                        .setENLIST_LEAVES(true)
                        .setFIXED_POSSIBLE_CP_BUDGET(TEST_FIXED_POSSIBLE_CP_BUDGET),
                MulibConfig.builder()
                        .setGLOBAL_SEARCH_STRATEGY(DFS)
                        .setCONCOLIC(true)
                        .setFIXED_POSSIBLE_CP_BUDGET(TEST_FIXED_POSSIBLE_CP_BUDGET)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET)
                        .assumeMulibDefaultValueRanges()
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

    public static List<List<PathSolution>> getAllSolutions(
            Function<MulibConfig.MulibConfigBuilder, List<PathSolution>> mbToList,
            String testedMethodName) {
        return getAllSolutions(b -> b, mbToList, testedMethodName);
    }

    public static List<List<PathSolution>> getAllSolutions(
            Function<MulibConfig.MulibConfigBuilder, MulibConfig.MulibConfigBuilder> adjustment,
            Function<MulibConfig.MulibConfigBuilder, List<PathSolution>> mulibConfigToList,
            String testedMethodName) {
        final List<MulibConfig.MulibConfigBuilder> executeTestsWith = initBuilders();
        return
            executeTestsWith.parallelStream().map(mcb -> {
                MulibConfig.MulibConfigBuilder mb = adjustment.apply(mcb);
                Mulib.log.log(java.util.logging.Level.INFO, "Started '" + testedMethodName + "' with config " + mb.build());
                List<PathSolution> pathSolutions = mulibConfigToList.apply(mb);
                Mulib.log.log(java.util.logging.Level.INFO, "Returns for '" + testedMethodName + "' with config " + mb.build());
                return pathSolutions;
            }).collect(Collectors.toList());
    }

    public static List<Optional<PathSolution>> getSolution(
            Function<MulibConfig.MulibConfigBuilder, Optional<PathSolution>> mulibConfigToPossibleSolution) {
        return getSolution(b -> b, mulibConfigToPossibleSolution);
    }

    public static List<Optional<PathSolution>> getSolution(
            Function<MulibConfig.MulibConfigBuilder, MulibConfig.MulibConfigBuilder> adjustment,
            Function<MulibConfig.MulibConfigBuilder, Optional<PathSolution>> mulibConfigToPossibleSolution) {
        final List<MulibConfig.MulibConfigBuilder> executeTestsWith = initBuilders();
        List<Optional<PathSolution>> result = new ArrayList<>();
        for (MulibConfig.MulibConfigBuilder mulibConfigBuilder : executeTestsWith) {
            MulibConfig.MulibConfigBuilder config = adjustment.apply(mulibConfigBuilder);
            result.add(mulibConfigToPossibleSolution.apply(config));
        }
        return result;
    }

    public static List<PathSolution> executeMulib(
            String methodName,
            Class<?> containingClass,
            MulibConfig.MulibConfigBuilder mb,
            boolean transformationRequired) {
        return executeMulib(methodName, containingClass, 2, mb, transformationRequired);
    }

    public static List<PathSolution> executeMulib(
            String methodName,
            Class<?> containingClass,
            int maxNumberOfSolutionsForEachPath,
            MulibConfig.MulibConfigBuilder mb,
            boolean transformationRequired) {
        MulibContext mc;
        if (transformationRequired) {
            mc = Mulib.getMulibContext(methodName, containingClass, mb, new Class<?>[0], new Object[0]);
        } else {
            mc = Mulib.getMulibContextWithoutTransformation(methodName, containingClass, mb, new Class<?>[0]);
        }
        List<PathSolution> result = mc.getAllPathSolutions();

        if (maxNumberOfSolutionsForEachPath > 1) {
            for (PathSolution ps : result) {
                mc.getUpToNSolutions(ps, maxNumberOfSolutionsForEachPath);
            }
        }
        return result;
    }

    public static Optional<PathSolution> executeMulibForOne(
            String methodName,
            Class<?> containingClass,
            int maxNumberOfSolutionsForEachPath,
            MulibConfig.MulibConfigBuilder mb,
            boolean transformationRequired) {
        MulibContext mc;
        if (transformationRequired) {
            mc = Mulib.getMulibContext(methodName, containingClass, mb, new Class<?> [0], new Object[0]);
        } else {
            mc = Mulib.getMulibContextWithoutTransformation(methodName, containingClass, mb, new Class<?>[0]);
        }
        Optional<PathSolution> result = mc.getPathSolution();

        if (result.isPresent()) {
            mc.getUpToNSolutions(result.get(), maxNumberOfSolutionsForEachPath);
        }
        return result;
    }

}
