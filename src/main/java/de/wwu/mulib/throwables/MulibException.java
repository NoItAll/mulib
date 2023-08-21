package de.wwu.mulib.throwables;

/**
 * Supertype of all exceptions specific to Mulib
 */
public abstract class MulibException extends RuntimeException {

    /**
     * @param msg The message
     */
    public MulibException(String msg) {
        super(msg);
    }

    /**
     * @param msg The message
     * @param cause The cause for throwing this exception
     */
    public MulibException(String msg, Throwable cause) {
        super(msg, cause);
    }

    /**
     * @param cause The cause for throwing this exception
     */
    public MulibException(Throwable cause) {
        super(cause);
    }
}
