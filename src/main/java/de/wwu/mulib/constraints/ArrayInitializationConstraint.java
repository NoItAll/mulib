package de.wwu.mulib.constraints;

import de.wwu.mulib.substitutions.Sarray;
import de.wwu.mulib.substitutions.Sym;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.substitutions.primitives.SymNumericExpressionSprimitive;

import java.util.Arrays;
import java.util.Set;

public final class ArrayInitializationConstraint implements ArrayConstraint {

    /**
     * Denotes the circumstances of initialization of the array.
     */
    public enum Type {
        /**
         * The Sarray was created outside of potential aliasing.
         */
        SIMPLE_SARRAY,
        /**
         * The Sarray was created using aliasing. This can happen when creating a new
         * Sarray with defaultIsSymbolic or when accessing a symbolic partner-classes field.
         */
        ALIASED_SARRAY,
        /**
         * The Sarray was initialized when accessing an element in a free array.
         */
        SARRAY_IN_SARRAY,
        /**
         * The Sarray was initialized when accessing a field of a free object
         */
        SARRAY_IN_PARTNER_CLASS_OBJECT
    }

    private final Type type;
    private final Sint arrayId;
    private final Sint reservedId;
    // If is not null: Contains all those array-ids which arrayId can equal to
    private final Set<Sint> potentialIds;
    private final ArrayAccessConstraint[] initialSelectConstraints;
    // Either the ID of the partner class object or the SarraySarray containing this
    private final Sint containingPartnerClassObjectId;
    private final Sint arrayLength;
    private final Sbool isNull;
    private final Class<?> valueType;
    private final boolean defaultIsSymbolic;
    // Only set if type == SARRAY_IN_PARTNER_CLASS_OBJECT
    private final String fieldName;
    // Only set if type == SARRAY_IN_SARRAY
    private final Sint index;

    private ArrayInitializationConstraint(
            Sint arrayId,
            Sint arrayLength,
            Sbool isNull,
            Set<Sint> potentialIds,
            Sint reservedId,
            Sint containingPartnerClassObjectId,
            Class<?> valueType,
            ArrayAccessConstraint[] initialSelectConstraints,
            boolean defaultIsSymbolic,
            String fieldName,
            Sint index) {
        assert isNull != null;
        assert valueType != null;
        assert !Sarray.class.isAssignableFrom(valueType);
        assert arrayId != null;
        assert arrayLength != null;
        assert fieldName == null || index == null;
        assert potentialIds == null || containingPartnerClassObjectId == null;
        assert initialSelectConstraints != null;
        assert Arrays.stream(initialSelectConstraints).allMatch(isc -> isc.getType() == ArrayAccessConstraint.Type.SELECT);
        assert Arrays.stream(initialSelectConstraints).allMatch(isc -> isc.getPartnerClassObjectId() == arrayId);
        assert Arrays.stream(initialSelectConstraints).noneMatch(isc -> isc.getIndex() instanceof Sym);
        this.initialSelectConstraints = initialSelectConstraints;
        if (fieldName != null) {
            assert arrayId instanceof SymNumericExpressionSprimitive;
            this.type = Type.SARRAY_IN_PARTNER_CLASS_OBJECT;
        } else if (index != null) {
            this.type = Type.SARRAY_IN_SARRAY;
        } else if (potentialIds == null && containingPartnerClassObjectId == null) {
            this.type = Type.SIMPLE_SARRAY;
        } else {
            assert arrayId instanceof SymNumericExpressionSprimitive;
            assert potentialIds != null && !potentialIds.isEmpty();
            this.type = Type.ALIASED_SARRAY;
        }
        this.arrayId = arrayId;
        this.reservedId = reservedId;
        this.potentialIds = potentialIds;
        this.containingPartnerClassObjectId = containingPartnerClassObjectId;
        this.arrayLength = arrayLength;
        this.isNull = isNull;
        this.valueType = valueType;
        this.defaultIsSymbolic = defaultIsSymbolic;
        this.fieldName = fieldName;
        this.index = index;
    }

    /**
     * Constructor for symbolically representing a usual array
     */
    public ArrayInitializationConstraint(
            Sint arrayId,
            Sint arrayLength,
            Sbool isNull,
            Class<?> valueType,
            ArrayAccessConstraint[] initialSelectConstraints,
            boolean defaultIsSymbolic) {
        this(arrayId, arrayLength, isNull, null, null, null, valueType, initialSelectConstraints,
                defaultIsSymbolic, null, null);
    }

    /**
     * Constructor for symbolically representing an array using aliasing
     */
    public ArrayInitializationConstraint(
            Sint arrayId,
            Sint arrayLength,
            Sbool isNull,
            Sint reservedId,
            Set<Sint> potentialIds,
            Class<?> valueType,
            ArrayAccessConstraint[] initialSelectConstraints,
            boolean defaultIsSymbolic) {
        this(arrayId, arrayLength, isNull, potentialIds, reservedId, null, valueType, initialSelectConstraints,
                defaultIsSymbolic, null, null);
    }

    /**
     * Constructor for symbolically representing an array symbolically generated by accessing a
     * partner class object's field or by accessing a sarray of sarrays via an index
     */
    public ArrayInitializationConstraint(
            Sint arrayId,
            Sint arrayLength,
            Sbool isNull,
            Sint reservedId,
            Sint containingPartnerClassObjectId,
            String fieldName,
            Sint index,
            Class<?> valueType,
            ArrayAccessConstraint[] initialSelectConstraints,
            boolean defaultIsSymbolic) {
        this(arrayId, arrayLength, isNull, null, reservedId, containingPartnerClassObjectId, valueType, initialSelectConstraints,
                defaultIsSymbolic, fieldName, index);
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public Sint getPartnerClassObjectId() {
        return arrayId;
    }

    public Type getType() {
        return type;
    }

    public Sint getArrayLength() {
        return arrayLength;
    }

    public Sbool getIsNull() {
        return isNull;
    }

    public Sint getReservedId() {
        return reservedId;
    }

    public Set<Sint> getPotentialIds() {
        return potentialIds;
    }

    public Sint getContainingPartnerClassObjectId() {
        return containingPartnerClassObjectId;
    }

    public Sint getIndex() {
        return index;
    }

    public ArrayAccessConstraint[] getInitialSelectConstraints() {
        return initialSelectConstraints;
    }

    public Class<?> getValueType() {
        return valueType;
    }

    public boolean defaultIsSymbolic() {
        return defaultIsSymbolic;
    }

    @Override
    public String toString() {
        return "AR_INIT{arrayId=" + arrayId + ",type=" + type
                + ",valueType=" + valueType + "}";
    }
}
