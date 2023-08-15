package de.wwu.mulib.search.choice_points;

import de.wwu.mulib.exceptions.MulibControlFlowException;

/**
 * Is thrown if we Backtrack due to a search strategy. For instance, after evaluating a choice option,
 * {@link de.wwu.mulib.search.executors.SearchStrategy#BFS} will throw this.
 * This should mostly be thrown in a {@link ChoicePointFactory}.
 */
public final class Backtrack extends MulibControlFlowException {
    private final static Backtrack BACKTRACK = new Backtrack();
    private Backtrack() {}

    public static Backtrack getInstance() {
        return BACKTRACK;
    }
}
