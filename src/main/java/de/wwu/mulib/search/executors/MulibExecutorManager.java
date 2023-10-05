package de.wwu.mulib.search.executors;

import de.wwu.mulib.Mulib;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.search.budget.GlobalExecutionBudgetManager;
import de.wwu.mulib.search.choice_points.ChoicePointFactory;
import de.wwu.mulib.search.choice_points.CoverageCfg;
import de.wwu.mulib.search.trees.*;
import de.wwu.mulib.solving.Solution;
import de.wwu.mulib.substitutions.ValueFactory;
import de.wwu.mulib.throwables.MulibIllegalStateException;
import de.wwu.mulib.transformations.MulibValueTransformer;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The supertype of all mulib executor manager. A mulib executor manager is used by {@link de.wwu.mulib.MulibContext} to
 * evaluate a search region. For this, it maintains one-to-many {@link MulibExecutor}s that implement search strategies.
 * Here, the global budget and other global synchronization means, such as the {@link GlobalIddfsSynchronizer} is stored,
 * in case that we use a {@link MulibExecutor} with {@link SearchStrategy#IDDSAS}.
 */
public abstract class MulibExecutorManager {

    /**
     * The executor running on the main thread with which this {@link MulibExecutorManager} has been called from
     */
    protected final MulibExecutor mainExecutor;
    /**
     * The configuration
     */
    protected final MulibConfig config;
    /**
     * The tree that is held by this {@link MulibExecutorManager} and that represents the evaluation of the search
     * region
     */
    protected final SearchTree observedTree;
    /**
     * The overall list of executors this manager maintains
     */
    protected final List<MulibExecutor> mulibExecutors;
    /**
     * The choice point factory
     */
    protected final ChoicePointFactory choicePointFactory;
    /**
     * The value factory
     */
    protected final ValueFactory valueFactory;
    /**
     * The calculation factory
     */
    protected final CalculationFactory calculationFactory;
    /**
     * The budget manager for budgets not reserved to a single execution run
     */
    protected final GlobalExecutionBudgetManager globalExecutionManagerBudgetManager;
    /**
     * The value transformer used for transforming passed arguments into the search region-representation
     */
    protected final MulibValueTransformer mulibValueTransformer;

    /**
     * If this manager is called to get {@link Solution}s rather than {@link PathSolution}s, the solutions
     * are accumulated here
     */
    protected final List<Solution> solutions;
    /**
     * The method handle used to invoke the search region with
     */
    protected final MethodHandle searchRegionMethod;
    /**
     * The static variables prototype. Such an instance will be passed to each {@link MulibExecutor}
     */
    protected final StaticVariables staticVariables;
    /**
     * The transformed search region arguments that will be copied by the {@link MulibExecutor}
     */
    protected final Object[] searchRegionArgs;
    /**
     * Whether or not we have found a first path solution
     */
    protected volatile boolean seenFirstPathSolution = false;
    /**
     * The time at which this manager has been called first
     */
    protected final long startTime;
    /**
     * Can be null. If is not null, {@link MulibConfig#TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID} was set
     */
    private final CoverageCfg coverageCfg;

    /**
     * Is null if no global incremental depth first search is used
     */
    protected final GlobalIddfsSynchronizer globalIddfsSynchronizer;

    /**
     * Can be null.
     * If not null: {@link MulibExecutor#getUpToNSolutions(PathSolution, AtomicInteger, boolean)} will be called
     */
    private AtomicInteger numberRequestedSolutions;
    private int numberAlreadyRequestedSolutions;
    private boolean terminateIfNHasBeenFound;

    /**
     * Constructs a new instance
     * @param config The config
     * @param mulibExecutorsList The list of additional executors managed by this manager, i.e.,
     *                           executors not defined via {@link MulibConfig#SEARCH_MAIN_STRATEGY}
     * @param observedTree The search tree representing the evaluation of the search region
     * @param choicePointFactory The choice point factory; - must be compatible with the value and calculation factory
     * @param valueFactory The value factory; - must be compatible with the choice point and calculation factory
     * @param calculationFactory The calculation factory; - must be compatible with the value and choice point factory
     * @param mulibValueTransformer The mulib value transformed used to transform the initial arguments into search region types
     * @param searchRegionMethod The method handle used to invoke the search region
     * @param staticVariables A prototype of the manager of static variables
     * @param searchRegionArgs The transformed search region arguments
     * @param coverageCfg Can be null: The coverage control flow graph.
     */
    protected MulibExecutorManager(
            MulibConfig config,
            List<MulibExecutor> mulibExecutorsList,
            SearchTree observedTree,
            ChoicePointFactory choicePointFactory,
            ValueFactory valueFactory,
            CalculationFactory calculationFactory,
            MulibValueTransformer mulibValueTransformer,
            MethodHandle searchRegionMethod,
            StaticVariables staticVariables,
            Object[] searchRegionArgs,
            CoverageCfg coverageCfg) {
        this.config = config;
        this.observedTree = observedTree;
        this.choicePointFactory = choicePointFactory;
        this.valueFactory = valueFactory;
        this.calculationFactory = calculationFactory;
        this.mulibExecutors = Collections.synchronizedList(mulibExecutorsList);
        this.mulibValueTransformer = mulibValueTransformer;
        this.searchRegionMethod = searchRegionMethod;
        this.staticVariables = staticVariables;
        this.searchRegionArgs = searchRegionArgs;
        this.mulibExecutors.add(new GenericExecutor(
                observedTree.root.getOption(0),
                this,
                this.mulibValueTransformer,
                config,
                config.SEARCH_MAIN_STRATEGY,
                searchRegionMethod,
                staticVariables,
                searchRegionArgs
        ));
        this.globalExecutionManagerBudgetManager = new GlobalExecutionBudgetManager(config);
        this.mainExecutor = this.mulibExecutors.get(0);
        this.solutions =
                config.SEARCH_ADDITIONAL_PARALLEL_STRATEGIES.isEmpty()
                        ?
                        new ArrayList<>()
                        :
                        Collections.synchronizedList(new ArrayList<>());
        this.numberRequestedSolutions = null;
        this.startTime = config.LOG_TIME_FOR_EACH_PATH_SOLUTION || config.LOG_TIME_FOR_FIRST_PATH_SOLUTION ? System.nanoTime() : 0L;
        this.globalIddfsSynchronizer =
                (config.SEARCH_MAIN_STRATEGY == SearchStrategy.IDDSAS
                || config.SEARCH_ADDITIONAL_PARALLEL_STRATEGIES.contains(SearchStrategy.IDDSAS))
                ?
                new GlobalIddfsSynchronizer(config.BUDGETS_INCR_ACTUAL_CP.get().intValue())
                :
                null;
        this.coverageCfg = coverageCfg;
    }

    /**
     * Resets the time budget and evaluates the search region to find a path solution.
     * Terminates this manager after execution
     * @return A path solution, if any can be found
     */
    public synchronized Optional<PathSolution> getPathSolution() {
        globalExecutionManagerBudgetManager.resetTimeBudget();
        Optional<PathSolution> result = _getPathSolution();
        printStatistics();
        return result;
    }

    // Shuts down this manager, if unable to find a path solution.
    private Optional<PathSolution> _getPathSolution() {
        int currentNumberPathSolutions = observedTree.getPathSolutionsList().size();
        while (!checkForPauseAndTerminateIfNeeded()) {
            Optional<PathSolution> possiblePathSolution = mainExecutor.getPathSolution();
            checkForFailure();
            if (possiblePathSolution.isPresent()) {
                return possiblePathSolution;
            }
        }
        if (observedTree.getPathSolutionsList().size() > currentNumberPathSolutions) {
            // Potentially, the result has been computed by another thread. In this case, we simply return
            // the last path solution added to the observed tree. It is mandatory that, in fact, a new path-solution
            // was added
            return Optional.of(observedTree.getPathSolutionsList().get(observedTree.getPathSolutionsList().size() - 1));
        }
        return Optional.empty();
    }

    /**
     * Evaluates the search region and returning as many path solutions as can be found given the budget.
     * If the search region is finite, returns all path solutions, if the budget allows for that.
     * Shuts down this manager thereafter.
     * @return As many path solutions as can be found
     */
    public synchronized List<PathSolution> getPathSolutions() {
        globalExecutionManagerBudgetManager.resetTimeBudget();
        // We constantly poll with the mainExecutor.
        while (!checkForPauseAndTerminateIfNeeded()) {
            checkForFailure();
            Optional<PathSolution> ps = mainExecutor.getPathSolution();
            if ((config.LOG_TIME_FOR_EACH_PATH_SOLUTION || (config.LOG_TIME_FOR_FIRST_PATH_SOLUTION && !seenFirstPathSolution))
                    && ps.isPresent()) {
                long end = System.nanoTime();
                Mulib.log.fine("Took " + ((end - startTime) / 1e6) + "ms for " + config + " to get a path solution");
                seenFirstPathSolution = true;
            }
        }
        printStatistics();
        return observedTree.getPathSolutionsList();
    }

    /**
     * Tries to find up to N solutions in the search region. Potentially, multiple solutions reside on the
     * same {@link PathSolution}.
     * Terminates this manager thereafter.
     * @param N The desired number of solutions to retrieve
     * @return The found solutions
     */
    public synchronized List<Solution> getUpToNSolutions(int N) {
        return getUpToNSolutions(N, true);
    }

    /**
     * Tries to find up to N solutions in the search region. Potentially, multiple solutions reside on the
     * same {@link PathSolution}.
     * @param N The desired number of solutions to retrieve
     * @param terminateIfNHasBeenFound If true, terminates the manager after finding N solutions.
     *                                 The manager also terminates if no further choice options can be found.
     * @return The additional found solutions
     */
    public synchronized List<Solution> getUpToNSolutions(int N, boolean terminateIfNHasBeenFound) {
        if (numberRequestedSolutions != null) {
            throw new MulibIllegalStateException("The previous request for solutions has not been completed");
        }
        numberRequestedSolutions = new AtomicInteger(N);
        int currentNumberSolutions = numberAlreadyRequestedSolutions;
        this.terminateIfNHasBeenFound = terminateIfNHasBeenFound;
        if (!terminateIfNHasBeenFound) {
            for (MulibExecutor me : new ArrayList<>(mulibExecutors)) { // TODO Avoid copy
                this.solutions.addAll(me.reenableForMoreSolutions(numberRequestedSolutions));
            }
        }

        while (!checkForPauseAndTerminateIfNeeded()) {
            _getPathSolution();
        }
        if (!terminateIfNHasBeenFound) {
            numberAlreadyRequestedSolutions += (N - numberRequestedSolutions.get());
            numberRequestedSolutions = null;
            for (MulibExecutor me : mulibExecutors) {
                me.pause();
            }
        } else {
            numberAlreadyRequestedSolutions = N;
        }
        printStatistics();
        return solutions.subList(currentNumberSolutions, Math.min(numberAlreadyRequestedSolutions, solutions.size()));
    }

    /**
     * Tries to add a fail node to the search tree's explicit list.
     * Also increments the fail budget.
     * @param fail The fail node
     */
    public final void addToFails(Fail fail) {
        this.observedTree.addToFails(fail);
        globalExecutionManagerBudgetManager.incrementFailBudget();
    }

    /**
     * Tries to add a path solution to the search tree's explicit list.
     * Increments the path solution budget.
     * If this is called due to an initial {@link MulibExecutorManager#getUpToNSolutions(int)} call,
     * the executor that found this path solution is asked to try to retrieve more solutions from it
     * @param pathSolution The found path solution
     * @param responsibleExecutor The executor that found the path solution
     */
    public void addToPathSolutions(PathSolution pathSolution, MulibExecutor responsibleExecutor) {
        this.observedTree.addToPathSolutions(pathSolution);
        this.globalExecutionManagerBudgetManager.incrementPathSolutionBudget();
        if (numberRequestedSolutions != null) {
            this.numberRequestedSolutions.decrementAndGet();
            solutions.add(pathSolution.getSolution());
            solutions.addAll(responsibleExecutor.getUpToNSolutions(pathSolution, numberRequestedSolutions, terminateIfNHasBeenFound));
        }
    }

    /**
     * Tries to add a exceeded budget node to the search tree's explicit list.
     * Also increments the exceeded budget budget.
     * @param exceededBudget The exceeded budget node
     */
    public final void addToExceededBudgets(ExceededBudget exceededBudget) {
        this.observedTree.addToExceededBudgets(exceededBudget);
        this.globalExecutionManagerBudgetManager.incrementExceededBudgetBudget();
    }

    /**
     * Inserts the list of choice options into the {@link ChoiceOptionDeque} maintained by the {@link SearchTree}
     * @param depth The depth at which to insert the choice options
     * @param choiceOptions The list of choice options
     */
    public void notifyNewChoice(int depth, List<Choice.ChoiceOption> choiceOptions) {
        observedTree.getChoiceOptionDeque().insert(depth, choiceOptions);
    }

    /**
     * @return true, if the global budget, kept in a {@link GlobalExecutionBudgetManager} was exceeded, else false.
     */
    public final boolean globalBudgetExceeded() {
        return shouldStopSinceFullCoverageAchieved() || nonSolutionBudgetExceeded() || shouldStopSinceEnoughSolutionsWereFound();
    }

    protected final boolean shouldStopSinceFullCoverageAchieved() {
        return config.CFG_TERMINATE_EARLY_ON_FULL_COVERAGE && coverageCfg.fullCoverageAchieved();
    }

    protected final boolean nonSolutionBudgetExceeded() {
        return globalExecutionManagerBudgetManager.timeBudgetIsExceeded()
                || globalExecutionManagerBudgetManager.fixedFailBudgetIsExceeded()
                || globalExecutionManagerBudgetManager.fixedPathSolutionBudgetIsExceeded()
                || globalExecutionManagerBudgetManager.fixedExceededBudgetBudgetsIsExceeded();
    }

    /**
     * @return The coverage cfg. If this manager is not configured to hold a coverage cfg, throws an {@link MulibIllegalStateException}.
     */
    public final CoverageCfg getCoverageCfg() {
        if (!config.TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID) {
            throw new MulibIllegalStateException("Must not request CFG if it is not used according to the configuration");
        }
        return coverageCfg;
    }

    /**
     * Checks for the a failure that this manager has implicitly been notified about.
     * Might happen due to {@link ExceptionThrowingThreadFactory}.
     */
    protected abstract void checkForFailure();

    /**
     * Log statistics using {@link Mulib#log}.
     */
    protected void printStatistics() {
        StringBuilder b = new StringBuilder();
        String indent = "   ";
        String linebreak = "\r\n";
        b.append(linebreak);
        MulibExecutor last = mulibExecutors.get(mulibExecutors.size() - 1);
        for (MulibExecutor me : new ArrayList<>(mulibExecutors)) { // TODO Avoid copy
            b.append(indent)
                    .append(me.getSearchStrategy())
                    .append(": ")
                    .append(me.getStatistics().toString());
            if (me != last) {
                b.append(linebreak);
            }
        }
        b.append(linebreak)
                .append(indent)
                .append(", numberPathSolutions: ")
                .append(observedTree.getPathSolutionsList().size());
        if (config.TREE_ENLIST_LEAVES) {
            b.append(", numberFails: ")
                    .append(observedTree.getFailsList().size())
                    .append(", numberExceededBudget: ")
                    .append(observedTree.getExceededBudgetList().size());

        }
        Mulib.log.fine(b.toString());
    }

    /**
     * @return true if the global budget has been exceeded or the choice option deque is empty
     */
    protected boolean checkForPause() {
        return globalBudgetExceeded() || observedTree.getChoiceOptionDeque().isEmpty();
    }

    /**
     * Checks whether the MulibExecutorManager and connected MulibExecutors should be paused.
     * Terminates the MulibExecutorManager and connected MulibExecutors if needed.
     * @return true, if this MulibExecutorManager pauses, else false.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected boolean checkForPauseAndTerminateIfNeeded() {
        if (checkForPause()) {
            if (shouldStopSinceFullCoverageAchieved() || nonSolutionBudgetExceeded()) {
                terminate();
            }
            return true;
        }
        return false;
    }

    /**
     * Terminates the MulibExecutorManager. It cannot be used after calling this method.
     */
    public void terminate() {
        for (MulibExecutor me : new ArrayList<>(mulibExecutors)) { // TODO Avoid copy
            me.terminate();
        }
        observedTree.getChoiceOptionDeque().setEmpty();
    }

    // Returns false if numberRequestedSolutions == null, which is true if we did not ask for
    // Solutions
    private boolean shouldStopSinceEnoughSolutionsWereFound() {
        return numberRequestedSolutions != null && numberRequestedSolutions.get() <= 0;
    }
}
