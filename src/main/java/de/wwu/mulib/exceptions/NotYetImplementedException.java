package de.wwu.mulib.exceptions;

public class NotYetImplementedException extends MulibException {
    public NotYetImplementedException() {
        super("Not yet implemented.");
    }

    public NotYetImplementedException(String msg) {
        super(msg);
    }

    public NotYetImplementedException(Exception e) {
        super("Not yet implemented.", e);
    }
}
