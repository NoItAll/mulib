package de.wwu.mulib;

import de.wwu.mulib.exceptions.MulibControlFlowException;

public final class Fail extends MulibControlFlowException {
    private final static Fail FAIL = new Fail();
    private Fail() {}

    public static Fail getInstance() {
        return FAIL;
    }
}
