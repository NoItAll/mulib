package de.wwu.mulib.constraints;

import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.substitutions.primitives.Sprimitive;

public final class PartnerClassObjectFieldAccessConstraint implements PartnerClassObjectConstraint {

    public enum Type { GETFIELD, STORE }

    private final Sint partnerClassObjectId;
    private final String fieldName;
    private final Sprimitive value;
    private final Type type;

    public PartnerClassObjectFieldAccessConstraint(
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

    public String getFieldName() {
        return fieldName;
    }

    public Sprimitive getValue() {
        return value;
    }

    public Type getType() {
        return type;
    }
}
