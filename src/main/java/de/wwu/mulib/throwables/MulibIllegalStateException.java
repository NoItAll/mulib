package de.wwu.mulib.throwables;

/**
 * Is thrown if an illegal state has been identified the exact reason of which has not yet been identified.
 */
public class MulibIllegalStateException extends MulibRuntimeException {

    /**
     *
     */
    public MulibIllegalStateException() {
        super("Illegal state");
    }

    /**
     * @param msg The message
     */
    public MulibIllegalStateException(String msg) {
        super(msg);
    }

    /**
     * @param msg The message
     * @param cause The cause for throwing this exception
     */
    public MulibIllegalStateException(String msg, Exception cause) {
        super(msg, cause);
    }
}
