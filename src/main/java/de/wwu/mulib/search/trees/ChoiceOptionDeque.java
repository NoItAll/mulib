package de.wwu.mulib.search.trees;

import java.util.List;
import java.util.Optional;

/**
 * Represents a priority double-ended queue. The priority is the depth of a choice option in the
 * {@link SearchTree}.
 * This queue MUST be thread-safe and is the synchronization instance where choice options can be retrieved from by multiple
 * {@link de.wwu.mulib.search.executors.MulibExecutor}s without redundantly evaluating choice options.
 * It is not strictly a double-ended queue anymore, since we can also request choice options from the middle of
 * the deque.
 */
public interface ChoiceOptionDeque {

    /**
     * @return One of the unevaluated choice options with the lowest depth.
     * Is typically used by {@link de.wwu.mulib.search.executors.SearchStrategy#BFS} and {@link de.wwu.mulib.search.executors.SearchStrategy#IDDFS}.
     */
    Optional<Choice.ChoiceOption> pollFirst();

    /**
     * @return One of the unevaluated choice options with the highest depth.
     * Is typically used by {@link de.wwu.mulib.search.executors.SearchStrategy#DFS}.
     */
    Optional<Choice.ChoiceOption> pollLast();

    /**
     * Inserts a list of choice options into the deque at the specified depth.
     * @param depth The depth to insert the choice options at
     * @param choiceOptions The choice options. Do not necessarily be unevaluated.
     */
    void insert(int depth, List<Choice.ChoiceOption> choiceOptions);

    /**
     * @return true, if there are no more choice options in the deque, else false
     */
    boolean isEmpty();

    /**
     * Request a choice option from the deque.
     * Is typically used by {@link de.wwu.mulib.search.executors.SearchStrategy#DSAS} and {@link de.wwu.mulib.search.executors.SearchStrategy#IDDSAS}.
     * If the requested choice option is available, it will be removed from the deque.
     * @param requested The requested choice option
     * @return true, if the choice option is available and has not been requested by another consumer, else false
     */
    boolean request(Choice.ChoiceOption requested);

    /**
     * Clears the deque
     */
    void setEmpty();

    /**
     * @return The number of options in the deque
     */
    int size();

    /**
     * A predefined min-max array for the case of an empty deque
     */
    int[] minMaxZero = { 0, 0 };

    /**
     * @return A pair consisting of the lowest and the highest depth in the deque
     */
    int[] getMinMaxDepth();
}
