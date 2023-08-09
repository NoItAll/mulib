package de.wwu.mulib.exceptions;

public class LabelingNotPossibleException extends MulibRuntimeException {

    public LabelingNotPossibleException(String msg) {
        super(msg);
    }

    public LabelingNotPossibleException(String msg, Exception e) {
        super(msg, e);
    }

}
