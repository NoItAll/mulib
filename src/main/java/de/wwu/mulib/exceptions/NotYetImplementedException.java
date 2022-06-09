package de.wwu.mulib.exceptions;

public class NotYetImplementedException extends MulibIllegalStateException {
    public NotYetImplementedException() {
        super("Not yet implemented.");
    }

    public NotYetImplementedException(String msg) {
        super(msg);
    }
}
