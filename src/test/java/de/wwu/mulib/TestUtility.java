package de.wwu.mulib;

import de.wwu.mulib.search.executors.SearchStrategy;
import de.wwu.mulib.search.trees.ChoiceOptionDeques;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.solving.Solvers;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.wwu.mulib.search.executors.SearchStrategy.*;
import static org.junit.jupiter.api.Assertions.fail;

public final class TestUtility {

    private static final boolean longSingleThreadedExecutorsCheck = false;
    private static final boolean longParallelExecutorsCheck = false;
    private static final boolean quickCheck = true;
    private static final boolean quickParallelExecutorsCheck = false;
    private TestUtility() {}

    public static final long TEST_FIXED_POSSIBLE_CP_BUDGET = 1000;
    public static final long TEST_FIXED_ACTUAL_CP_BUDGET = 500;
    
    private static List<MulibConfig.MulibConfigBuilder> quickCheck() {
        return List.of(
                MulibConfig.builder()
                        .setGLOBAL_SOLVER_TYPE(Solvers.Z3)
                        .setGLOBAL_SEARCH_STRATEGY(DSAS)
                        .setADDITIONAL_PARALLEL_SEARCH_STRATEGIES(DSAS, DSAS, DSAS)
//                        .setINCR_ACTUAL_CP_BUDGET(32)
                        .setCHOICE_OPTION_DEQUE_TYPE(ChoiceOptionDeques.DIRECT_ACCESS)
                        .setFIXED_POSSIBLE_CP_BUDGET(TEST_FIXED_POSSIBLE_CP_BUDGET)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET)
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
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET),
                MulibConfig.builder()
                        .setGLOBAL_SOLVER_TYPE(Solvers.JSMT_SMTINTERPOL)
                        .setGLOBAL_SEARCH_STRATEGY(DSAS)
                        .setADDITIONAL_PARALLEL_SEARCH_STRATEGIES(DSAS, DSAS, DSAS)
                        .setFIXED_POSSIBLE_CP_BUDGET(TEST_FIXED_POSSIBLE_CP_BUDGET)
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
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET),
                MulibConfig.builder()
                        .setGLOBAL_SEARCH_STRATEGY(BFS)
                        .setADDITIONAL_PARALLEL_SEARCH_STRATEGIES(BFS, BFS)
                        .setFIXED_POSSIBLE_CP_BUDGET(TEST_FIXED_POSSIBLE_CP_BUDGET)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET),
                MulibConfig.builder()
                        .setGLOBAL_SEARCH_STRATEGY(IDDFS)
                        .setADDITIONAL_PARALLEL_SEARCH_STRATEGIES(IDDFS, IDDFS)
                        .setINCR_ACTUAL_CP_BUDGET(3)
                        .setFIXED_POSSIBLE_CP_BUDGET(TEST_FIXED_POSSIBLE_CP_BUDGET)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET),
                MulibConfig.builder()
                        .setGLOBAL_SOLVER_TYPE(Solvers.JSMT_SMTINTERPOL)
                        .setADDITIONAL_PARALLEL_SEARCH_STRATEGIES(DFS, BFS)
                        .setFIXED_POSSIBLE_CP_BUDGET(TEST_FIXED_POSSIBLE_CP_BUDGET)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET),
                MulibConfig.builder()
                        .setINCR_ACTUAL_CP_BUDGET(2)
                        .setGLOBAL_SEARCH_STRATEGY(SearchStrategy.IDDFS)
                        .setADDITIONAL_PARALLEL_SEARCH_STRATEGIES(DFS, BFS)
                        .setGLOBAL_SOLVER_TYPE(Solvers.JSMT_SMTINTERPOL)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET)
                        .setFIXED_POSSIBLE_CP_BUDGET(TEST_FIXED_POSSIBLE_CP_BUDGET),
                MulibConfig.builder()
                        .setGLOBAL_SOLVER_TYPE(Solvers.JSMT_SMTINTERPOL)
                        .setADDITIONAL_PARALLEL_SEARCH_STRATEGIES(DFS, BFS)
                        .setGLOBAL_SEARCH_STRATEGY(BFS)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET)
                        .setFIXED_POSSIBLE_CP_BUDGET(TEST_FIXED_POSSIBLE_CP_BUDGET),
                MulibConfig.builder()
                        .setGLOBAL_SOLVER_TYPE(Solvers.JSMT_SMTINTERPOL)
                        .setADDITIONAL_PARALLEL_SEARCH_STRATEGIES(DFS, BFS)
                        .setGLOBAL_SEARCH_STRATEGY(DSAS)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET)
                        .setFIXED_POSSIBLE_CP_BUDGET(TEST_FIXED_POSSIBLE_CP_BUDGET),
                MulibConfig.builder()
                        .setGLOBAL_SOLVER_TYPE(Solvers.Z3)
                        .setADDITIONAL_PARALLEL_SEARCH_STRATEGIES(DFS, BFS, DSAS, IDDFS)
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
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET),
                MulibConfig.builder()
                        .setGLOBAL_SEARCH_STRATEGY(SearchStrategy.IDDFS)
                        .setINCR_ACTUAL_CP_BUDGET(3)
                        .setFIXED_POSSIBLE_CP_BUDGET(TEST_FIXED_POSSIBLE_CP_BUDGET)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET),
                MulibConfig.builder()
                        .setGLOBAL_SOLVER_TYPE(Solvers.JSMT_SMTINTERPOL)
                        .setFIXED_POSSIBLE_CP_BUDGET(TEST_FIXED_POSSIBLE_CP_BUDGET)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET),
                MulibConfig.builder()
                        .setINCR_ACTUAL_CP_BUDGET(2)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET)
                        .setGLOBAL_SEARCH_STRATEGY(SearchStrategy.IDDFS)
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
                        .setFIXED_POSSIBLE_CP_BUDGET(TEST_FIXED_POSSIBLE_CP_BUDGET),
                MulibConfig.builder()
                        .setINCR_ACTUAL_CP_BUDGET(2)
                        .setGLOBAL_SEARCH_STRATEGY(SearchStrategy.IDDFS)
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET)
                        .setGLOBAL_SOLVER_TYPE(Solvers.JSMT_SMTINTERPOL)
                        .setFIXED_POSSIBLE_CP_BUDGET(TEST_FIXED_POSSIBLE_CP_BUDGET),
                MulibConfig.builder()
                        .setFIXED_ACTUAL_CP_BUDGET(TEST_FIXED_ACTUAL_CP_BUDGET)
                        .setGLOBAL_SOLVER_TYPE(Solvers.JSMT_SMTINTERPOL)
                        .setGLOBAL_SEARCH_STRATEGY(BFS)
                        .setFIXED_POSSIBLE_CP_BUDGET(TEST_FIXED_POSSIBLE_CP_BUDGET)
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
            Function<MulibConfig, List<PathSolution>> mulibConfigToList,
            String testedMethodName) {
        return getAllSolutions(b -> b, mulibConfigToList, testedMethodName);
    }

    public static List<List<PathSolution>> getAllSolutions(
            Function<MulibConfig.MulibConfigBuilder, MulibConfig.MulibConfigBuilder> adjustment,
            Function<MulibConfig, List<PathSolution>> mulibConfigToList,
            String testedMethodName) {
        final List<MulibConfig.MulibConfigBuilder> executeTestsWith = initBuilders();
        return
            executeTestsWith.parallelStream().map(mcb -> {
                long startTime = System.nanoTime();
                MulibConfig config = adjustment.apply(mcb).build();
                Mulib.log.log(java.util.logging.Level.INFO, "Started '" + testedMethodName + "' with config " + config);
                List<PathSolution> pathSolutions = mulibConfigToList.apply(config);
                long endTime = System.nanoTime();
                Mulib.log.log(java.util.logging.Level.INFO, "Returns for '" + testedMethodName + "' with config " + config
                        + "\r\nRequired time: " + (endTime - startTime) * 1e-6 + "ms");
                return pathSolutions;
            }).collect(Collectors.toList());
    }

    public static List<Optional<PathSolution>> getSolution(
            Function<MulibConfig, Optional<PathSolution>> mulibConfigToPossibleSolution) {
        return getSolution(b -> b, mulibConfigToPossibleSolution);
    }

    public static List<Optional<PathSolution>> getSolution(
            Function<MulibConfig.MulibConfigBuilder, MulibConfig.MulibConfigBuilder> adjustment,
            Function<MulibConfig, Optional<PathSolution>> mulibConfigToPossibleSolution) {
        final List<MulibConfig.MulibConfigBuilder> executeTestsWith = initBuilders();
        List<Optional<PathSolution>> result = new ArrayList<>();
        for (MulibConfig.MulibConfigBuilder mulibConfigBuilder : executeTestsWith) {
            MulibConfig config = adjustment.apply(mulibConfigBuilder).build();
            long startTime = System.nanoTime();
            Mulib.log.log(java.util.logging.Level.INFO, "Started with config " + config);
            result.add(mulibConfigToPossibleSolution.apply(config));
            long endTime = System.nanoTime();
            Mulib.log.log(java.util.logging.Level.INFO, "Returns with config " + config
                    + "\r\nRequired time: " + (endTime - startTime) * 1e-6 + "ms\r\n\r\n");
        }
        return result;
    }

    public static List<PathSolution> executeMulib(
            String methodName,
            Class<?> containingClass,
            MulibConfig config) {
        return executeMulib(methodName, containingClass, 2, config);
    }

    public static List<PathSolution> executeMulib(
            String methodName,
            Class<?> containingClass,
            int maxNumberOfSolutionsForEachPath,
            MulibConfig config) {
        Method method = null;
        try {
            method = containingClass.getDeclaredMethod(methodName);
        } catch (NoSuchMethodException e) {
            fail(e);
        }
        try {
            MulibContext mc = Mulib.generateWithoutTransformation(
                    MethodHandles.lookup().unreflect(method),
                    config
            );
            List<PathSolution> result = mc.getAllPathSolutions();

            if (maxNumberOfSolutionsForEachPath > 1) {
                for (PathSolution ps : result) {
                    mc.getUpToNSolutions(ps, maxNumberOfSolutionsForEachPath);
                }
            }
            return result;
        } catch (IllegalAccessException e) {
            fail(e);
        }
        return Collections.emptyList();
    }

    public static Optional<PathSolution> executeMulibForOne(
            String methodName,
            Class<?> containingClass,
            int maxNumberOfSolutionsForEachPath,
            MulibConfig config) {
        Method method = null;
        try {
            method = containingClass.getDeclaredMethod(methodName);
        } catch (NoSuchMethodException e) {
            fail(e);
        }
        try {
            MulibContext mc = Mulib.generateWithoutTransformation(
                    MethodHandles.lookup().unreflect(method),
                    config
            );
            Optional<PathSolution> result = mc.getPathSolution();

            if (!result.isEmpty()) {
                mc.getUpToNSolutions(result.get(), maxNumberOfSolutionsForEachPath);
            }
            return result;
        } catch (IllegalAccessException e) {
            fail(e);
        }
        return Optional.empty();
    }

}
