package de.wwu.mulib.search.trees;

import de.wwu.mulib.search.budget.Budget;

public class ExceededBudget extends TreeNode {

    public final Budget exceededBudget;

    public ExceededBudget(Choice.ChoiceOption parent, Budget exceededBudget) {
        super(parent);
        this.exceededBudget = exceededBudget;
    }

}
