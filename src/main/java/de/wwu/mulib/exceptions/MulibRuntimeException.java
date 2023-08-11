package de.wwu.mulib.exceptions;

/**
 * Supertype of all runtime exceptions in Mulib
 */
public class MulibRuntimeException extends MulibException {

    /**
     * @param msg The message
     * @param cause The cause for throwing this exception
     */
    public MulibRuntimeException(String msg, Throwable cause) {
        super(msg, cause);
    }

    /**
     * @param msg The message
     */
    public MulibRuntimeException(String msg) {
        super(msg);
    }

    /**
     * @param cause The cause for throwing this exception
     */
    public MulibRuntimeException(Throwable cause) {
        super(cause);
    }
}
