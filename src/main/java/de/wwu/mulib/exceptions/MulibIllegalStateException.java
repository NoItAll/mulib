package de.wwu.mulib.exceptions;

public class MulibIllegalStateException extends MulibRuntimeException {

    public MulibIllegalStateException() {
        super("Illegal state");
    }

    public MulibIllegalStateException(String msg) {
        super(msg);
    }

    public MulibIllegalStateException(String msg, Exception e) {
        super(msg, e);
    }
}
