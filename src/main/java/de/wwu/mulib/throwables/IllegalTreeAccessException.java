package de.wwu.mulib.throwables;

/**
 * Is thrown if attributes of the {@link de.wwu.mulib.search.trees.SearchTree} are accessed that are not yet properly
 * initialized.
 */
public class IllegalTreeAccessException extends MulibIllegalStateException {

    /**
     * @param msg The message
     */
    public IllegalTreeAccessException(String msg) {
        super(msg);
    }

}
