package de.wwu.mulib.search.budget;

import de.wwu.mulib.MulibConfig;

/**
 * Manages budgets that are given for one single run of {@link de.wwu.mulib.search.executors.SymbolicExecution}.
 */
public class ExecutionBudgetManager {
    // If there are no budgets, we do not need to actually copy this
    private final boolean shouldNotBeCopied;
    private final Budget fixedActualChoicePointBudget;
    private final Budget incrementalActualChoicePointBudget;

    private ExecutionBudgetManager(MulibConfig config) {
        this.fixedActualChoicePointBudget = config.BUDGETS_FIXED_ACTUAL_CP.isPresent() ?
                CountingBudget.getFixedBudget(config.BUDGETS_FIXED_ACTUAL_CP.get())
                :
                NullBudget.INSTANCE;
        this.incrementalActualChoicePointBudget = config.BUDGETS_INCR_ACTUAL_CP.isPresent() ?
                CountingBudget.getIncrementalBudget(config.BUDGETS_INCR_ACTUAL_CP.get())
                :
                NullBudget.INSTANCE;
        this.shouldNotBeCopied = config.BUDGETS_FIXED_ACTUAL_CP.isEmpty() && config.BUDGETS_INCR_ACTUAL_CP.isEmpty();
    }

    private ExecutionBudgetManager(
            Budget fixedActualChoicePointBudget,
            Budget incrementalActualChoicePointBudget) {
        this.fixedActualChoicePointBudget = fixedActualChoicePointBudget;
        this.incrementalActualChoicePointBudget = incrementalActualChoicePointBudget;
        this.shouldNotBeCopied = false;
    }

    /**
     * @param config The configuration with settings for the given budget
     * @return An ExecutionBudgetManager. If no budgets are set {@link NullBudget} is used
     */
    public static ExecutionBudgetManager newInstance(MulibConfig config) {
        return new ExecutionBudgetManager(config);
    }

    /**
     * @return true, if the actual choice point budget (concerning the depth in the search tree) is exceeded, else false
     */
    public boolean fixedActualChoicePointBudgetIsExceeded() {
        return _isExceeded(fixedActualChoicePointBudget);
    }

    /**
     * @return true, if the incremental budget for an incremental search strategy is exceeded, else false
     */
    public boolean incrementalActualChoicePointBudgetIsExceeded() {
        return _isExceeded(incrementalActualChoicePointBudget);
    }

    private static boolean _isExceeded(Budget b) {
        b.increment();
        return b.isExceeded();
    }

    /**
     * @return The budget concerning the depth in the search tree
     */
    public Budget getFixedActualChoicePointBudget() {
        return fixedActualChoicePointBudget;
    }

    /**
     * @return A copy where each budget is also copied from its prototype
     */
    public ExecutionBudgetManager copyFromPrototype() {
        if (shouldNotBeCopied) {
            return this;
        } else {
            return new ExecutionBudgetManager(
                    fixedActualChoicePointBudget.copyFromPrototype(),
                    incrementalActualChoicePointBudget.copyFromPrototype()
            );
        }
    }
}
