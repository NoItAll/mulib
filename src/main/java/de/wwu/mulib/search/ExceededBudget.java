package de.wwu.mulib.search;

import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.search.budget.Budget;

public abstract class ExceededBudget extends MulibRuntimeException {

    private final Budget exceededBudget;

    protected ExceededBudget(Budget exceededBudget) {
        super("Budget exceeded.");
        this.exceededBudget = exceededBudget;
    }

    public final Budget getExceededBudget() {
        return exceededBudget;
    }

}
