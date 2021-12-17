package de.wwu.mulib.exceptions;

public class MulibRuntimeException extends MulibException {

    @SuppressWarnings("all")
    private Throwable throwable;

    public MulibRuntimeException(String msg, Throwable e) {
        super(msg, e);
    }

    public MulibRuntimeException(String msg) {
        super(msg);
    }

    public MulibRuntimeException(Exception e) {
        super(e);
    }

    public MulibRuntimeException(Throwable t) {
        super("Throwable caught: " + t);
        this.throwable = t;
    }
}
