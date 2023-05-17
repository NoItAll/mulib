package de.wwu.mulib.exceptions;

public class MulibIllegalStateException extends MulibException {

    public MulibIllegalStateException() {}

    public MulibIllegalStateException(String msg) {
        super(msg);
    }

    public MulibIllegalStateException(String msg, Exception e) {
        super(msg, e);
    }
}
