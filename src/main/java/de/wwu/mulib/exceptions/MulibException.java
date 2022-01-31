package de.wwu.mulib.exceptions;

public abstract class MulibException extends RuntimeException {
    public MulibException(String msg) {
        super(msg);
    }

    public MulibException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public MulibException(Throwable cause) {
        super(cause);
    }
}
