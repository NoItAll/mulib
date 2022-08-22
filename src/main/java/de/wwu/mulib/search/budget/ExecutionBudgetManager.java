package de.wwu.mulib.search.budget;

import de.wwu.mulib.MulibConfig;

public class ExecutionBudgetManager {

    private final Budget fixedActualChoicePointBudget;
    private final Budget incrementalActualChoicePointBudget;

    private ExecutionBudgetManager(MulibConfig config) {
        this.fixedActualChoicePointBudget = config.FIXED_ACTUAL_CP_BUDGET.isPresent() ?
                CountingBudget.getFixedBudget(config.FIXED_ACTUAL_CP_BUDGET.get())
                :
                NullBudget.INSTANCE;
        this.incrementalActualChoicePointBudget = config.INCR_ACTUAL_CP_BUDGET.isPresent() ?
                CountingBudget.getIncrementalBudget(config.INCR_ACTUAL_CP_BUDGET.get())
                :
                NullBudget.INSTANCE;
    }

    private ExecutionBudgetManager(
            Budget fixedActualChoicePointBudget,
            Budget incrementalActualChoicePointBudget) {
        this.fixedActualChoicePointBudget = fixedActualChoicePointBudget;
        this.incrementalActualChoicePointBudget = incrementalActualChoicePointBudget;
    }

    public static ExecutionBudgetManager newInstance(MulibConfig config) {
        return new ExecutionBudgetManager(config);
    }

    public boolean fixedActualChoicePointBudgetIsExceeded() {
        return _isExceeded(fixedActualChoicePointBudget);
    }

    public boolean incrementalActualChoicePointBudgetIsExceeded() {
        return _isExceeded(incrementalActualChoicePointBudget);
    }

    private static boolean _isExceeded(Budget b) {
        b.increment();
        return b.isExceeded();
    }

    public Budget getFixedActualChoicePointBudget() {
        return fixedActualChoicePointBudget;
    }

    public ExecutionBudgetManager copyFromPrototype() {
        return new ExecutionBudgetManager(
                fixedActualChoicePointBudget.copyFromPrototype(),
                incrementalActualChoicePointBudget.copyFromPrototype()
        );
    }
}
