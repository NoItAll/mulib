package de.wwu.mulib.search.budget;

/**
 * If no budget is specified, NullBudget will be used. NullBudget does nothing on increment and is never exceeded.
 */
public class NullBudget implements Budget {

    /**
     * The singleton instance of NullBudget
     */
    public static final NullBudget INSTANCE = new NullBudget();

    private NullBudget() {}

    @Override
    public void increment() { }

    @Override
    public boolean isExceeded() {
        return false;
    }

    @Override
    public boolean isIncremental() {
        return false;
    }

    @Override
    public Budget copyFromPrototype() {
        return this;
    }
}
