package de.wwu.mulib.exceptions;

public class MisconfigurationException extends MulibRuntimeException {

    public MisconfigurationException(String msg) {
        super(msg);
    }

    public MisconfigurationException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public MisconfigurationException(Throwable cause) {
        super(cause);
    }
}
