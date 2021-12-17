package de.wwu.mulib.search.executors;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.search.budget.GlobalExecutionBudgetManager;
import de.wwu.mulib.search.choice_points.ChoicePointFactory;
import de.wwu.mulib.search.trees.*;
import de.wwu.mulib.substitutions.primitives.ValueFactory;
import de.wwu.mulib.transformations.MulibValueTransformer;

import java.util.*;

public abstract class MulibExecutorManager {

    protected final MulibConfig config;
    protected final SearchTree observedTree;
    protected final List<MulibExecutor> mulibExecutors;
    protected final ChoicePointFactory choicePointFactory;
    protected final ValueFactory valueFactory;
    protected final CalculationFactory calculationFactory;
    protected final GlobalExecutionBudgetManager globalExecutionManagerBudgetManager;
    protected final MulibValueTransformer prototypicalMulibValueTransformer;

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
        this.prototypicalMulibValueTransformer = mulibValueTransformer;
        mulibExecutors.add(new GenericExecutor(
                observedTree.root.getOption(0),
                this,
                prototypicalMulibValueTransformer,
                config,
                config.GLOBAL_SEARCH_STRATEGY
        ));
        this.globalExecutionManagerBudgetManager = new GlobalExecutionBudgetManager(config);
    }

    public abstract Optional<PathSolution> getSolution();

    public abstract List<PathSolution> getAllSolutions();

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
}
