package de.wwu.mulib.throwables;

/**
 * Is thrown if an error is encountered during labeling. For instance, if the constraint stack is not satisfiable and
 * it is tried to label a value, this is thrown.
 */
public class LabelingNotPossibleException extends MulibRuntimeException {

    /**
     * @param msg The message
     */
    public LabelingNotPossibleException(String msg) {
        super(msg);
    }

    /**
     * @param msg The message
     * @param e The exception responsible for throwing this exception
     */
    public LabelingNotPossibleException(String msg, Exception e) {
        super(msg, e);
    }

}
