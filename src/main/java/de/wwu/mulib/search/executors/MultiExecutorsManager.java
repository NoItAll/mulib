package de.wwu.mulib.search.executors;

import de.wwu.mulib.Mulib;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.search.choice_points.ChoicePointFactory;
import de.wwu.mulib.search.choice_points.CoverageCfg;
import de.wwu.mulib.search.trees.Choice;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.search.trees.SearchTree;
import de.wwu.mulib.substitutions.ValueFactory;
import de.wwu.mulib.transformations.MulibValueTransformer;

import java.lang.invoke.MethodHandle;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Mulib executor manager that manages multiple {@link MulibExecutor}s.
 */
public class MultiExecutorsManager extends MulibExecutorManager {
    private final SimpleSyncedQueue<SearchStrategy> nextStrategiesToInitialize;
    private final SimpleSyncedQueue<MulibExecutor> idle;
    private final ExecutorService executorService;
    private final long activateParallelFor;
    private volatile Throwable failureInThread = null;

    /**
     * @param config The configuration
     * @param observedTree The tree representing the evaluation of the search region
     * @param choicePointFactory The choice point factory; - must be compatible with the value and calculation factory
     * @param valueFactory The value factory; - must be compatible with the choice point and calculation factory
     * @param calculationFactory The calculation factory; - must be compatible with the value and choice point factory
     * @param mulibValueTransformer The mulib value transformed used to transform the initial arguments into search region types
     * @param searchRegionMethod The method handle used to invoke the search region
     * @param staticVariables A prototype of the manager of static variables
     * @param searchRegionArgs The transformed search region arguments
     * @param coverageCfg Can be null: The coverage control flow graph.
     */
    public MultiExecutorsManager(
            MulibConfig config,
            SearchTree observedTree,
            ChoicePointFactory choicePointFactory,
            ValueFactory valueFactory,
            CalculationFactory calculationFactory,
            MulibValueTransformer mulibValueTransformer,
            MethodHandle searchRegionMethod,
            StaticVariables staticVariables,
            Object[] searchRegionArgs,
            CoverageCfg coverageCfg) {
        super(config, Collections.synchronizedList(new ArrayList<>()), observedTree,
                choicePointFactory, valueFactory, calculationFactory, mulibValueTransformer,
                searchRegionMethod, staticVariables, searchRegionArgs, coverageCfg);
        this.nextStrategiesToInitialize = new SimpleSyncedQueue<>(config.SEARCH_ADDITIONAL_PARALLEL_STRATEGIES);
        this.executorService = Executors.newCachedThreadPool(new ExceptionThrowingThreadFactory(this));
        this.idle = new SimpleSyncedQueue<>();
        this.activateParallelFor = config.SEARCH_ACTIVATE_PARALLEL_FOR.isPresent() ? config.SEARCH_ACTIVATE_PARALLEL_FOR.get() : 1;
    }

    private static class SimpleSyncedQueue<T> {
        private final LinkedList<T> queue;
        private SimpleSyncedQueue() {
            queue = new LinkedList<>();
        }
        private SimpleSyncedQueue(Collection<T> elements) {
            queue = new LinkedList<>(elements);
        }
        private synchronized T poll() {
            return queue.isEmpty() ? null : queue.removeFirst();
        }
        private synchronized void add(T element) {
            queue.addLast(element);
        }
        private synchronized int size() {
            return queue.size();
        }
        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        private synchronized boolean isEmpty() {
            return queue.isEmpty();
        }
    }

    @Override
    public void notifyNewChoice(int depth, List<Choice.ChoiceOption> choiceOptions) {
        super.notifyNewChoice(depth, choiceOptions);
        // Additional functionality compared to SingleExecutorManager: Start new executor if there are choices
        while (!globalBudgetExceeded() && ((!nextStrategiesToInitialize.isEmpty()
                || !idle.isEmpty()) && observedTree.getChoiceOptionDeque().size() >= activateParallelFor)) {
            // Case 1: An existing MulibExecutor is idle, use this
            MulibExecutor nextExecutor = idle.poll();
            if (nextExecutor != null) {
                executorService.execute(() -> {
                    computePathSolutionsWithNonMainExecutor(nextExecutor);
                    idle.add(nextExecutor);
                });
            } else {
                // Case 2: No idles, start new search
                SearchStrategy searchStrategy = nextStrategiesToInitialize.poll();
                if (searchStrategy == null) {
                    return;
                }
                if (!observedTree.getChoiceOptionDeque().isEmpty()) {
                    executorService.execute(() -> {
                        MulibExecutor finalNextExecutor = new GenericExecutor(
                                observedTree.root.getOption(0),
                                this,
                                mulibValueTransformer,
                                config,
                                searchStrategy,
                                searchRegionMethod,
                                staticVariables,
                                searchRegionArgs
                        );
                        finalNextExecutor.addConstraintAfterBacktrackingPoint(
                                observedTree.root.getOption(0).getOptionConstraint());
                        finalNextExecutor.addExistingPartnerClassObjectConstraints(
                                observedTree.root.getOption(0).getPartnerClassObjectConstraints());
                        mulibExecutors.add(finalNextExecutor);
                        computePathSolutionsWithNonMainExecutor(finalNextExecutor);
                        idle.add(finalNextExecutor);
                    });

                } else {
                    return;
                }
            }
        }
    }

    /**
     * If {@link ExceptionThrowingThreadFactory} finds a {@link Throwable} that escaped a {@link MulibExecutor}, this
     * will call this method.
     * @param failureInThread Throwable that made a thread fail
     */
    public void signalFailure(Throwable failureInThread) {
        this.failureInThread = failureInThread;
    }

    @Override
    protected void checkForFailure() {
        if (failureInThread != null) {
            throw new MulibRuntimeException("Throwable thrown in one of the threads: "
                    + Arrays.toString(failureInThread.getStackTrace()) + config,
                    failureInThread
            );
        }
    }

    @Override
    protected boolean checkForPause() {
        checkForFailure();
        return globalBudgetExceeded() || observedTree.getChoiceOptionDeque().isEmpty();
    }

    @Override
    protected boolean checkForTerminationAndTerminate() {
        checkForFailure();
        if (checkForPause() && idle.size() == mulibExecutors.size() - 1) {
            executorService.shutdown();
            try {
                boolean terminated = executorService.awaitTermination(config.SHUTDOWN_PARALLEL_TIMEOUT_ON_SHUTDOWN_IN_MS, TimeUnit.MILLISECONDS);
                if (!terminated) {
                    throw new MulibRuntimeException("Executor service did not terminate in time");
                }
                for (MulibExecutor executor : mulibExecutors) {
                    executor.terminate();
                }
            } catch (Exception e) {
                throw new MulibRuntimeException(e);
            }
            return true;
        } else {
            return false;
        }
    }

    private void computePathSolutionsWithNonMainExecutor(MulibExecutor mulibExecutor) {
        while (!checkForPause()) {
            Optional<PathSolution> ps = mulibExecutor.getPathSolution();
            if ((config.LOG_TIME_FOR_EACH_PATH_SOLUTION || (config.LOG_TIME_FOR_FIRST_PATH_SOLUTION && !seenFirstPathSolution))
                    && ps.isPresent()) {
                long end = System.nanoTime();
                Mulib.log.info("Took " + ((end - startTime) / 1e6) + "ms for " + config + " to get a path solution");
                seenFirstPathSolution = true;
            }
        }
    }
}
