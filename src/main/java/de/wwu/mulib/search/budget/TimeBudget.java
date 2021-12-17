package de.wwu.mulib.search.budget;

public final class TimeBudget implements Budget {

    private long startTime;
    private final long maxDuration;

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
