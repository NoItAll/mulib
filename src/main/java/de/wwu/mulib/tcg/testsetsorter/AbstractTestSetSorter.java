package de.wwu.mulib.tcg.testsetsorter;

/**
 * Simply overrides {@link Object#toString()}
 */
public abstract class AbstractTestSetSorter implements TestSetSorter {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
