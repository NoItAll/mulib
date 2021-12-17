package de.wwu.mulib.exceptions;

public class TimeoutException extends MulibException {

    public TimeoutException() {
        super("Execution took too long.");
    }
}
