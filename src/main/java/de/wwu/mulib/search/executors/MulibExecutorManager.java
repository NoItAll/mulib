package de.wwu.mulib.search.executors;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.search.budget.GlobalExecutionBudgetManager;
import de.wwu.mulib.search.choice_points.ChoicePointFactory;
import de.wwu.mulib.search.trees.*;
import de.wwu.mulib.search.values.ValueFactory;

import java.util.*;

public abstract class MulibExecutorManager {

    protected final MulibConfig config;
    protected final SearchTree observedTree;
    protected final List<MulibExecutor> mulibExecutors;
    protected final ChoicePointFactory choicePointFactory;
    protected final ValueFactory valueFactory;
    protected final GlobalExecutionBudgetManager globalExecutionManagerBudgetManager;

    protected MulibExecutorManager(
            MulibConfig config,
            List<MulibExecutor> mulibExecutorsList,
            SearchTree observedTree,
            ChoicePointFactory choicePointFactory,
            ValueFactory valueFactory) {
        this.config = config;
        this.observedTree = observedTree;
        this.choicePointFactory = choicePointFactory;
        this.valueFactory = valueFactory;
        this.mulibExecutors = mulibExecutorsList;
        mulibExecutors.add(new GenericExecutor(
                observedTree.root.getOption(0),
                this,
                config,
                config.GLOBAL_SEARCH_STRATEGY
        ));
        this.globalExecutionManagerBudgetManager = new GlobalExecutionBudgetManager(config);
    }

    public abstract Optional<PathSolution> getSolution();

    public abstract List<PathSolution> getAllSolutions();

    // TODO Refactor
    public abstract List<PathSolution> getAllSolutions(List<Object> args);

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

    protected final List<PathSolution> getAllSolutions(MulibExecutor executorToDispatch) {
        List<PathSolution> result = new ArrayList<>();
        while (!observedTree.getChoiceOptionDeque().isEmpty() && !globalBudgetExceeded()) {
            List<PathSolution> solutions  = executorToDispatch.runForSolutions();
            result.addAll(solutions);
        }
        return result;
    }

    protected final List<PathSolution> getAllSolutions(MulibExecutor executorToDispatch, List<Object> arguments) {
        List<PathSolution> result = new ArrayList<>();
        while (!observedTree.getChoiceOptionDeque().isEmpty() && !globalBudgetExceeded()) {
            List<PathSolution> solutions  = executorToDispatch.runForSolutions(arguments);
            result.addAll(solutions);
        }
        return result;
    }
}
