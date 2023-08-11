package de.wwu.mulib.exceptions;

/**
 * Exceptions thrown in Mulib for the purpose of control flow are subclasses of {@link MulibControlFlowException}.
 * {@link de.wwu.mulib.Fail} and {@link de.wwu.mulib.search.choice_points.Backtrack} are two of such exceptions.
 * For performance reasons, no trace is recorded for them.
 */
public abstract class MulibControlFlowException extends RuntimeException {
    public MulibControlFlowException() {
        super(null, null, false, false);
    }
}
