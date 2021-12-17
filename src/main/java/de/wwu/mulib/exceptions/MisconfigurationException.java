package de.wwu.mulib.exceptions;

public class MisconfigurationException extends MulibException {

    public MisconfigurationException(String msg) {
        super(msg);
    }

    public MisconfigurationException(String msg, Exception cause) {
        super(msg, cause);
    }

    public MisconfigurationException(Exception cause) {
        super(cause);
    }
}
