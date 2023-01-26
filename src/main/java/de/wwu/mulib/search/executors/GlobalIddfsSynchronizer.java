package de.wwu.mulib.search.executors;

public class GlobalIddfsSynchronizer {
    private int toReachDepth;
    private final int incrementSize;

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
