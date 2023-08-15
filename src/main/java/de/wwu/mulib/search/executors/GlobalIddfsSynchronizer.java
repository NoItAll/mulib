package de.wwu.mulib.search.executors;

/**
 * Maintained in an instance of {@link MulibExecutorManager}. Is used for {@link SearchStrategy#IDDSAS} to
 * evaluate until which depth we should evaluate {@link de.wwu.mulib.search.trees.Choice.ChoiceOption}s.
 */
public class GlobalIddfsSynchronizer {
    private int toReachDepth;
    private final int incrementSize;

    /**
     * Constructs a new instance
     * @param incrementSize The increment size to increase the current targeted reached depth
     */
    public GlobalIddfsSynchronizer(int incrementSize) {
        this.incrementSize = incrementSize;
        this.toReachDepth = incrementSize;
    }

    public int getToReachDepth() {
        return toReachDepth;
    }

    public synchronized void setNextDepth(int currentStartingDepth) {
        if (currentStartingDepth >= toReachDepth) {
            toReachDepth = currentStartingDepth + incrementSize;
        }
    }
}
