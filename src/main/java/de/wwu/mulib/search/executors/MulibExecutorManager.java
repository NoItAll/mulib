package de.wwu.mulib.search.executors;

import de.wwu.mulib.Mulib;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.exceptions.MulibIllegalStateException;
import de.wwu.mulib.search.budget.GlobalExecutionBudgetManager;
import de.wwu.mulib.search.choice_points.ChoicePointFactory;
import de.wwu.mulib.search.trees.*;
import de.wwu.mulib.substitutions.primitives.ValueFactory;
import de.wwu.mulib.transformations.MulibValueTransformer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public abstract class MulibExecutorManager {

    protected final MulibExecutor mainExecutor;
    protected final MulibConfig config;
    protected final SearchTree observedTree;
    protected final List<MulibExecutor> mulibExecutors;
    protected final ChoicePointFactory choicePointFactory;
    protected final ValueFactory valueFactory;
    protected final CalculationFactory calculationFactory;
    protected final GlobalExecutionBudgetManager globalExecutionManagerBudgetManager;
    protected final MulibValueTransformer mulibValueTransformer;
    private AtomicInteger numberRequestedSolutions;
    protected final List<Solution> solutions;

    protected MulibExecutorManager(
            MulibConfig config,
            List<MulibExecutor> mulibExecutorsList,
            SearchTree observedTree,
            ChoicePointFactory choicePointFactory,
            ValueFactory valueFactory,
            CalculationFactory calculationFactory,
            MulibValueTransformer mulibValueTransformer) {
        this.config = config;
        this.observedTree = observedTree;
        this.choicePointFactory = choicePointFactory;
        this.valueFactory = valueFactory;
        this.calculationFactory = calculationFactory;
        this.mulibExecutors = mulibExecutorsList;
        this.mulibValueTransformer = mulibValueTransformer;
        this.mulibExecutors.add(new GenericExecutor(
                observedTree.root.getOption(0),
                this,
                this.mulibValueTransformer,
                config,
                config.GLOBAL_SEARCH_STRATEGY
        ));
        this.globalExecutionManagerBudgetManager = new GlobalExecutionBudgetManager(config);
        this.mainExecutor = this.mulibExecutors.get(0);
        this.solutions =
                config.ADDITIONAL_PARALLEL_SEARCH_STRATEGIES.isEmpty()
                        ?
                        new ArrayList<>()
                        :
                        Collections.synchronizedList(new ArrayList<>());
        this.numberRequestedSolutions = null;
    }

    public synchronized Optional<PathSolution> getPathSolution() {
        globalExecutionManagerBudgetManager.resetTimeBudget();
        Optional<PathSolution> result = _getPathSolution();
        printStatistics();
        return result;
    }

    private Optional<PathSolution> _getPathSolution() {
        int currentNumberPathSolutions = observedTree.getPathSolutionsList().size();
        while (!checkForShutdown()) {
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

    public synchronized List<PathSolution> getAllPathSolutions() {
        globalExecutionManagerBudgetManager.resetTimeBudget();
        // We constantly poll with the mainExecutor.
        long start = 0L;
        while (!checkForShutdown()) {
            checkForFailure();
            if (config.LOG_TIME_FOR_EACH_PATH_SOLUTION) {
                start = System.nanoTime();
            }
            Optional<PathSolution> ps = mainExecutor.getPathSolution();
            if (config.LOG_TIME_FOR_EACH_PATH_SOLUTION && ps.isPresent()) {
                long end = System.nanoTime();
                Mulib.log.log(Level.INFO, "Took " + ((end - start) / 1e6) + "ms for " + config + " to get a path solution");
            }
        }
        printStatistics();
        return observedTree.getPathSolutionsList();
    }

    public synchronized List<Solution> getUpToNSolutions(int N) {
        if (numberRequestedSolutions != null) {
            throw new MulibIllegalStateException("The previous request for solutions has not been completed");
        }
        numberRequestedSolutions = new AtomicInteger(N);
        int currentNumberSolutions = solutions.size();
        while (!checkForShutdown()) {
            _getPathSolution();
        }
        printStatistics();
        numberRequestedSolutions = null;
        return solutions.subList(currentNumberSolutions, Math.min(N, solutions.size()));
    }

    public final void addToFails(Fail fail) {
        this.observedTree.addToFails(fail);
        globalExecutionManagerBudgetManager.incrementFailBudget();
    }

    public void addToPathSolutions(PathSolution pathSolution, MulibExecutor responsibleExecutor) {
        this.observedTree.addToPathSolutions(pathSolution);
        this.globalExecutionManagerBudgetManager.incrementPathSolutionBudget();
        if (numberRequestedSolutions != null) {
            solutions.addAll(responsibleExecutor.getUpToNSolutions(pathSolution, numberRequestedSolutions));
        }
    }

    public final void addToExceededBudgets(ExceededBudget exceededBudget) {
        this.observedTree.addToExceededBudgets(exceededBudget);
        this.globalExecutionManagerBudgetManager.incrementExceededBudgetBudget();
    }

    public void notifyNewChoice(int depth, List<Choice.ChoiceOption> choiceOptions) {
        observedTree.getChoiceOptionDeque().insert(depth, choiceOptions);
    }

    public final boolean globalBudgetExceeded() {
        return globalExecutionManagerBudgetManager.timeBudgetIsExceeded()
                || globalExecutionManagerBudgetManager.fixedFailBudgetIsExceeded()
                || globalExecutionManagerBudgetManager.fixedPathSolutionBudgetIsExceeded()
                || globalExecutionManagerBudgetManager.fixedExceededBudgetBudgetsIsExceeded()
                || shouldStopSinceEnoughSolutionsWereFound();
    }

    protected abstract void checkForFailure();

    protected void printStatistics() {
        StringBuilder b = new StringBuilder();
        String indent = "   ";
        String linebreak = "\r\n";
        b.append(linebreak);
        MulibExecutor last = mulibExecutors.get(mulibExecutors.size() - 1);
        for (MulibExecutor me : mulibExecutors) {
            b.append(indent)
                    .append(me.getSearchStrategy())
                    .append(": ")
                    .append(me.getStatistics().toString());
            if (me != last) {
                b.append(linebreak);
            }
        }
        if (config.ENLIST_LEAVES) {
            b.append(linebreak)
                    .append(indent)
                    .append(", numberPathSolutions: ")
                    .append(observedTree.getPathSolutionsList().size())
                    .append(", numberFails: ")
                    .append(observedTree.getFailsList().size())
                    .append(", numberExceededBudget: ")
                    .append(observedTree.getExceededBudgetList().size());

        }
        Mulib.log.log(Level.INFO, b.toString());
    }

    protected boolean checkForPause() {
        return globalBudgetExceeded() || observedTree.getChoiceOptionDeque().isEmpty();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected boolean checkForShutdown() {
        return checkForPause();
    }

    // Returns false if numberRequestedSolutions == null, which is true if we did not ask for
    // Solutions
    private boolean shouldStopSinceEnoughSolutionsWereFound() {
        return numberRequestedSolutions != null && numberRequestedSolutions.get() <= 0;
    }
}
