package de.wwu.mulib.search.executors;

import de.wwu.mulib.Mulib;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.search.budget.GlobalExecutionBudgetManager;
import de.wwu.mulib.search.choice_points.ChoicePointFactory;
import de.wwu.mulib.search.trees.*;
import de.wwu.mulib.substitutions.primitives.ValueFactory;
import de.wwu.mulib.transformations.MulibValueTransformer;

import java.util.List;
import java.util.Optional;
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
        mulibExecutors.add(new GenericExecutor(
                observedTree.root.getOption(0),
                this,
                this.mulibValueTransformer,
                config,
                config.GLOBAL_SEARCH_STRATEGY
        ));
        this.globalExecutionManagerBudgetManager = new GlobalExecutionBudgetManager(config);
        this.mainExecutor = this.mulibExecutors.get(0);
    }

    public Optional<PathSolution> getPathSolution() {
        int currentNumberSolutions = observedTree.getSolutionsList().size();
        globalExecutionManagerBudgetManager.resetTimeBudget();
        while (!checkForShutdown()) {
            Optional<PathSolution> possiblePathSolution = mainExecutor.runForSinglePathSolution();
            checkForFailure();
            if (possiblePathSolution.isPresent()) {
                printStatistics();
                return possiblePathSolution;
            }
        }
        printStatistics();
        if (observedTree.getSolutionsList().size() > currentNumberSolutions) {
            // Potentially, the result has been computed by another thread. In this case, we simply return
            // the last path solution added to the observed tree. It is mandatory that, in fact, a new path-solution
            // was added
            return Optional.of(observedTree.getSolutionsList().get(observedTree.getSolutionsList().size() - 1));
        }
        return Optional.empty();
    }

    public List<PathSolution> getAllPathSolutions() {
        globalExecutionManagerBudgetManager.resetTimeBudget();
        // We constantly poll with the mainExecutor.
        while (!checkForShutdown()) {
            checkForFailure();
            mainExecutor.runForSinglePathSolution();
        }
        printStatistics();
        return observedTree.getSolutionsList();
    }

    public final void addToFails(Fail fail) {
        this.observedTree.addToFails(fail);
        globalExecutionManagerBudgetManager.incrementFailBudget();
    }

    public final void addToPathSolutions(PathSolution pathSolution) {
        this.observedTree.addToPathSolutions(pathSolution);
        this.globalExecutionManagerBudgetManager.incrementPathSolutionBudget();
    }

    public final void addToExceededBudgets(ExceededBudget exceededBudget) {
        this.observedTree.addToExceededBudgets(exceededBudget);
        this.globalExecutionManagerBudgetManager.incrementExceededBudgetBudget();
    }

    public void notifyNewChoice(int depth, List<Choice.ChoiceOption> choiceOptions) {
        observedTree.getChoiceOptionDeque().insert(depth, choiceOptions);
    }

    public final boolean globalBudgetExceeded() {
        boolean timeBudgetExceeded = globalExecutionManagerBudgetManager.timeBudgetIsExceeded() ;
        boolean failBudgetExceeded = globalExecutionManagerBudgetManager.fixedFailBudgetIsExceeded();
        boolean pathSolutionBudget = globalExecutionManagerBudgetManager.fixedPathSolutionBudgetIsExceeded();
        boolean exceededBudgetBudget = globalExecutionManagerBudgetManager.fixedExceededBudgetBudgetsIsExceeded();
        return timeBudgetExceeded || failBudgetExceeded || pathSolutionBudget || exceededBudgetBudget;
    }

    protected void checkForFailure() { }

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
        Mulib.log.log(Level.INFO, b.toString());
    }

    protected abstract boolean checkForPause();

    protected abstract boolean checkForShutdown();

    protected void computePathSolutionsWithNonMainExecutor(MulibExecutor mulibExecutor) {
        while (!checkForPause()) {
            mulibExecutor.runForSinglePathSolution();
        }
    }
}
