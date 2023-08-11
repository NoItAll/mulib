package de.wwu.mulib.search.budget;

/**
 * Used for budgets based on the number of (actual and possible) choice points seen, the number of solutions, and fails.
 */
public final class CountingBudget implements Budget {
    private final long maxNumber;
    private final boolean isIncremental;
    private long seenNumber;

    /**
     * @param incrementBy How much should this incremental budget be incrementable until the execution of the
     *                    incremental search strategy is aborted?
     * @return The incremental budget
     */
    public static CountingBudget getIncrementalBudget(long incrementBy) {
        return new CountingBudget(incrementBy, true);
    }

    /**
     * @param maxNumber How much should this non-incremental budget be incrementable until the execution declares the
     *                  current choice option as unsatisfiable?
     * @return The budget
     */
    public static CountingBudget getFixedBudget(long maxNumber) {
        return new CountingBudget(maxNumber, false);
    }

    private CountingBudget(
            long maxNumber,
            boolean isIncremental) {
        this.maxNumber = maxNumber;
        this.isIncremental = isIncremental;
        this.seenNumber = 0;
    }

    @Override
    public void increment() {
        seenNumber++;
    }

    @Override
    public boolean isExceeded() {
        return seenNumber >= maxNumber;
    }

    @Override
    public boolean isIncremental() {
        return isIncremental;
    }

    @Override
    public CountingBudget copyFromPrototype() {
        return new CountingBudget(
                maxNumber,
                isIncremental
        );
    }
}
