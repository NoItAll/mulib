package de.wwu.mulib.tcg.testsetreducer;

public abstract class AbstractTestSetReducer implements TestSetReducer {
    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
