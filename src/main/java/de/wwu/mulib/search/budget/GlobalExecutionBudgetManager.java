package de.wwu.mulib.search.budget;

import de.wwu.mulib.MulibConfig;

/**
 * Manages budgets that are held for the entire execution.
 */
public class GlobalExecutionBudgetManager {

    private final Budget timeBudget;
    private final Budget failBudget;
    private final Budget pathSolutionsBudget;
    private final Budget exceededBudgetsBudget;

    /**
     * @param config The configuration with the respective global budget settings
     */
    public GlobalExecutionBudgetManager(
            MulibConfig config) {
        this.timeBudget = config.NANOSECONDS_PER_INVOCATION.isEmpty() ?
                NullBudget.INSTANCE : TimeBudget.getTimeBudget(config.NANOSECONDS_PER_INVOCATION.get());
        this.failBudget = config.MAX_FAILS.isEmpty() ?
                NullBudget.INSTANCE : CountingBudget.getFixedBudget(config.MAX_FAILS.get());
        this.pathSolutionsBudget = config.MAX_PATH_SOLUTIONS.isEmpty() ?
                NullBudget.INSTANCE : CountingBudget.getFixedBudget(config.MAX_PATH_SOLUTIONS.get());
        this.exceededBudgetsBudget = config.MAX_EXCEEDED_BUDGETS.isEmpty() ?
                NullBudget.INSTANCE : CountingBudget.getFixedBudget(config.MAX_EXCEEDED_BUDGETS.get());
    }

    /**
     * Restarts the time budget
     */
    public void resetTimeBudget() {
        timeBudget.increment();
    }

    /**
     * Increments the path solution budget, i.e., another {@link de.wwu.mulib.search.trees.PathSolution} has been found
     */
    public void incrementPathSolutionBudget() {
        pathSolutionsBudget.increment();
    }

    /**
     * Increments the fail budget, i.e., a {@link de.wwu.mulib.Fail} has been thrown
     */
    public void incrementFailBudget() {
        failBudget.increment();
    }

    /**
     * Increments the budget tracking the number of exceeded budgets encountered by {@link ExecutionBudgetManager}s.
     */
    public void incrementExceededBudgetBudget() {
        exceededBudgetsBudget.increment();
    }

    /**
     * @return true, if the time budget is exceeded, else false
     */
    public boolean timeBudgetIsExceeded() {
        return timeBudget.isExceeded();
    }

    /**
     * @return true, if the path solution budget is exceeded, else false
     */
    public boolean fixedPathSolutionBudgetIsExceeded() {
        return pathSolutionsBudget.isExceeded();
    }

    /**
     * @return true, if the fail budget is exceeded, else false
     */
    public boolean fixedFailBudgetIsExceeded() {
        return failBudget.isExceeded();
    }

    /**
     * @return true, if the exceeded budgets budget is exceeded, else false
     */
    public boolean fixedExceededBudgetBudgetsIsExceeded() {
        return exceededBudgetsBudget.isExceeded();
    }
}
