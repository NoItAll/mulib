package de.wwu.mulib.exceptions;

/**
 * Is thrown if the solver cannot decide if some formula is satisfiable or unsatisfiable
 */
public class UnknownSolutionException extends MulibRuntimeException {
    /**
     * @param msg The message
     */
    public UnknownSolutionException(String msg) {
        super(msg);
    }
}
