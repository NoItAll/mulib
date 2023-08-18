package de.wwu.mulib.exceptions;

import de.wwu.mulib.search.budget.Budget;

/**
 * Abstract supertype of all exceptions for exceeded runtime-budgets
 */
public abstract class ExceededBudget extends MulibRuntimeException {

    private final Budget exceededBudget;

    /**
     * Constructor for calling in subclasses
     * @param exceededBudget The budget that is exceeded
     */
    protected ExceededBudget(Budget exceededBudget) {
        super("Budget exceeded.");
        this.exceededBudget = exceededBudget;
    }

    /**
     * @return The budget that is exceeded
     */
    public final Budget getExceededBudget() {
        return exceededBudget;
    }

}
