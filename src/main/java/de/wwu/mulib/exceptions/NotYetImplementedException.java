package de.wwu.mulib.exceptions;

/**
 * Exception indicating that some functionality has not yet been implemented in Mulib.
 */
public class NotYetImplementedException extends MulibIllegalStateException {

    /**
     *
     */
    public NotYetImplementedException() {
        super("Not yet implemented.");
    }

    /**
     * @param msg The message
     */
    public NotYetImplementedException(String msg) {
        super(msg);
    }
}
