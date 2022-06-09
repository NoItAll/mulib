package de.wwu.mulib.exceptions;

public class TimeoutException extends MulibRuntimeException {

    public TimeoutException() {
        super("Execution took too long.");
    }
}
