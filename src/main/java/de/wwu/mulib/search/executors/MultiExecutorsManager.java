package de.wwu.mulib.search.executors;

import de.wwu.mulib.Mulib;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.search.choice_points.ChoicePointFactory;
import de.wwu.mulib.search.trees.Choice;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.search.trees.SearchTree;
import de.wwu.mulib.substitutions.primitives.ValueFactory;
import de.wwu.mulib.transformations.MulibValueTransformer;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class MultiExecutorsManager extends MulibExecutorManager {
    private final SimpleSyncedQueue<SearchStrategy> nextStrategiesToInitialize;
    private final SimpleSyncedQueue<MulibExecutor> idle;
    private final List<PathSolution> pathSolutions = Collections.synchronizedList(new ArrayList<>());
    private final ExecutorService executorService;
    private final MulibExecutor mainExecutor;
    private volatile boolean terminated = false;
    private final long activateParallelFor;

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
        this.mainExecutor = this.mulibExecutors.get(0);
        this.executorService = Executors.newCachedThreadPool(new ExceptionThrowingThreadFactory(this));
        this.idle = new SimpleSyncedQueue<>();
        this.activateParallelFor = config.ACTIVATE_PARALLEL_FOR.isPresent() ? config.ACTIVATE_PARALLEL_FOR.get() : 1;
    }

    private void setTerminated() {
        terminated = true;
        for (MulibExecutor me : mulibExecutors) {
            me.setTerminated(true);
        }
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
        private synchronized boolean isEmpty() {
            return queue.isEmpty();
        }
    }


    private void checkForFailure() {
        if (failureInThread != null) {
            throw new MulibRuntimeException("Failure caught in one of the threads: "
                    + Arrays.toString(failureInThread.getStackTrace()),
                    failureInThread
            );
        }
    }

    private boolean checkShutdown() {
        if (terminated || globalBudgetExceeded() || (idle.size() == mulibExecutors.size() - 1 && observedTree.getChoiceOptionDeque().isEmpty())) {
            try {
                checkForFailure();
//                setTerminated();
                executorService.shutdown();
                boolean terminated = executorService.awaitTermination(config.PARALLEL_TIMEOUT_IN_MS, TimeUnit.MILLISECONDS);
                if (!terminated) {
                    throw new MulibRuntimeException("Executor service did not terminate in time");
                }
                return true;
            } catch (Exception e) {
                throw new MulibRuntimeException(e);
            }
        }
        return false;
    }

    @Override
    public List<PathSolution> getAllSolutions() {
        globalExecutionManagerBudgetManager.resetTimeBudget();

        // We constantly poll with the mainExecutor.
        while (!checkShutdown()) {
            checkForFailure();
            addToPathSolutions(mainExecutor);
        }

        printStatistics();
        return pathSolutions;
    }

    private void printStatistics() {
        StringBuilder b = new StringBuilder();
        MulibExecutor last = mulibExecutors.get(mulibExecutors.size() - 1);
        b.append("\r\n");
        b.append("\r\n");
        for (MulibExecutor me : mulibExecutors) {
            b.append("   ")
                    .append(me.searchStrategy)
                    .append(": ")
                    .append(me.getStatistics().toString());
            if (me != last) {
                b.append("\r\n");
            }
        }
        Mulib.log.log(Level.INFO, b.toString());
    }
    @Override
    public Optional<PathSolution> getSolution() {
        globalExecutionManagerBudgetManager.resetTimeBudget();
        while (!checkShutdown()) {
            if (pathSolutions.size() > 0) {
                checkShutdown();
                printStatistics();
                return Optional.of(pathSolutions.get(pathSolutions.size() - 1));
            }
            Optional<PathSolution> possibleSymbolicExecution = mainExecutor.runForSingleSolution();
            if (possibleSymbolicExecution.isPresent()) {
                this.observedTree.getChoiceOptionDeque().setEmpty();
                checkShutdown();
                printStatistics();
                return possibleSymbolicExecution;
            }
        }
        if (pathSolutions.size() > 0) {
            checkShutdown();
            return Optional.of(pathSolutions.get(pathSolutions.size() - 1));
        }
        return Optional.empty();
    }

    private void addToPathSolutions(MulibExecutor mulibExecutor) {
        List<PathSolution> solutionsOfGlobalSolver = getAllSolutions(mulibExecutor);
        pathSolutions.addAll(solutionsOfGlobalSolver);
    }

    @Override
    public void notifyNewChoice(int depth, List<Choice.ChoiceOption> choiceOptions) {
        super.notifyNewChoice(depth, choiceOptions);
        // Additional functionality compared to SingleExecutorManager: Start new executor if there are choices
        while (!terminated && (!nextStrategiesToInitialize.isEmpty() || !idle.isEmpty()) && observedTree.getChoiceOptionDeque().size() >= activateParallelFor) {
            // Case 1: An existing MulibExecutor is idle, use this
            MulibExecutor nextExecutor = idle.poll();
            if (nextExecutor != null) {
                executorService.execute(() -> {
                    addToPathSolutions(nextExecutor);
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
                                prototypicalMulibValueTransformer,
                                config,
                                searchStrategy
                        );
                        finalNextExecutor
                                .solverManager
                                .addConstraintAfterNewBacktrackingPoint(observedTree.root.getOption(0).getOptionConstraint());
                        mulibExecutors.add(finalNextExecutor);
                        addToPathSolutions(finalNextExecutor);
                        idle.add(finalNextExecutor);
                    });

                } else {
                    return;
                }
            }
        }
    }

    private volatile Throwable failureInThread = null;
    public void signalFailure(Throwable failureInThread) {
        assert failureInThread != null;
        assert this.failureInThread == null;
        this.failureInThread = failureInThread;
    }
}
