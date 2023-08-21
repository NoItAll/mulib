package de.wwu.mulib.throwables;

/**
 * Error thrown during runtime for unrecoverable error
 */
public class MulibRuntimeError extends Error {

    /**
     * @param msg The message to display to the user
     */
    public MulibRuntimeError(String msg) {
        super(msg);
    }

}
