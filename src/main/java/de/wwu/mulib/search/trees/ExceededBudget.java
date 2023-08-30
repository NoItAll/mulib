package de.wwu.mulib.search.trees;

import de.wwu.mulib.search.budget.Budget;

/**
 * A tree node representing the abortion of exploring a path through the {@link SearchTree} due to an exceeded budget.
 */
public class ExceededBudget extends TreeNode {

    /**
     * The budget that was exceeded
     */
    private final Budget exceededBudget;

    ExceededBudget(Choice.ChoiceOption parent, Budget exceededBudget) {
        super(parent);
        this.exceededBudget = exceededBudget;
    }

    public Budget getExceededBudget() {
        return exceededBudget;
    }

}
