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
        this.timeBudget = config.BUDGETS_GLOBAL_TIME_IN_NANOSECONDS.isEmpty() ?
                NullBudget.INSTANCE : TimeBudget.getTimeBudget(config.BUDGETS_GLOBAL_TIME_IN_NANOSECONDS.get());
        this.failBudget = config.BUDGETS_MAX_FAILS.isEmpty() ?
                NullBudget.INSTANCE : CountingBudget.getFixedBudget(config.BUDGETS_MAX_FAILS.get());
        this.pathSolutionsBudget = config.BUDGETS_MAX_PATH_SOLUTIONS.isEmpty() ?
                NullBudget.INSTANCE : CountingBudget.getFixedBudget(config.BUDGETS_MAX_PATH_SOLUTIONS.get());
        this.exceededBudgetsBudget = config.BUDGETS_MAX_EXCEEDED_BUDGET.isEmpty() ?
                NullBudget.INSTANCE : CountingBudget.getFixedBudget(config.BUDGETS_MAX_EXCEEDED_BUDGET.get());
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
