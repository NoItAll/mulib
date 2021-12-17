package de.wwu.mulib.search.budget;

import de.wwu.mulib.MulibConfig;

public class GlobalExecutionBudgetManager {

    private final Budget timeBudget;
    private final Budget failBudget;
    private final Budget pathSolutionsBudget;
    private final Budget exceededBudgetsBudget;

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

    public void resetTimeBudget() {
        timeBudget.increment();
    }

    public void incrementPathSolutionBudget() {
        pathSolutionsBudget.increment();
    }

    public void incrementFailBudget() {
        failBudget.increment();
    }

    public void incrementExceededBudgetBudget() {
        exceededBudgetsBudget.increment();
    }

    public boolean timeBudgetIsExceeded() {
        return timeBudget.isExceeded();
    }

    public boolean fixedPathSolutionBudgetIsExceeded() {
        return pathSolutionsBudget.isExceeded();
    }

    public boolean fixedFailBudgetIsExceeded() {
        return failBudget.isExceeded();
    }

    public boolean fixedExceededBudgetBudgetsIsExceeded() {
        return exceededBudgetsBudget.isExceeded();
    }
}
