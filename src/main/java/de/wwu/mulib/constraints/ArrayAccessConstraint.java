package de.wwu.mulib.constraints;

import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.substitutions.primitives.Sprimitive;

public final class ArrayAccessConstraint implements ArrayConstraint {

    public enum Type { SELECT, STORE }

    private final Sint arrayId;
    private final Sint index;
    // Either the inserted, or the value that is selected. Potentially, this selected value is a proxy value, i.e., a
    // different symbolic value that is set to equal another symbolic value.
    private final Sprimitive value;
    private final Type type;

    public ArrayAccessConstraint(Sint arrayId, Sint index, Sprimitive value, Type type) {
        assert arrayId != null;
        this.arrayId = arrayId;
        this.index = index;
        if (value == null) {
            this.value = Sint.ConcSint.MINUS_ONE;
        } else {
            this.value = value;
        }
        this.type = type;
    }

    @Override
    public Sint getPartnerClassObjectId() {
        return arrayId;
    }

    public Sint getIndex() {
        return index;
    }

    public Sprimitive getValue() {
        return value;
    }

    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return (type == Type.SELECT ? "SELECT{" : "STORE{")
                + "ar=" + arrayId + ", " + index + ": " + value + "}";
    }
}
