package de.wwu.mulib.search.executors;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.search.choice_points.ChoicePointFactory;
import de.wwu.mulib.search.trees.Choice;
import de.wwu.mulib.search.trees.SearchTree;
import de.wwu.mulib.substitutions.primitives.ValueFactory;
import de.wwu.mulib.transformations.MulibValueTransformer;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MultiExecutorsManager extends MulibExecutorManager {
    private final SimpleSyncedQueue<SearchStrategy> nextStrategiesToInitialize;
    private final SimpleSyncedQueue<MulibExecutor> idle;
    private final ExecutorService executorService;
    private final long activateParallelFor;
    private volatile Throwable failureInThread = null;

    public MultiExecutorsManager(
            MulibConfig config,
            SearchTree observedTree,
            ChoicePointFactory choicePointFactory,
            ValueFactory valueFactory,
            CalculationFactory calculationFactory,
            MulibValueTransformer mulibValueTransformer) {
        super(config, Collections.synchronizedList(new ArrayList<>()), observedTree,
                choicePointFactory, valueFactory, calculationFactory, mulibValueTransformer);
        this.nextStrategiesToInitialize = new SimpleSyncedQueue<>(config.ADDITIONAL_PARALLEL_SEARCH_STRATEGIES);
        this.executorService = Executors.newCachedThreadPool(new ExceptionThrowingThreadFactory(this));
        this.idle = new SimpleSyncedQueue<>();
        this.activateParallelFor = config.ACTIVATE_PARALLEL_FOR.isPresent() ? config.ACTIVATE_PARALLEL_FOR.get() : 1;
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
        while ((!nextStrategiesToInitialize.isEmpty() || !idle.isEmpty()) && observedTree.getChoiceOptionDeque().size() >= activateParallelFor) {
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
                                searchStrategy
                        );
                        finalNextExecutor.addNewConstraintAfterBacktrackingPoint(
                                observedTree.root.getOption(0).getOptionConstraint());
                        finalNextExecutor.addExistingArrayConstraints(observedTree.root.getOption(0).getArrayConstraints());
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

    public void signalFailure(Throwable failureInThread) {
        assert failureInThread != null;
        this.failureInThread = failureInThread;
    }

    @Override
    protected void checkForFailure() {
        if (failureInThread != null) {
            throw new MulibRuntimeException("Failure caught in one of the threads: "
                    + Arrays.toString(failureInThread.getStackTrace()) + config,
                    failureInThread
            );
        }
    }

    @Override
    protected boolean checkForPause() {
        checkForFailure();
        if (globalBudgetExceeded() || observedTree.getChoiceOptionDeque().isEmpty()) {
            return true;
        }
        return false;
    }

    @Override
    protected boolean checkForShutdown() {
        checkForFailure();
        if (checkForPause() && idle.size() == mulibExecutors.size() - 1) {
            executorService.shutdown();
            try {
                boolean terminated = executorService.awaitTermination(config.PARALLEL_TIMEOUT_IN_MS, TimeUnit.MILLISECONDS);
                if (!terminated) {
                    throw new MulibRuntimeException("Executor service did not terminate in time");
                }
            } catch (Exception e) {
                throw new MulibRuntimeException(e);
            }
            return true;
        } else {
            return false;
        }
    }
}
