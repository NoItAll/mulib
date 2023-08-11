package de.wwu.mulib.search.budget;

/**
 * Used for budgets concerning the time. Incrementing this budget will reset the start time, i.e., thereafter TimeBudget
 * will not be exceeded anymore and the countdown is started again. It is exceeded if a number of nanoseconds has passed.
 */
public final class TimeBudget implements Budget {

    private long startTime;
    private final long maxDuration;

    /**
     * Returns a new instance of TimeBudget
     * @param maxDuration The allowed duration in nanoseconds
     * @return A new TimeBudget
     */
    public static TimeBudget getTimeBudget(long maxDuration) {
        return new TimeBudget(maxDuration, System.nanoTime());
    }

    private TimeBudget(long maxDuration, long startTime) {
        this.maxDuration = maxDuration;
        this.startTime = startTime;
    }

    @Override
    public void increment() { // Used as reset.
        this.startTime = System.nanoTime();
    }

    @Override
    public boolean isExceeded() {
        return (System.nanoTime() - maxDuration) > startTime ;
    }

    @Override
    public boolean isIncremental() {
        return false;
    }

    @Override
    public Budget copyFromPrototype() {
        return new TimeBudget(maxDuration, startTime);
    }
}
