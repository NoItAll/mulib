package de.wwu.mulib.search.budget;

public interface Budget {

    void increment();

    boolean isExceeded();

    boolean isIncremental();

    Budget copyFromPrototype();
}
