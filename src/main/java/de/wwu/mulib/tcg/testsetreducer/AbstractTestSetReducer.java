package de.wwu.mulib.tcg.testsetreducer;

/**
 * Simply overrides {@link Object#toString()}
 */
public abstract class AbstractTestSetReducer implements TestSetReducer {
    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
