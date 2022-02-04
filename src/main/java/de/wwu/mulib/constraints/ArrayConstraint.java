package de.wwu.mulib.constraints;

import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.primitives.Sint;

public final class ArrayConstraint {

    public enum Type { SELECT, STORE }

    private final long arrayId;
    private final Sint index;
    // Either the inserted, or the value that is selected. Potentially, this selected value is a proxy value, i.e., a
    // different symbolic value that is set to equal another symbolic value.
    private final SubstitutedVar value;
    private final Type type;
    private final int level;

    public ArrayConstraint(long arrayId, Sint index, SubstitutedVar value, Type type, int level) {
        this.arrayId = arrayId;
        this.index = index;
        this.value = value;
        this.type = type;
        this.level = level;
    }

    public long getArrayId() {
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

    public int getLevel() {
        return level;
    }

    @Override
    public String toString() {
        return (type == Type.SELECT ? "SELECT{" : "STORE{")
                + "ar=" + arrayId + ", " + index + ": " + value + "}";
    }
}
