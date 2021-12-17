package de.wwu.mulib.search.budget;

public class NullBudget implements Budget {

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
