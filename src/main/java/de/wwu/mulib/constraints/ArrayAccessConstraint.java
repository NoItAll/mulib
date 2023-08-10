package de.wwu.mulib.constraints;

import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.substitutions.primitives.Sprimitive;

/**
 * Represents an access to a array symbolically. Either the array access is a SELECT access, or a STORE access.
 */
public final class ArrayAccessConstraint implements ArrayConstraint {

    /**
     * The types of array accesses that are represented
     */
    public enum Type {
        /**
         * A select access
         */
        SELECT,
        /**
         * A store access
         */
        STORE
    }

    private final Sint arrayId;
    private final Sint index;
    // Either the inserted, or the value that is selected. Potentially, this selected value is a proxy value, i.e., a
    // different symbolic value that is set to equal another symbolic value.
    private final Sprimitive value;
    private final Type type;

    /**
     * Constructs a new array access constraint
     * @param arrayId The identifier of the array that is accessed
     * @param index The index with which to access the array
     * @param value The value extracted from or stored into the array
     * @param type The type of access
     */
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

    /**
     * @return The used index
     */
    public Sint getIndex() {
        return index;
    }

    /**
     * @return The value extracted from or stored into the array
     */
    public Sprimitive getValue() {
        return value;
    }

    /**
     * @return The type of the array access
     */
    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return (type == Type.SELECT ? "SELECT{" : "STORE{")
                + "ar=" + arrayId + ", " + index + ": " + value + "}";
    }
}
