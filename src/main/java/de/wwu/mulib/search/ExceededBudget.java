package de.wwu.mulib.search;

import de.wwu.mulib.search.budget.Budget;

public class ExceededBudget extends RuntimeException {

    private final Budget exceededBudget;

    protected ExceededBudget(Budget exceededBudget) {
        super("Budget exceeded.");
        this.exceededBudget = exceededBudget;
    }

    public final Budget getExceededBudget() {
        return exceededBudget;
    }

}
