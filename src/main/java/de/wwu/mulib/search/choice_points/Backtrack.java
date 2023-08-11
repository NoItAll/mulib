package de.wwu.mulib.search.choice_points;

import de.wwu.mulib.exceptions.MulibControlFlowException;

/**
 * Is thrown if we Backtrack due to a search strategy. For instance, after evaluating a choice option,
 * {@link de.wwu.mulib.search.executors.SearchStrategy#BFS} will throw this.
 */
public class Backtrack extends MulibControlFlowException {
    public Backtrack() {}
}
