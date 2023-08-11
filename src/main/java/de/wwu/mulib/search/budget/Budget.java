package de.wwu.mulib.search.budget;

/**
 * Interface for all budgets
 */
public interface Budget {

    /**
     * Increments the budget
     */
    void increment();

    /**
     * @return true, if the budget is exceeded, else false
     */
    boolean isExceeded();

    /**
     * @return true, if the budget is an incremental budget for incremental search strategies, else false
     */
    boolean isIncremental();

    /**
     * @return A copy of the given budget with the given parameters
     */
    Budget copyFromPrototype();
}
