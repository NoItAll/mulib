package de.wwu.mulib.exceptions;

public class IllegalTreeModificationException extends MulibIllegalStateException {
    public IllegalTreeModificationException(String cause) {
        super(cause);
    }
}
