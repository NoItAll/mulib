package de.wwu.mulib.tcg.testsetsorter;

public abstract class AbstractTestSetSorter implements TestSetSorter {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
