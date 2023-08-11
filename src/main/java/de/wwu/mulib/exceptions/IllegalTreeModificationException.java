package de.wwu.mulib.exceptions;

/**
 * Is thrown if the {@link de.wwu.mulib.search.trees.SearchTree} is modified in an illegal manner. For instance,
 * if constraints are added to a ChoiceOption that was already declared to be fully evaluated or if it is tried to set
 * the child of a {@link de.wwu.mulib.search.trees.Choice.ChoiceOption} that already was set, or if the state
 * of a {@link de.wwu.mulib.search.trees.Choice.ChoiceOption} is updated in an illegal manner (for instance from
 * 'evaluated' to 'unknown', this is thrown.
 */
public class IllegalTreeModificationException extends MulibIllegalStateException {
    /**
     * @param msg The message
     */
    public IllegalTreeModificationException(String msg) {
        super(msg);
    }
}
