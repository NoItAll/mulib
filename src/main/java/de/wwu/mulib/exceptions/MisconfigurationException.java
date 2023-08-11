package de.wwu.mulib.exceptions;

/**
 * Is thrown if an illegal configuration has been detected. For instance, if an incremental search strategy has been
 * set, but no incremental budget has been defined.
 */
public class MisconfigurationException extends MulibRuntimeException {

    /**
     * @param msg The message
     */
    public MisconfigurationException(String msg) {
        super(msg);
    }

    /**
     * @param msg The message
     * @param cause The Throwable indicating an illegal exception
     */
    public MisconfigurationException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
