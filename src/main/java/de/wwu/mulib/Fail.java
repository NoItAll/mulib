package de.wwu.mulib;

import de.wwu.mulib.exceptions.MulibControlFlowException;

/**
 * Special exception that can be thrown to indicate that the current branch in the search tree should
 * be explicitly discarded.
 */
public final class Fail extends MulibControlFlowException {
    private final static Fail FAIL = new Fail();
    private Fail() {}

    static Fail getInstance() {
        return FAIL;
    }
}
