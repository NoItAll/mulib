package de.wwu.mulib.search.executors;

import de.wwu.mulib.search.trees.Choice;
import de.wwu.mulib.search.trees.ChoiceOptionDeque;

public enum SearchStrategy {
    /**
     * Breadth-first search on a (global) queue
     * @see ChoiceOptionDeque#pollFirst()
     */
    BFS,
    /**
     * Depth-first search on a (global) stack
     * @see ChoiceOptionDeque#pollLast()
     */
    DFS,
    /**
     * Iterative deepening depth-first search.
     * In this variant, we always select one {@link de.wwu.mulib.search.trees.Choice.ChoiceOption} with
     * the lowest depth and evaluate it until we have not more budget.
     * @see ChoiceOptionDeque#pollFirst()
     */
    IDDFS,
    /**
     * Iterative deepening deepest shared ancestor search.
     * In this variant of iterative deepening depth-first search, we search for {@link de.wwu.mulib.search.trees.Choice.ChoiceOption}s
     * which share the deepest possible ancestor to the previous evaluated {@link de.wwu.mulib.search.trees.Choice.ChoiceOption}.
     * This option is evaluated until reaching the current depth-target specified by {@link GlobalIddfsSynchronizer#getToReachDepth()}.
     * @see ChoiceOptionDeque#request(Choice.ChoiceOption)
     */
    IDDSAS,
    /**
     * Variant of depth-first search where, instead of choosing the uppermost {@link de.wwu.mulib.search.trees.Choice.ChoiceOption}
     * to start a new run with, we instead try to evaluate the option where the ancestor deepest in the tree is shared with the
     * previous evaluated {@link de.wwu.mulib.search.trees.Choice.ChoiceOption}.
     * In the single-threaded case, this is equivalent, although slightly less efficient, to {@link SearchStrategy#DFS}.
     * @see ChoiceOptionDeque#request(Choice.ChoiceOption)
     */
    DSAS
}
