package de.wwu.mulib.constraints;

import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.substitutions.primitives.Sprimitive;

/**
 * Represents an access to a non-array object symbolically. Either the access is a GETFIELD access, or a PUTFIELD access.
 */
public final class PartnerClassObjectFieldConstraint implements PartnerClassObjectConstraint {

    /**
     * The types of accesses
     */
    public enum Type {
        /**
         * A value is retrieved from a non-array object's field
         */
        GETFIELD,
        /**
         * A value is stored in a non-array object's field
         */
        PUTFIELD
    }

    private final Sint partnerClassObjectId;
    private final String fieldName;
    private final Sprimitive value;
    private final Type type;

    /**
     * Constructs a new access constraint for a non-array object
     * @param partnerClassObjectId The identifier of the non-array object that is accessed
     * @param fieldName The accessed field's name. Also contains the name of the class to which this field belongs
     * @param value The value extracted from or stored in the field
     * @param type The type of access
     */
    public PartnerClassObjectFieldConstraint(
            Sint partnerClassObjectId,
            String fieldName,
            Sprimitive value,
            Type type) {
        this.partnerClassObjectId = partnerClassObjectId;
        this.fieldName = fieldName;
        this.value = value;
        this.type = type;
    }

    @Override
    public Sint getPartnerClassObjectId() {
        return partnerClassObjectId;
    }

    /**
     * @return The name of the accessed field. Also contains the name of the class to which this field belongs.
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * @return The value extracted from or stored into the field
     */
    public Sprimitive getValue() {
        return value;
    }

    /**
     * @return The type of field access
     */
    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return String.format(
                "POField{partnerClassObjectId=%s, fieldName=%s, value=%s, type=%s}",
                partnerClassObjectId,
                fieldName,
                value,
                type
        );
    }
}
