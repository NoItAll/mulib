package de.wwu.mulib.search.choice_points;

import de.wwu.mulib.throwables.ExceededBudget;
import de.wwu.mulib.search.budget.Budget;

/**
 * Class indicating that a budget concerning the number of encountered choice points is exceeded
 */
public class ChoicePointExceededBudget extends ExceededBudget {
    /**
     * @param exceededBudget The budget that is exceeded
     */
    protected ChoicePointExceededBudget(Budget exceededBudget) {
        super(exceededBudget);
    }
}
