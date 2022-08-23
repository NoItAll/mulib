package de.wwu.mulib.constraints;

import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.primitives.Sint;

public final class ArrayConstraint {

    public enum Type { SELECT, STORE }

    private final Sint arrayId;
    // Each ArrayConstraint currently needs to be able to initialize a constraint representation in the backend, thus, we also store arraylength
    private final Sint arrayLength;
    private final Sint index;
    // Either the inserted, or the value that is selected. Potentially, this selected value is a proxy value, i.e., a
    // different symbolic value that is set to equal another symbolic value.
    private final SubstitutedVar value;
    private final Type type;

    public ArrayConstraint(Sint arrayId, Sint arrayLength, Sint index, SubstitutedVar value, Type type) {
        assert arrayId != null;
        this.arrayId = arrayId;
        this.arrayLength = arrayLength;
        this.index = index;
        this.value = value;
        this.type = type;
    }

    public Sint getArrayId() {
        return arrayId;
    }

    public Sint getIndex() {
        return index;
    }

    public SubstitutedVar getValue() {
        return value;
    }

    public Type getType() {
        return type;
    }

    public Sint getArrayLength() {
        return arrayLength;
    }

    @Override
    public String toString() {
        return (type == Type.SELECT ? "SELECT{" : "STORE{")
                + "ar=" + arrayId + ", " + index + ": " + value + "}";
    }
}
