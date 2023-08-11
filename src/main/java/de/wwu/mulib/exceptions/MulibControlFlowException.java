package de.wwu.mulib.exceptions;

public abstract class MulibControlFlowException extends RuntimeException {
    public MulibControlFlowException() {
        super(null, null, false, false);
    }
}
